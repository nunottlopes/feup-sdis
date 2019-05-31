package chord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import peer.Chunk;
import peer.Peer;


public class Chord
{
	 protected int id;
	 protected int m;
	 protected int r; //successorList size, r < m
	 protected int maxPeers;
	 protected InetSocketAddress address;
	 protected int fingerFixerIndex = 0;

	 protected Pair<Integer, InetSocketAddress> predecessor = null;
	 protected Pair<Integer, InetSocketAddress>[] fingerTable = null;
	 protected Pair<Integer, InetSocketAddress>[] successorList = null;

	 protected ChordChannel channel = null;

	 protected ScheduledThreadPoolExecutor  pool = null;

	 protected boolean client = false;

	 protected Peer peer = null;


	public Chord(int maxPeers, int port)
	{
		this.initialize(maxPeers, port, false);

		this.fingerTable[0] = new Pair<>(this.id, new InetSocketAddress(this.address.getAddress().getHostAddress(), this.address.getPort()));

		this.updateSuccessorList();

		startMaintenance();
	}

	public Chord(int maxPeers, int port, InetSocketAddress address, boolean client)
	{
		this.initialize(maxPeers, port, client);

		if (!client) // Peer
		{			
			String[] args = channel.sendLookup(address, this.address, this.id, true, false);
			
			if (args != null) // Success
			{
				int peerId = Integer.parseInt(args[2]);

				if (peerId == this.id)
				{
					System.err.println("The network already has a peer with this id!");
					System.exit(1);
				}
			}
			else
			{
				System.err.println("Failed to connect to peer!");
				System.exit(1);
			}

			int value = getFingerTableIndex(0);
			args = channel.sendLookup(address, this.address, value, true, false);


			if (args != null) // Success
			{
				int peerId = Integer.parseInt(args[2]);
				InetSocketAddress peerIP = new InetSocketAddress(args[3], Integer.parseInt(args[4]));

				this.fingerTable[0] = new Pair<Integer, InetSocketAddress>(peerId, peerIP);
				updateSuccessorList();
			}

			startMaintenance();
			getKeysFromSuccessor();

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
	private void initialize(int maxPeers, int port, boolean client)
	{
		this.m = (int) Math.ceil(Math.log(maxPeers)/Math.log(2));
		this.r = (int)Math.ceil(this.m/3.0);
		this.maxPeers = (int)Math.pow(2, this.m);

		this.address = new InetSocketAddress(getAddress(), port);
		//this.address = new InetSocketAddress("25.43.160.5", port);
		this.address = new InetSocketAddress(this.address.getAddress().getHostAddress(), port);

		if (!client)
		{
			this.id = hash(this.address.toString());
		}


		this.fingerTable = new Pair[this.m];
		this.successorList = new Pair[this.r];

		this.channel = new ChordChannel(this);
		this.channel.open(port);

		this.client = client;
	}

	private void startMaintenance()
	{
        pool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
        pool.scheduleWithFixedDelay(new ChordMaintenance(this), 1500, 500, TimeUnit.MILLISECONDS);
	}

	public String[] lookup(int hash, boolean successor)
	{
		return lookup(this.address, hash, successor);
	}

	public String[] sendLookup(int hash, boolean successor){
		String[] args = channel.sendLookup(fingerTable[0].second, this.address, hash, successor);
		return args;
	}

	public String[] lookup(InetSocketAddress origin, int hash, boolean successor)
	{

		if (client)
		{
			long start = System.currentTimeMillis();

			String[] args = channel.sendLookup(fingerTable[0].second, this.address, hash, successor);

			long end = System.currentTimeMillis();

			if (args == null)
				System.err.println("Connection peer is not alive!");

			System.out.println("Query took " + (end-start) + " milliseconds");

	
			if ((end-start) > this.channel.timeout)
				System.err.println("timeout");
			
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
	
	public int hash(String key)
	{
		return Math.floorMod(sha1(key), this.maxPeers);
	}
	public boolean amISuccessor(int hash)
	{
		return ((this.predecessor != null && isInInterval(hash, this.predecessor.first, this.id+1, true)) || hash == this.id); // I am the successor of the hash
	}

	public String[] sendLookup(InetSocketAddress connectionIP, InetSocketAddress requestIP, int hash, boolean successor)
	{
		return channel.sendLookup(connectionIP, requestIP, hash, successor);
	}

	protected void stabilize()
	{
		Pair<Integer, InetSocketAddress> successor = null;
		Pair<Integer, InetSocketAddress> successorPredecessor = null;

		for (int i = 0; i < this.r; i++) // Search for first successor alive
		{
			successor = this.successorList[i];
			
			if (successor == null)
				continue;
			
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
					successorPredecessor = new Pair<>(Integer.parseInt(args[2]), new InetSocketAddress(args[3], Integer.parseInt(args[4])));

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

	protected void notify(int originId, InetSocketAddress originIP)
	{
		if (this.predecessor == null || isInInterval(originId, this.predecessor.first, this.id, true))
		{
			this.predecessor = new Pair<>(originId, originIP);
		}
	}

	protected void fixFingers()
	{
		this.fingerFixerIndex = (this.fingerFixerIndex + 1) % this.m; // Increments fingerFixerIndex

		int value = getFingerTableIndex(this.fingerFixerIndex);

		String[] args = channel.sendLookup(this.fingerTable[0].second, this.address, value, true);

		if (args == null)
		{
			return;
		}

		int peerId = Integer.parseInt(args[2]);
		InetSocketAddress peerIP = new InetSocketAddress(args[3], Integer.parseInt(args[4]));

		this.fingerTable[this.fingerFixerIndex] = new Pair<>(peerId, peerIP);

	}

	protected void checkPredecessor()
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

				successor = new Pair<>(peerId, peerIP);

				this.successorList[i] = new Pair<>(successor);
			}
		}
	}
	
	protected void fixSuccessor(InetSocketAddress IP)
	{
		for (int i = 0; i < this.fingerTable.length; i++)
		{
			if (this.fingerTable[i].second.equals(IP))
				this.fingerTable[i] = null;
		}
		
		if (fingerTable[0] == null)
		{
			int i;
			for (i = 0; i < this.fingerTable.length; i++)
			{
				if (fingerTable[i] != null)
				{
					fingerTable[0] = new Pair<>(fingerTable[i]);
					break;
				}
			}
			
			if (i == this.fingerTable.length)
				this.fingerTable[0] = new Pair<>(this.id, this.address);
		}
			
		
		for (int i = 0; i < this.successorList.length; i++)
		{
			if (this.successorList[i].second.equals(IP))
				this.successorList[i] = null;
		}
		
		
		if (this.predecessor != null && this.predecessor.second.equals(IP))
			this.predecessor = null;
	}

	protected boolean isInInterval(int target, int lowerBound, int upperBound)
	{
		return isInInterval(target, lowerBound, upperBound, false);
	}

	protected boolean isInInterval(int target, int lowerBound, int upperBound, boolean inclusive)
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
			return !isInInterval(target, upperBound-1, lowerBound+1, inclusive);
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
		String address = "";
		try {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress("google.com", 80));
			address = socket.getLocalAddress().getHostAddress();
			socket.close();
		}
		catch(Exception e){
			System.out.println("Failed to get local address");
		}
		return address;
	}

	public String getChordAddress(){
		return this.address.getHostName();
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

	public int getMaxPeers() {
		return maxPeers;
	}

	@Override
	public String toString()
	{
		return "Chord: " + "m=" + m + ", id = " + id + ", address=" + address + "\nfingerFixerIndex=" + fingerFixerIndex + "\npredecessor=" + predecessor + ", \nfingerTable=" + Arrays.toString(fingerTable) + "\nsuccessorList=" + Arrays.toString(successorList);
	}

	public InetAddress getSuccessor() {
		return fingerTable[0].second.getAddress();
	}

	private void getKeysFromSuccessor(){
		this.channel.sendGetKeys(fingerTable[0].second,this.address, this.id);
	}

	public ArrayList<Pair<Integer, Chunk>> getKeysToPredecessor(int peerHash){
		ArrayList<Pair<Integer, Chunk>> keysValues = new ArrayList<>();

		if (Peer.getInstance() != null)
		{
			ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>> chunksStored = Peer.getInstance().getFileManager().getChunksStored();

			if (chunksStored != null)
			{
				
				for(Map.Entry<String, ConcurrentHashMap<Integer, Chunk>> fileChunks: chunksStored.entrySet()){
					ConcurrentHashMap<Integer,Chunk> chunksMap = fileChunks.getValue();
					for(Map.Entry<Integer, Chunk> chunkEntry: chunksMap.entrySet()){
						Chunk chunk = chunkEntry.getValue();
						int chunkHash = hash(chunk.getFileId() + chunk.getChunkNo());
		
							
						if(this.id < peerHash){
							if(chunkHash > this.id && chunkHash <= peerHash){
								keysValues.add(new Pair<Integer,Chunk>(chunkHash,chunk));
								chunksMap.remove(chunkEntry.getKey());
							}
						}
		
						else{
							if(chunkHash <= peerHash || chunkHash > this.id){
								keysValues.add(new Pair<Integer,Chunk>(chunkHash,chunk));
								chunksMap.remove(chunkEntry.getKey());
							}
						}
					}
				}	
			}
		}

		return keysValues;
	}

	public synchronized void storeChunk(Chunk chunk){
		peer.getFileManager().addChunk(chunk);
	}

	public void setPeer(Peer peer){
		this.peer = peer;
	}

	public int getId() {
		return id;
	}

	public InetAddress getInetChordAddress(){
		return this.address.getAddress();
	}


}
