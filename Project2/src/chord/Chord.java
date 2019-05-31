package chord;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import peer.Chunk;
import peer.Peer;


/**
 * Chord class
 */
public class Chord
{
	/**
	 * The peer's unique identifier
	 */
	 protected int id;
	 
	 /**
	  * Number of bits of the addressing space
	  */
	 protected int m;
	 
	 /**
	  * The successorList's size, r < m
	  */
	 protected int r;
	 
	 /**
	  * The maximum amount of peers of the ring, 2^m
	  */
	 protected int maxPeers;
	 
	 /**
	  * The peer's address
	  */
	 protected InetSocketAddress address;
	 
	 /**
	  * The index used on the maintenance function fixFingers
	  */
	 protected int fingerFixerIndex = 0;

	 /**
	  * The peer's predecessor
	  */
	 protected Pair<Integer, InetSocketAddress> predecessor = null;
	 
	 /**
	  * The peer's finger table. Stores m entries of other peers in the ring
	  */
	 protected Pair<Integer, InetSocketAddress>[] fingerTable = null;
	 
	 /**
	  * The list of its successors, size r
	  */
	 protected Pair<Integer, InetSocketAddress>[] successorList = null;


	 protected ChordChannel channel = null;

	 protected ScheduledThreadPoolExecutor  pool = null;

	 protected boolean client = false;

	 protected Peer peer = null;

	
	/**
	 * The chord's constructor for initializing a ring
	 * 
	 * @param maxPeers The minimum amount of peers supported in the ring
	 * @param port The chord's port number
	 */
	public Chord(int maxPeers, int port)
	{
		this.initialize(maxPeers, port, false);

		this.fingerTable[0] = new Pair<>(this.id, new InetSocketAddress(this.address.getAddress().getHostAddress(), this.address.getPort()));

		this.updateSuccessorList();

		startMaintenance();
	}
	
	/**
	 * The Constructor
	 * @param maxPeers The minimum amount of peers supported in the ring
	 * @param port The chord's port number
	 * @param address The connection address
	 * @param client Whether it is a member of the ring
	 */
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
	
	/**
	 * Default constructor
	 * @param maxPeers The minimum amount of peers supported in the ring
	 * @param port The chord's port number
	 * @param address The connection address
	 */
	public Chord(int maxPeers, int port, InetSocketAddress address)
	{
		this(maxPeers, port, address, false);
	}

	/**
	 * Common code between the constructors
	 * @param maxPeers The minimum amount of peers supported in the ring
	 * @param port The chord's port number
	 * @param client Whether it is a member of the ring
	 */
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
	
