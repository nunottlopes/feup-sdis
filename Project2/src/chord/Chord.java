package chord;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import global.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;


public class Chord
{	
	 int id;
	 int m;
	 int r; //successorList size, r < m
	 int maxPeers;
	 InetSocketAddress address;
	 int fingerFixerIndex = 0;

	 Pair<Integer, InetSocketAddress> predecessor = null;
	 Pair<Integer, InetSocketAddress>[] fingerTable = null;
	 Pair<Integer, InetSocketAddress>[] successorList = null;
	
	 ChordChannel channel = null;
		 
	 ScheduledThreadPoolExecutor  pool = null;
	 
	 boolean client = false;


	public Chord(int maxPeers, int port)
	{
		this.initialize(maxPeers, port, false);
		
		this.fingerTable[0] = new Pair<Integer, InetSocketAddress>(this.id, new InetSocketAddress(this.address.getAddress().getHostAddress(), this.address.getPort()));
		
		this.updateSuccessorList();

		startMaintenance();
	}
	
	public Chord(int maxPeers, int port, InetSocketAddress address, boolean client)
	{
		this.initialize(maxPeers, port, client);
		
		if (!client) // Peer
		{			
			String[] args = channel.sendLookup(address, this.address, this.id, true);
			
			if (args != null) // Success
			{
				int peerId = Integer.parseInt(args[2]);
				
				if (peerId == this.id)
				{
					System.err.println("The network already has a peer with this id!");
					System.exit(1);
				}
			}
			
			int value = getFingerTableIndex(0);
			args = channel.sendLookup(address, this.address, value, true);

			
			if (args != null) // Success
			{
				int peerId = Integer.parseInt(args[2]);
				InetSocketAddress peerIP = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
				
				this.fingerTable[0] = new Pair<Integer, InetSocketAddress>(peerId, peerIP);
				updateSuccessorList();
			}
			
			startMaintenance();
		}
		else // Client
		{			
			this.fingerTable[0] = new Pair<Integer, InetSocketAddress>(0, address);
		}
	}

	public Chord(int maxPeers, int port, InetSocketAddress address)
	{
		this(maxPeers, port, address, false);
	}
	
	
	@SuppressWarnings("unchecked")
	public void initialize(int maxPeers, int port, boolean client)
	{
		this.m = (int) Math.ceil(Math.log(maxPeers)/Math.log(2));
		this.r = (int)Math.ceil(this.m/3.0);
		this.maxPeers = (int)Math.pow(2, this.m);
		
		this.address = new InetSocketAddress(getAddress(), port);
		this.address = new InetSocketAddress(this.address.getAddress().getHostAddress(), port);
		
		if (!client)
			this.id = Math.floorMod(sha1(this.address.toString()), this.maxPeers);

		
		this.fingerTable = new Pair[this.m];
		this.successorList = new Pair[this.r];
		
		this.channel = new ChordChannel(this);
		this.channel.open(port);
		
		this.client = client;
	}
	
	public void startMaintenance()
	{
        pool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
        pool.scheduleWithFixedDelay(new ChordMaintenance(this), 1500, 500, TimeUnit.MILLISECONDS);
	}
	
	public String[] lookup(int hash, boolean successor)
	{
		return lookup(this.address, hash, successor);
	}
	
	public String[] lookup(InetSocketAddress origin, int hash, boolean successor)
	{
		
		if (hash == 0 && this.id == 16)
		{
			int a = 0;
		}
		
		if (client)
		{			
			long start = System.currentTimeMillis();
			
			String[] args = channel.sendLookup(fingerTable[0].second, this.address, hash, successor);
			
			long end = System.currentTimeMillis();
			
			if (args == null)
				System.err.println("Connection peer is not alive!");
			
			System.out.println("Query took " + (end-start) + " milliseconds");
			
			return args;
		}
		
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
			
			return null;
		}
		
		if ((this.predecessor != null && isInInterval(hash, this.predecessor.first, this.id+1, true)) || hash == this.id) // I am the successor of the hash
		{
			channel.sendReturn(this.id, this.address, origin, hash, successor);
			return null;
		}
		
		if (isInInterval(hash, this.id, fingerTable[0].first+1)) // My successor is the successor of the hash
		{
			channel.sendReturn(fingerTable[0].first, fingerTable[0].second, origin, hash, successor);
			return null;
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

		return null;
	}
	
	public void stabilize()
	{
		Pair<Integer, InetSocketAddress> successor = null;
		Pair<Integer, InetSocketAddress> successorPredecessor = null;
		
		for (int i = 0; i < this.r; i++) // Search for first successor alive
		{
			successor = this.successorList[i];
			
			if (successor.first == this.id)
			{
				successorPredecessor = this.predecessor;
				break;
			}
			
			String[] args = channel.sendLookup(successor.second, this.address, successor.first, false);
			
			if (args != null)
			{
				if (Integer.parseInt(args[2]) == -1)
					successorPredecessor = null;
				else
					successorPredecessor = new Pair<Integer, InetSocketAddress>(Integer.parseInt(args[2]), new InetSocketAddress(args[3], Integer.parseInt(args[4])));
				
				if (i > 0)
					fingerTable[0] = successor;
				
				break;
			}
		}

		if (successorPredecessor != null && isInInterval(successorPredecessor.first, this.id, successor.first+1, true))
		{
			fingerTable[0] = successorPredecessor;
			successor = fingerTable[0];
		}
		
		channel.sendNotify(this.id, this.address, successor.second);
		
		this.updateSuccessorList();
	}
	
