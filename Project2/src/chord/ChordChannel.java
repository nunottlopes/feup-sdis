package chord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;

import peer.Chunk;

/**
 * ChordChannel class
 *
 */
public class ChordChannel implements Runnable {
	/**
	 * The Choord object parent
	 */
	private Chord parent = null;;

	/**
	 * The thread where the channel is running
	 */
	private Thread thread = null;

	/**
	 * The channel's socket
	 */
	private ServerSocket socket = null;

	/**
	 * The received message's queue
	 */
	private ConcurrentLinkedQueue<Pair<InetSocketAddress, String[]>> messageQueue = null;

	/**
	 * Read timeout
	 */
	protected long timeout = 1 * 2000;

	/**
	 * ChordChannel's constructor
	 * @param parent
	 */
	public ChordChannel(Chord parent) {
		this.parent = parent;

		messageQueue = new ConcurrentLinkedQueue<Pair<InetSocketAddress, String[]>>();
	}

	/**
	 * Opens a ServerSocket
	 * @param port
	 */
	protected void open(int port) {
		try {
			socket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.start();
	}

	/**
	 * The Thread action
	 */
	@Override
	public void run() {
		while (true) {
			try {
				Socket connection = socket.accept();

				ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());

				String message = (String) ois.readObject();

				handleMessage(connection, message,ois);

				connection.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Handles a received message
	 * @param connection
	 * @param message
	 * @param ois
	 */
	protected void handleMessage(Socket connection, String message,ObjectInputStream ois) {
		String[] args = message.split(" +");

		if (args[0].equals("CHORDLOOKUP")) // CHORDLOOKUP <{SUCCESSOR | PREDECESSOR}> <request_IP> <request_port> <key>
		{
			if (!parent.client) {
				boolean successor = args[1].equals("SUCCESSOR");
				parent.lookup(new InetSocketAddress(args[2], Integer.parseInt(args[3])), Integer.parseInt(args[4]),
						successor);
			}
		}

		else if (args[0].equals("CHORDRETURN")) // CHORDRETURN <{SUCCESSOR | PREDECESSOR}> <target_id> <target_IP> <target_port> <key>
		{
			synchronized (this.parent) {
				if (connection == null)
					messageQueue.add(new Pair<InetSocketAddress, String[]>((InetSocketAddress) this.parent.address, args));
				else
					messageQueue.add(new Pair<InetSocketAddress, String[]>((InetSocketAddress) connection.getRemoteSocketAddress(), args));

				this.parent.notify();
			}
		}

		else if (args[0].equals("CHORDGETKEYS")) // CHORDGETKEYS
		{
			System.out.println("Received CHORDGETKEYS");

//			synchronized (this.parent) {

				int hash = Integer.parseInt(args[1]);
				ArrayList<Pair<Integer, Chunk>> chunks = this.parent.getKeysToPredecessor(hash);
				this.sendKeys(new InetSocketAddress(args[2], Integer.parseInt(args[3])), chunks);
				System.out.println("CHUNKS SENT");
//			}

		}

		else if (args[0].equals("CHORDKEYS")) // CHORDGETKEYS
		{
			System.out.println("Received " + message);
			int chunkNum = Integer.parseInt(args[1]);
			this.receiveKeys(ois, chunkNum);

		}

		else if (args[0].equals("CHORDNOTIFY")) // CHORDNOTIFY <origin_id> <origin_IP> <origin_port>
		{
			if (!parent.client) {
				parent.notify(Integer.parseInt(args[1]), new InetSocketAddress(args[2], Integer.parseInt(args[3])));
			}
		}
	}

	/**
	 * Sends Lookup message
	 * @param connectionIP
	 * @param requestIP
	 * @param hash
	 * @param successor
	 * @return
	 */
	protected String[] sendLookup(InetSocketAddress connectionIP, InetSocketAddress requestIP, int hash, boolean successor) {
		return sendLookup(connectionIP, requestIP, hash, successor, true);
	}

	/**
	 * sendLookup argument overload
	 * @param connectionIP
	 * @param requestIP
	 * @param hash
	 * @param successor
	 * @param fix
	 * @return
	 */
	protected String[] sendLookup(InetSocketAddress connectionIP, InetSocketAddress requestIP, int hash, boolean successor, boolean fix) {
		String message = createLookupMessage(requestIP, hash, successor);
		sendMessage(connectionIP, message, fix);

		synchronized (this.parent) 
		{
			try 
			{
				if (!connectionIP.equals(this.parent.address))
					this.parent.wait(this.timeout*2);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			for (Pair<InetSocketAddress, String[]> element : messageQueue) // Search for my response
			{
				if (element == null)
					continue;

				String[] args = element.second;
				boolean messageSuccessor = args[1].equals("SUCCESSOR");
				int messageHash = Integer.parseInt(args[5]);

				if (messageSuccessor == successor && messageHash == hash) {
					messageQueue.remove(element);
					return args;
				}
			}

			return null;
		}
	}

	/**
	 * Relays lookup to another peer
	 * @param connectionIP
	 * @param requestIP
	 * @param hash
	 * @param successor
	 */
	protected void relayLookup(InetSocketAddress connectionIP, InetSocketAddress requestIP, int hash,
			boolean successor) {
		String message = createLookupMessage(requestIP, hash, successor);

		sendMessage(connectionIP, message);
	}

	/**
	 * Sends a Chord return message
	 * @param targetId
	 * @param targetIP
	 * @param destination
	 * @param hash
	 * @param successor
	 */
	protected void sendReturn(int targetId, InetSocketAddress targetIP, InetSocketAddress destination, int hash,
			boolean successor) {
		String message = createReturnMessage(targetId, targetIP, hash, successor);

		sendMessage(destination, message);
	}

	/**
	 * Notifies the chord
	 * @param originId
	 * @param originIP
	 * @param destination
	 */
	protected void sendNotify(int originId, InetSocketAddress originIP, InetSocketAddress destination) {
		// if (!destination.equals(this.parent.address))
		// {
		String message = createNotifyMessage(originId, originIP);

		sendMessage(destination, message);
		// }
	}

	/**
	 * Sends the chunk's keys
	 * @param targetIP
	 * @param keysChunks
	 */
	protected void sendKeys(InetSocketAddress targetIP, ArrayList<Pair<Integer, Chunk>> keysChunks) {
		sendChunks(targetIP, keysChunks);

		// System.out.println(message);

	}

	/**
	 * Receives the chunk's keys
	 * @param ois
	 * @param chunkNum
	 */
	protected void receiveKeys(ObjectInputStream ois, int chunkNum) {
		if (chunkNum > 0) {
			try {

				Object obj = ois.readObject();
								
				// Check it's an ArrayList
				if (obj instanceof ArrayList<?>) {
					// Get the List.
					ArrayList<?> al = (ArrayList<?>) obj;
					if (al.size() > 0) {
					  // Iterate.
					  for (int i = 0; i < al.size(); i++) {
							// Still not enough for a type.
							Object o = al.get(i);
							if (o instanceof Chunk) {
							  // Here we go!
							  Chunk chunk = (Chunk) o;
							  synchronized(this.parent){
							  	this.parent.storeChunk(chunk);
							  }
							}
					  }
					}
			  }
				System.out.println("--------------CHUNK RECEIVED------------");
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Creates The lookup message
	 * @param requestIP
	 * @param hash
	 * @param successor
	 * @return
	 */
	protected String createLookupMessage(InetSocketAddress requestIP, int hash, boolean successor) {
		String message = "CHORDLOOKUP" + " "; // CHORDLOOKUP <{SUCCESSOR | PREDECESSOR}> <request_IP> <request_port>
												// <key>

		if (successor)
			message += "SUCCESSOR" + " ";
		else
			message += "PREDECESSOR" + " ";

		if (requestIP == null)
			message += "null" + " " + "null" + " " + hash;
		else
			message += requestIP.getAddress().getHostAddress() + " " + requestIP.getPort() + " " + hash;

		return message;
	}

	/**
	 * Creates return message
	 * @param targetId
	 * @param targetIP
	 * @param hash
	 * @param successor
	 * @return
	 */
	protected String createReturnMessage(int targetId, InetSocketAddress targetIP, int hash, boolean successor) {
		String message = "CHORDRETURN" + " "; // CHORDRETURN <{SUCCESSOR | PREDECESSOR}> <target_id> <target_IP>
												// <target_port> <key>

		if (successor)
			message += "SUCCESSOR" + " ";
		else
			message += "PREDECESSOR" + " ";

		if (targetId == -1)
			message += targetId + " " + "null" + " " + "null" + " " + hash;
		else
			message += targetId + " " + targetIP.getAddress().getHostAddress() + " " + targetIP.getPort() + " " + hash;

		return message;
	}

	/**
	 * Creates the Notify message
	 * @param originId
	 * @param originIP
	 * @return
	 */
	protected String createNotifyMessage(int originId, InetSocketAddress originIP) {
		String message = "CHORDNOTIFY" + " " + originId + " " + originIP.getAddress().getHostAddress() + " "
				+ originIP.getPort(); // CHORDNOTIFY <origin_id> <origin_IP> <origin_port>

		return message;
	}

	/**
	 * Sends a message
	 * @param address
	 * @param message
	 */
	protected void sendMessage(InetSocketAddress address, String message) {
		sendMessage(address, message, false);
	}

	/**
	 * SendMessage argument overload
	 * @param address
	 * @param message
	 * @param fix
	 */
	protected void sendMessage(InetSocketAddress address, String message, boolean fix) {
		if (address.equals(this.parent.address)) {
			handleMessage(null, message,null);
		} else {
			try {
				Socket connection = new Socket();
				connection.connect(address, (int) this.timeout);
				ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());

				oos.writeObject(message);
				connection.close();
			}
			catch (IOException e) {
				System.err.print("\rServer is overloaded!");

				if (fix)
				{
					this.parent.fixSuccessor(address);
				}
			}
		}

	}

	/**
	 * Sends Chunks
	 * @param address
	 * @param keysChunks
	 */
	protected void sendChunks(InetSocketAddress address, ArrayList<Pair<Integer, Chunk>> keysChunks) {
		if (address.equals(this.parent.address)) {
			return;
		} else {
			try {
				Socket connection = new Socket();
				connection.connect(address, (int) this.timeout);
				ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());

				String message = "CHORDKEYS " + keysChunks.size();
				oos.writeObject(message);
				ArrayList<Chunk> chunks = new ArrayList<>();

				for(Pair<Integer,Chunk> pair: keysChunks){
					chunks.add(pair.second);
					
				}
				oos.writeObject(chunks);
				connection.close();
			} catch (IOException e) {
				System.err.println("Failed!");
			}
		}

	}

	/**
	 * Sends Get keys message
	 * @param connectionIP
	 * @param requestIP
	 * @param hash
	 */
	protected void sendGetKeys(InetSocketAddress connectionIP, InetSocketAddress requestIP, int hash) {
		String msg = createGetKeysMessage(requestIP, hash);
		sendMessage(connectionIP, msg);
		System.out.println("-------SEND GET KEYS ---------");
	}

	/**
	 * Creates Get Keys Message
	 * @param originIP
	 * @param hash
	 * @return
	 */
	protected String createGetKeysMessage(InetSocketAddress originIP, int hash) {
		String message = "CHORDGETKEYS" + " " + hash + " " + originIP.getAddress().getHostAddress() + " "
				+ originIP.getPort(); // CHORDGETKEYS <origin_id> <origin_IP> <origin_port>

		return message;
	}

	/**
	 * Starts ChordChannel Thread
	 */
	protected void start() {
		thread = new Thread(this, "ChordChannel");

		thread.start();
	}

}
