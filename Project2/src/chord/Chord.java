package chord;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import global.Pair;
import peer.Chunk;

public class Chord
{	
	 int id;
	 int m;
	 int maxPeers;
	
	 Pair<Integer, InetSocketAddress> predecessor = null;
	 Pair<Integer, InetSocketAddress>[] fingerTable = null;
	
	 ChordChannel channel = null;
	
	HashMap<String, Chunk> storage  = new HashMap<String, Chunk>();

	 InetSocketAddress address;
	
	 Object notifyValue = null;

	@SuppressWarnings("unchecked")
	public Chord(int id, int maxPeers)
	{
		this.initialize(id, maxPeers);
		
		this.fingerTable = new Pair[this.m];

		for (int i = 0; i < this.fingerTable.length; i++)
		{			
			this.fingerTable[i] = null;
		}
		
	}

	@SuppressWarnings("unchecked")
	public Chord(int id, int maxPeers, InetSocketAddress address)
	{
		this.initialize(id, maxPeers);
		
		fingerTable = new Pair[this.m];

		for (int i = 0; i < fingerTable.length; i++)
		{
			int value = (this.id + (int)Math.pow(2, i)) % maxPeers;
			
			// Query successor(value)
		}
		
		
	}
	
	public void initialize(int id, int maxPeers)
	{
		this.m = (int) Math.ceil(Math.log(maxPeers)/Math.log(2));
		this.maxPeers = (int)Math.pow(2, this.m);

		this.id = Math.floorMod(sha1(new Integer(id).toString()), this.maxPeers);

		this.address = new InetSocketAddress(getLocalIP(), 5000);

		channel = new ChordChannel(this);
	}
	
	
	public void lookup(InetSocketAddress origin, String key)
	{
		Chunk chunk = storage.get(key);
		if (chunk != null)
		{
			channel.sendChunk(origin, "CHORDRETURN", chunk);
		}
		
		int hash = Math.floorMod(sha1(key), this.maxPeers);
		int i;
		for (i = 0; i < fingerTable.length; i++)
		{
			if (fingerTable[i] == null)
				continue;
			
			if (fingerTable[i].first > hash) // If peer id is greater than the hash then we have exceeded the desired peer
				break;
		}
		i--;
		
		if (i == -1 || fingerTable[i] == null) // Not found
		{
			channel.sendChunk(origin, "CHORDRETURN", null);
		}
		else
		{
			// Pass query to node i
			
			String message = "CHORDLOOKUP" + " " + origin.getAddress() + " " + origin.getPort() + " " + key;
			
			channel.sendMessage(fingerTable[i].second, message);
		}
	}
	
	public boolean addChunk(String key, Chunk chunk)
	{
		lookup(address, key); // starts a lookup with its own address as the origin address
		
		Pair<InetSocketAddress, Chunk> pair = this.channel.waitChunk(5 * 1000);
		
		if (pair == null)
		{
			System.err.println("No CHORDRETURN message!");
			return false;
		}
		
		InetSocketAddress origin = pair.first;
		Chunk receivedChunk = pair.second;
		
		if (receivedChunk == null) // Key not bound
		{
			String message = "CHORDSTORE" + " " + key;
			
			channel.sendChunk(origin, message, chunk);
			
			Pair<InetSocketAddress, String[]> storePair = this.channel.waitMessage(5 * 1000);
			
			if (storePair.second[1].equals("0"))
			{
				System.err.println("Failed adding chunk!");
				return false;
			}
			else if (storePair.second[1].equals("1"))
			{
				System.err.println("Successfully added chunk!");
				return true;
			}
			else
			{
				// Error
			}
		}
		else // Key bound
		{
			System.err.println("Key already bound!");
		}
		
		return false;
	}

	
	public void notifyMessage(Pair<InetSocketAddress, String[]> value)
	{
		synchronized (this)
		{
			this.notifyValue = value;
			this.notify();
		}
	}

	public void notifyChunk(Pair<InetSocketAddress, Chunk> value)
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

