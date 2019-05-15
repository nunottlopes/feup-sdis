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
	
	long timeout = 1 * 1000;

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
				
				String[] args = message.split(" +");
				
				
				if (args[0].equals("CHORDLOOKUP")) // CHORDLOOKUP <{SUCCESSOR | PREDECESSOR}> <request_IP> <request_port> <key>
				{
					boolean successor = args[1].equals("SUCCESSOR");
					
					parent.lookup(new InetSocketAddress(args[2], Integer.parseInt(args[3])), Integer.parseInt(args[4]), successor);						
				}
				
				else if (args[0].equals("CHORDRETURN")) // CHORDRETURN <{SUCCESSOR | PREDECESSOR}> <target_id> <target_IP> <target_port> <key>
				{
					synchronized(this.parent)
					{
						messageQueue.add(new Pair<InetSocketAddress, String[]>((InetSocketAddress) connection.getRemoteSocketAddress(), args));
						
						this.parent.notifyAll();
					}
				}
				
				else if (args[0].equals("CHORDNOTIFY")) // CHORDNOTIFY <origin_id> <origin_IP> <origin_port>
				{
					parent.notify(Integer.parseInt(args[1]), new InetSocketAddress(args[2], Integer.parseInt(args[3])));						
				}
				
				
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
	
	protected String[] sendLookup(InetSocketAddress connectionIP, InetSocketAddress requestIP, int hash, boolean successor)
	{
		if (connectionIP.equals(this.parent.address))
		{
			this.parent.lookup(requestIP, hash, successor);
		}
		else
		{
			String message = createLookupMessage(requestIP, hash, successor);		
			sendMessage(connectionIP, message);
		}
			
		synchronized(this.parent)
		{
			try
			{
				this.parent.wait(this.timeout);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			Pair<InetSocketAddress, String[]> pair = messageQueue.poll();
			
			if (pair == null)
				return null;
			
			boolean messageSuccessor = pair.second[1].equals("SUCCESSOR");
			
			if (messageSuccessor != successor)
				return null;
			
			return pair.second;
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
		if (destination.equals(this.parent.address))
			this.parent.notify(originId, originIP);
		else
		{
			String message = createNotifyMessage(originId, originIP);
			
			sendMessage(destination, message);
		}
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
		try
		{
			Socket connection = new Socket();
			connection.connect(address);
			ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
			
			oos.writeObject(message);
			
			connection.close();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}
	
	
	public Pair<InetSocketAddress, String[]> waitMessage(int timeout)
	{
		synchronized (this.parent)
		{
			try
			{
				this.parent.wait(timeout);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			return (Pair<InetSocketAddress, String[]>) this.parent.notifyValue;
		}
	}
	
	
	public void start()
	{
		thread = new Thread(this, "ChordChannel");
		
		thread.start();
	}
	
}
