package chord;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import global.Pair;

public class Chord
{
	private int id;
	private int m;
	private int maxPeers;
	
	private Pair<Integer, InetSocketAddress> predecessor = null;
	private Pair<Integer, InetSocketAddress>[] fingerTable = null;
	private static Chord singleton = null;
	
	
//	private HashMap<String, Chunk> storage  = new HashMap<String, Chunk>();

	private InetSocketAddress address;

	public static void main(String[] args)
	{
		singleton = new Chord(0, (int)Math.pow(2, 31)-1);
		
		singleton.run();
	}

	@SuppressWarnings("unchecked")
	public Chord(int id, int maxPeers)
	{
		this.m = (int) Math.ceil(Math.log(maxPeers)/Math.log(2));
		this.maxPeers = (int)Math.pow(2, this.m);

		this.id = Math.floorMod(sha1(new Integer(id).toString()), this.maxPeers);

		this.address = new InetSocketAddress(getLocalIP(), 5555);

		this.fingerTable = new Pair[this.m];

		for (int i = 0; i < this.fingerTable.length; i++)
		{
			int value = (this.id + (int)Math.pow(2, i)) % maxPeers;
			
			this.fingerTable[i] = new Pair<Integer, InetSocketAddress>(this.id, this.address);
		}
	}

	@SuppressWarnings("unchecked")
	public Chord(int id, int maxPeers, InetSocketAddress address)
	{
		this.m = (int) Math.ceil(Math.log(maxPeers)/Math.log(2));
		this.maxPeers = (int)Math.pow(2, this.m);

		this.id = Math.floorMod(sha1(new Integer(id).toString()), this.maxPeers);

		fingerTable = new Pair[this.m];

		for (int i = 0; i < fingerTable.length; i++)
		{
			int value = (this.id + (int)Math.pow(2, i)) % maxPeers;
			
			// Query successor(value)
		}
	}
	
	public InetSocketAddress lookup(String key)
	{
//		if (storage.get(key) != null)
//			return this.address;		
		
		int hash = Math.floorMod(sha1(key), this.maxPeers);
		int i;
		for (i = 0; i < fingerTable.length; i++)
		{
			if (fingerTable[i].first > hash)
				break;
		}
		
		if (i == 0) // Not found
			return null;
		else
		{
			// Pass query to node i
			
			
			return null;
		}
	}

	private void run()
	{
		System.out.println(this.m);
		System.out.println(this.id);
		
		System.out.println(getLocalIP());
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

