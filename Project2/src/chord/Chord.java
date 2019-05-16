package chord;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sun.jmx.snmp.tasks.Task;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import global.Pair;

public class Chord
{	
	 int id;
	 int m;
	 int maxPeers;
	 InetSocketAddress address;
	 int fingerFixerIndex = 0;

	 Pair<Integer, InetSocketAddress> predecessor = null;
	 Pair<Integer, InetSocketAddress>[] fingerTable = null;
	
	 ChordChannel channel = null;
	
	 Object notifyValue = null;
	 
	 ScheduledThreadPoolExecutor  pool = null;


	@SuppressWarnings("unchecked")
	public Chord(int id, int maxPeers, int port)
	{
		this.initialize(id, maxPeers, port);
		
//		this.predecessor = new Pair<Integer, InetSocketAddress>(this.id, this.address);
				
		this.fingerTable = new Pair[this.m];

//		for (int i = 0; i < this.fingerTable.length; i++)
//		{			
//			this.fingerTable[i] = new Pair<Integer, InetSocketAddress>(this.id, new InetSocketAddress(this.address.getAddress().getHostAddress(), this.address.getPort()));
//		}
		
		this.fingerTable[0] = new Pair<Integer, InetSocketAddress>(this.id, new InetSocketAddress(this.address.getAddress().getHostAddress(), this.address.getPort()));

		startMaintenance();
	}

	@SuppressWarnings("unchecked")
	public Chord(int id, int maxPeers, int port, InetSocketAddress address)
	{
		this.initialize(id, maxPeers, port);
				
		this.fingerTable = new Pair[this.m];

//		for (int i = 0; i < this.fingerTable.length; i++)
//		{
//			int value = getFingerTableIndex(i);
//			
//			args = channel.sendLookup(address, this.address, value, true);
//			
//			if (args != null) // Success
//			{
//				int peerId = Integer.parseInt(args[2]);
//				InetSocketAddress peerIP = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
//				
//				this.fingerTable[i] = new Pair<Integer, InetSocketAddress>(peerId, peerIP);
//			}
//		}
		
		int value = getFingerTableIndex(0);
		String[] args = channel.sendLookup(address, this.address, value, true);
		
		if (args != null) // Success
		{
			int peerId = Integer.parseInt(args[2]);
			InetSocketAddress peerIP = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
			
			this.fingerTable[0] = new Pair<Integer, InetSocketAddress>(peerId, peerIP);
		}
		
		startMaintenance();
	}
	
	public void initialize(int id, int maxPeers, int port)
	{
		this.m = (int) Math.ceil(Math.log(maxPeers)/Math.log(2));
		this.maxPeers = (int)Math.pow(2, this.m);

		this.id = Math.floorMod(sha1(new Integer(id).toString()), this.maxPeers);
		
		this.address = new InetSocketAddress("localhost", port);
		this.address = new InetSocketAddress(this.address.getAddress().getHostAddress(), port);
		this.channel = new ChordChannel(this);
		this.channel.open(port);
	}
	
	public void startMaintenance()
	{
        pool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
        pool.scheduleWithFixedDelay(new ChordMaintenance(this), 0, 1, TimeUnit.SECONDS);
	}
	
	public void lookup(InetSocketAddress origin, int hash, boolean successor)
	{
		if (!successor)
		{
			if (predecessor == null)
			{
				channel.sendReturn(-1, null, origin, hash, successor);
			}
			else
			{
				channel.sendReturn(predecessor.first, predecessor.second, origin, hash, successor);
			}
			
			return;
		}
		
		if (this.predecessor != null && isInInterval(hash, this.predecessor.first, this.id+1)) // I am the successor of the hash
		{
			channel.sendReturn(this.id, this.address, origin, hash, successor);
			return;
		}
		
		if (isInInterval(hash, this.id, fingerTable[0].first+1)) // My successor is the successor of the hash
		{
			channel.sendReturn(fingerTable[0].first, fingerTable[0].second, origin, hash, successor);
			return;
		}
		else // Search finger table
		{
			int i;
			for (i = m-1; i >= 0; i--)
			{
				if (fingerTable[i] == null || fingerTable[i].first == null || fingerTable[i].second == null)
					continue;
				
				if (isInInterval(fingerTable[i].first, this.id, hash)) // If peer id is greater than the hash then we have exceeded the desired peer
					break;
			}
			
			if (i == -1)
			{			
				channel.sendReturn(this.id, this.address, origin, hash, successor);
			}
			else
			{
				// Pass query to node i
				channel.relayLookup(fingerTable[i].second, origin, hash, successor);
			}
		}

		
	}
	
