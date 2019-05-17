package chord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import global.Pair;

public class ChordChannel implements Runnable
{
	Chord parent = null;;
	
	Thread thread = null;
	
	ServerSocket socket = null;

	ConcurrentLinkedQueue<Pair<InetSocketAddress, String[]>> messageQueue = null;
	
	long timeout = 1 * 2000;

	public ChordChannel(Chord parent)
	{
		this.parent = parent;
		
		messageQueue = new ConcurrentLinkedQueue<Pair<InetSocketAddress, String[]>>();
	}
	
	public void open()
	{
		this.open(5000);
	}
	
	public void open(int port)
	{
		try
		{
			socket = new ServerSocket(port);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		this.start();
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				Socket connection = socket.accept();
				
				ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
				
				String message = (String) ois.readObject();
				
				handleMessage(connection, message);
				
				connection.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			}
		}	
	}
	
	protected void handleMessage(Socket connection, String message)
	{
		String[] args = message.split(" +");
		
		if (args[0].equals("CHORDLOOKUP")) // CHORDLOOKUP <{SUCCESSOR | PREDECESSOR}> <request_IP> <request_port> <key>
		{
			if (!parent.client)
			{
				boolean successor = args[1].equals("SUCCESSOR");
				parent.lookup(new InetSocketAddress(args[2], Integer.parseInt(args[3])), Integer.parseInt(args[4]), successor);						
			}
		}
		
		else if (args[0].equals("CHORDRETURN")) // CHORDRETURN <{SUCCESSOR | PREDECESSOR}> <target_id> <target_IP> <target_port> <key>
		{
			synchronized(this.parent)
			{
				if (connection == null)
					messageQueue.add(new Pair<InetSocketAddress, String[]>((InetSocketAddress) this.parent.address, args));
				else
					messageQueue.add(new Pair<InetSocketAddress, String[]>((InetSocketAddress) connection.getRemoteSocketAddress(), args));

				this.parent.notify();
			}
		}
		
		else if (args[0].equals("CHORDNOTIFY")) // CHORDNOTIFY <origin_id> <origin_IP> <origin_port>
		{
			if (!parent.client)
			{
				parent.notify(Integer.parseInt(args[1]), new InetSocketAddress(args[2], Integer.parseInt(args[3])));
			}
		}
	}
	
	protected String[] sendLookup(InetSocketAddress connectionIP, InetSocketAddress requestIP, int hash, boolean successor)
	{
		String message = createLookupMessage(requestIP, hash, successor);		
		sendMessage(connectionIP, message);
			
		synchronized(this.parent)
		{
			try
			{
				if (!connectionIP.equals(this.parent.address))
					this.parent.wait(this.timeout);
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
				
				if (messageSuccessor == successor && messageHash == hash)
				{
					messageQueue.remove(element);
					return args;
				}
			}
			
			return null;
		}
	}
	
	protected void relayLookup(InetSocketAddress connectionIP, InetSocketAddress requestIP, int hash, boolean successor)
	{
		String message =  createLookupMessage(requestIP, hash, successor);
		
		sendMessage(connectionIP, message);
	}
	
	protected void sendReturn(int targetId, InetSocketAddress targetIP, InetSocketAddress destination, int hash, boolean successor)
	{
		String message = createReturnMessage(targetId, targetIP, hash, successor);
		
		sendMessage(destination, message);
	}
	
	protected void sendNotify(int originId, InetSocketAddress originIP, InetSocketAddress destination)
	{
//		if (!destination.equals(this.parent.address))
//		{
			String message = createNotifyMessage(originId, originIP);
			
			sendMessage(destination, message);
//		}
	}
	
	
	protected String createLookupMessage(InetSocketAddress requestIP, int hash, boolean successor)
	{
		String message = "CHORDLOOKUP" + " "; // CHORDLOOKUP <{SUCCESSOR | PREDECESSOR}> <request_IP> <request_port> <key>
		
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
	
	protected String createReturnMessage(int targetId, InetSocketAddress targetIP, int hash, boolean successor)
	{
		String message = "CHORDRETURN" + " ";  // CHORDRETURN <{SUCCESSOR | PREDECESSOR}> <target_id> <target_IP> <target_port> <key>
		
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
	
	protected String createNotifyMessage(int originId, InetSocketAddress originIP)
	{
		String message = "CHORDNOTIFY" + " " + originId + " " + originIP.getAddress().getHostAddress() + " " + originIP.getPort();  // CHORDNOTIFY <origin_id> <origin_IP> <origin_port>
		
		return message;
	}
	
	public void sendMessage(InetSocketAddress address, String message)
	{
		if (address.equals(this.parent.address))
		{
			handleMessage(null, message);
		}
		else
		{
			try
			{
				Socket connection = new Socket();
				connection.connect(address, 5000);
				ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
				
				oos.writeObject(message);
				
				connection.close();
			}
			catch (IOException e1)
			{
				System.err.println("Failed!");
				
				failedLookup(address, message);
			}
		}
				
	}
	
	private void failedLookup(InetSocketAddress address, String message)
	{
		if (address.equals(this.parent.fingerTable[0].second))
		{
			if (this.parent.fixSuccessor())
			{
				sendMessage(this.parent.fingerTable[0].second, message);
			}
			else
			{
				this.parent.fingerTable[0] = new Pair<Integer, InetSocketAddress>(this.parent.id, this.parent.address);
			}
			
		}
	}
	
	
	public void start()
	{
		thread = new Thread(this, "ChordChannel");
		
		thread.start();
	}
	
}