	public void notify(int originId, InetSocketAddress originIP)
	{
		if (this.predecessor == null || isInInterval(originId, this.predecessor.first, this.id, true))
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

	
	protected void updateSuccessorList()
	{
		Pair<Integer, InetSocketAddress> successor = new Pair<Integer, InetSocketAddress>(this.id, this.address);
		for (int i = 0; i < this.successorList.length; i++)
		{
			String[] args = this.channel.sendLookup(successor.second, this.address, successor.first+1, true);
			
			if (args != null) // Success
			{
				int peerId = Integer.parseInt(args[2]);
				InetSocketAddress peerIP = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
				
				successor = new Pair<Integer, InetSocketAddress>(peerId, peerIP);
				
				this.successorList[i] = new Pair<Integer, InetSocketAddress>(successor);
			}
		}
	}
	
	protected boolean fixSuccessor()
	{
		boolean found = false;
		int i;
		String[] args;
		Integer lastPeer = this.fingerTable[0].first;
		
		for (i = 1; i < this.m ; i++)
		{
			if (this.fingerTable[i].first != lastPeer)
			{
				lastPeer = this.fingerTable[i].first;
				args = this.channel.sendLookup(this.fingerTable[i].second, this.address, 0, false);
				
				if (args != null)
				{
					found = true;
					break;
				}
			}
		}
		
		if (found)
		{
			this.fingerTable[0] = new Pair<Integer, InetSocketAddress>(this.fingerTable[i]);
		}
		
		return found;
	}
	
	public boolean isInInterval(int target, int lowerBound, int upperBound)
	{
		return isInInterval(target, lowerBound, upperBound, false);
	}
	
	public boolean isInInterval(int target, int lowerBound, int upperBound, boolean inclusive)
	{
		target = Math.floorMod(target,  this.maxPeers);
		lowerBound = Math.floorMod(lowerBound,  this.maxPeers);
		upperBound = Math.floorMod(upperBound,  this.maxPeers);
		
		if (Math.abs(upperBound - lowerBound) <= 1)
			return inclusive;
		
		if (upperBound == 0)
		{
			upperBound = Math.floorMod(upperBound-1,  this.maxPeers);
			return (target > lowerBound && target <= upperBound); 
		}
			
		if (upperBound > lowerBound)
			return (target > lowerBound && target < upperBound);
		else
		{
			return !isInInterval(target, upperBound-1, lowerBound+1, inclusive);
		}
	}

	protected int getFingerTableIndex(int i)
	{
		return (this.id + (int)Math.pow(2, i)) % this.maxPeers;
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
	
	public String getAddress() {
		try {
	        InetAddress candidateAddress = null;
	        // Iterate all NICs (network interface cards)...
	        for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
	            NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
	            // Iterate all IP addresses assigned to each card...
	            for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
	                InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
	                if (!inetAddr.isLoopbackAddress()) {

	                    if (inetAddr.isSiteLocalAddress()) {
	                        // Found non-loopback site-local address. Return it immediately...
	                        return inetAddr.getHostAddress();
	                    }
	                    else if (candidateAddress == null) {
	                        // Found non-loopback address, but not necessarily site-local.
	                        // Store it as a candidate to be returned if site-local address is not subsequently found...
	                        candidateAddress = inetAddr;
	                        // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
	                        // only the first. For subsequent iterations, candidate will be non-null.
	                    }
	                }
	            }
	        }
	        if (candidateAddress != null) {
	            // We did not find a site-local address, but we found some other non-loopback address.
	            // Server might have a non-site-local address assigned to its NIC (or it might be running
	            // IPv6 which deprecates the "site-local" concept).
	            // Return this non-loopback candidate address...
	            return candidateAddress.getHostAddress();
	        }
	        // At this point, we did not find a non-loopback address.
	        // Fall back to returning whatever InetAddress.getLocalHost() returns...
	        InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
	        if (jdkSuppliedAddress == null) {
	            throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
	        }
	        return jdkSuppliedAddress.getHostName();
	    }
	    catch (Exception e) {
	    	
	    }
		return "";
	}
	
	public static InetAddress getExternalIP()
	{
		String ip;
		try
		{
			URL whatismyip = new URL("http://checkip.amazonaws.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			ip = in.readLine(); //you get the IP as a String
			return InetAddress.getByName(ip);
		}
		catch (IOException e)
		{
			ip = null;
			e.printStackTrace();
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

	@Override
	public String toString()
	{
		return "Chord: " + "m=" + m + ", id = " + id + ", address=" + address + "\nfingerFixerIndex=" + fingerFixerIndex + "\npredecessor=" + predecessor + ", \nfingerTable=" + Arrays.toString(fingerTable) + "\nsuccessorList=" + Arrays.toString(successorList);
	}


}