	public void stabilize()
	{
		Pair<Integer, InetSocketAddress> successorPredecessor;
		Pair<Integer, InetSocketAddress> successor = fingerTable[0];
		
		if (successor.first == this.id)
			successorPredecessor = this.predecessor;
		else
		{
			String[] args = channel.sendLookup(successor.second, this.address, successor.first, false);
			
			if (Integer.parseInt(args[2]) == -1)
				successorPredecessor = null;
			else
				successorPredecessor = new Pair<Integer, InetSocketAddress>(Integer.parseInt(args[2]), new InetSocketAddress(args[3], Integer.parseInt(args[4])));
		}

		if (successorPredecessor != null && isInInterval(successorPredecessor.first, this.id, successor.first))
			fingerTable[0] = successorPredecessor;
		
		channel.sendNotify(this.id, this.address, successor.second);		
	}
	
	public void notify(int originId, InetSocketAddress originIP)
	{
		if (this.predecessor == null || isInInterval(originId, this.predecessor.first, this.id))
		{
			this.predecessor = new Pair<Integer, InetSocketAddress>(originId, originIP);
		}
	}
	
	public void fixFingers()
	{
		this.fingerFixerIndex = (this.fingerFixerIndex + 1) % this.m; // Increments fingerFixerIndex
		
		int value = getFingerTableIndex(this.fingerFixerIndex);

		String[] args = channel.sendLookup(this.fingerTable[0].second, this.address, value, true);
		
		if (args == null)
		{
			System.err.println("error");
			return;
		}
		
		int peerId = Integer.parseInt(args[2]);
		InetSocketAddress peerIP = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
		
		this.fingerTable[this.fingerFixerIndex] = new Pair<Integer, InetSocketAddress>(peerId, peerIP);

	}
	
	public void checkPredecessor()
	{
		if (this.predecessor != null)
		{
			String[] args = channel.sendLookup(this.predecessor.second, this.address, this.predecessor.first, true);
			
			if (args == null)
				this.predecessor = null;
		}
	}
	
	public boolean isInInterval(int target, int lowerBound, int upperBound)
	{
		target = Math.floorMod(target,  this.maxPeers);
		lowerBound = Math.floorMod(lowerBound,  this.maxPeers);
		upperBound = Math.floorMod(upperBound,  this.maxPeers);
		
		if (upperBound == lowerBound)
			return true;
	
		else if (upperBound > lowerBound)
			return (target > lowerBound && target < upperBound);
		
		else
			return !isInInterval(target, upperBound, lowerBound);
	}

	protected int getFingerTableIndex(int i)
	{
		return (this.id + (int)Math.pow(2, i)) % this.maxPeers;
	}

	
	public void notifyMessage(Pair<InetSocketAddress, String[]> value)
	{
		synchronized (this)
		{
			this.notifyValue = value;
			this.notify();
		}
	}
	
	public static InetAddress getLocalIP()
	{
		try
		{
			Enumeration<NetworkInterface> e = null;
			e = NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements())
			{
				NetworkInterface n = (NetworkInterface) e.nextElement();
				Enumeration<InetAddress> ee = n.getInetAddresses();
				while (ee.hasMoreElements())
				{
					InetAddress i = (InetAddress) ee.nextElement();
					if (i.isSiteLocalAddress())
						return i;
				}
			}
		}
		catch (SocketException e1)
		{
			e1.printStackTrace();
		}

		return null;
	}
	

	public static int sha1(String s)
	{
        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        ByteBuffer wrapped = ByteBuffer.wrap(hash);

		return wrapped.getInt();
    }


}