	/**
	 * Starts the maintenance routine
	 */
	private void startMaintenance()
	{
        pool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
        pool.scheduleWithFixedDelay(new ChordMaintenance(this), 1500, 500, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Call lookup with this peer as the destination
	 * @param hash
	 * @param successor
	 * @return
	 */
	public String[] lookup(int hash, boolean successor)
	{
		return lookup(this.address, hash, successor);
	}
	
	/**
	 * Call lookup with this peer as the destination
	 * @param hash
	 * @param successor
	 * @return
	 */
	public String[] sendLookup(int hash, boolean successor){
		
		if (fingerTable[0] == null)
			return null;
		
		String[] args = channel.sendLookup(fingerTable[0].second, this.address, hash, successor);
		return args;
	}
	
	/**
	 * Runs the lookup protocol. Searches first for directly behind itself and directly in front. It its not in either, iterates the finger table.
	 * @param origin The origin of the request
	 * @param hash The hash we are searching
	 * @param successor Whether its the successor or the predecessor
	 * @return The response message
	 */
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

	/**
	 * Returns whether it is the direct successor of the hash
	 * @param hash The hash
	 * @return whether it is the direct successor of the hash
	 */
	public boolean amISuccessor(int hash)
	{
		return ((this.predecessor != null && isInInterval(hash, this.predecessor.first, this.id+1, true)) || hash == this.id); // I am the successor of the hash
	}
	
	/**
	 * Overloaded function for sendLookup
	 * @param connectionIP
	 * @param requestIP
	 * @param hash
	 * @param successor
	 * @return
	 */
	public String[] sendLookup(InetSocketAddress connectionIP, InetSocketAddress requestIP, int hash, boolean successor)
	{
		return channel.sendLookup(connectionIP, requestIP, hash, successor);
	}
	
	/**
	 * Stabilize protocol. Notifies the successors's predecessor
	 */
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
	
	/**
	 * Updates its predecessor.
	 * @param originId
	 * @param originIP
	 */
	protected void notify(int originId, InetSocketAddress originIP)
	{
		if (this.predecessor == null || isInInterval(originId, this.predecessor.first, this.id, true))
		{
			this.predecessor = new Pair<>(originId, originIP);
		}
	}

	/**
	 * Fix the finger table entry with index fingerFixerIndex
	 */
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
	
	/**
	 * Check's if the predecessor has failed
	 */
	protected void checkPredecessor()
	{
		if (this.predecessor != null)
		{
			String[] args = channel.sendLookup(this.predecessor.second, this.address, this.predecessor.first, true);

			if (args == null)
				this.predecessor = null;
		}
	}

	/**
	 * Updates all entries of the successorList
	 */
	protected void updateSuccessorList()
	{
		Pair<Integer, InetSocketAddress> successor = new Pair<Integer, InetSocketAddress>(this.id, this.address);
		for (int i = 0; i < this.successorList.length; i++)
		{
			if (successor != null)
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
	}
	
	/**
	 * Function called when IP fails. Removes IP from all the tables
	 * @param IP
	 */
	protected void fixSuccessor(InetSocketAddress IP)
	{
		for (int i = 0; i < this.fingerTable.length; i++)
		{
			if (this.fingerTable[i] == null)
				continue;
			
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
			if (this.successorList[i] == null)
				continue;
			
			if (this.successorList[i].second.equals(IP))
				this.successorList[i] = null;
		}
		
		
		if (this.predecessor != null && this.predecessor.second.equals(IP))
			this.predecessor = null;
	}
	
	/**
	 * Overloaded function for isInInterval.
	 * @param target
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	protected boolean isInInterval(int target, int lowerBound, int upperBound)
	{
		return isInInterval(target, lowerBound, upperBound, false);
	}
	
	/**
	 * Check whether target is between upperBound and lowerBound
	 * @param target
	 * @param lowerBound
	 * @param upperBound
	 * @param inclusive
	 * @return
	 */
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
	
	/**
	 * Calculates fingerTable number by index
	 * @param i
	 * @return
	 */
	protected int getFingerTableIndex(int i)
	{
		return (this.id + (int)Math.pow(2, i)) % this.maxPeers;
	}

	/**
	 * Gets the local IP Address
	 * @return The local IP Address
	 */
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
	
	/**
	 * Gets the chord's address
	 * @return
	 */
	public String getChordAddress(){
		return this.address.getAddress().toString().substring(1);
	}
	
	/**
	 * Calculates the hash of key
	 * @param key
	 * @return
	 */
    public int hash(String key)
    {
        return Math.floorMod(sha1(key), this.maxPeers);
    }
    
    /**
     * Calculates the SHA1 hash
     * @param s
     * @return
     */
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
	
	/**
	 * Gets the maximum amount of allowed peers
	 * @return The maximum amount of allowed peers
	 */
	public int getMaxPeers() {
		return maxPeers;
	}
	
	/**
	 * Returns a string
	 */
	@Override
	public String toString()
	{
		return "Chord: " + "m=" + m + ", id = " + id + ", address=" + address + "\nfingerFixerIndex=" + fingerFixerIndex + "\npredecessor=" + predecessor + ", \nfingerTable=" + Arrays.toString(fingerTable) + "\nsuccessorList=" + Arrays.toString(successorList);
	}
	
	/**
	 * Gets the immediate succesor
	 * @return
	 */
	public InetAddress getSuccessor() {
		return fingerTable[0].second.getAddress();
	}
	
	/**
	 * Requests the keys from it's successor
	 */
	private void getKeysFromSuccessor(){
		this.channel.sendGetKeys(fingerTable[0].second,this.address, this.id);
	}
	
	/**
	 * Sends the keys to the predecessor
	 * @param peerHash
	 * @return
	 */
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
	
	/**
	 * Stores a chunk
	 * @param chunk A chunk
	 */
	public synchronized void storeChunk(Chunk chunk){
		peer.getFileManager().addChunk(chunk);
	}
	
	/**
	 * Sets the peer object
	 * @param peer
	 */
	public void setPeer(Peer peer){
		this.peer = peer;
	}
	
	/**
	 * Gets the id
	 * @return The id
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Gets the chord's IP Address as an InetAddress
	 * @return
	 */
	public InetAddress getInetChordAddress(){
		return this.address.getAddress();
	}


}
