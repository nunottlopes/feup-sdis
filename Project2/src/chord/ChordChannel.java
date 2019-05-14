package chord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import global.Pair;
import peer.Chunk;

public class ChordChannel implements Runnable
{
	Chord parent = null;;
	
	Thread thread = null;
	
	ServerSocket socket = null;

	ConcurrentLinkedQueue<Pair<InetSocketAddress, String[]>> messageQueue = null;
	ConcurrentLinkedQueue<Pair<InetSocketAddress, Chunk>> chunkQueue = null;

	public ChordChannel(Chord parent)
	{
		this.parent = parent;
		
		messageQueue = new ConcurrentLinkedQueue<Pair<InetSocketAddress, String[]>>();
	}
	
	public void open()
	{
		try
		{
			socket = new ServerSocket(5000);
//			this.parent.address = new InetSocketAddress(this.parent.address.getAddress(), socket.getLocalPort());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		this.start();
	}
	
	public void open(int port)
	{
		try
		{
			socket = new ServerSocket(port);
			this.parent.address = new InetSocketAddress(this.parent.address.getAddress(), socket.getLocalPort());
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
				
				
				if (args[0].equals("CHORDLOOKUP")) // CHORDLOOKUP <request_IP> <request_port> <key>
				{
					parent.lookup(new InetSocketAddress(args[1], Integer.parseInt(args[2])), args[3]);
				}
				
				else if (args[0].equals("CHORDRETURN")) // CHORDRETURN + Chunk
				{
					Chunk chunk = null;
					
					chunk = (Chunk) ois.readObject();
					
					Pair<InetSocketAddress, Chunk> pair = new Pair<InetSocketAddress, Chunk>((InetSocketAddress)connection.getRemoteSocketAddress(), chunk);
					
//					chunkQueue.add(pair);
					
					parent.notifyChunk(pair);
				}
				
				else if (args[0].equals("CHORDSTORE")) // CHORDSTORE <key> + Chunk
				{
					Chunk chunk = null;
					
					chunk = (Chunk) ois.readObject();
					
					if (chunk != null)
					{
						parent.storage.put(args[1], chunk);
					}
					
					String response = "CHORDSTORERETURN" + " " + "1";
					
					sendMessage((InetSocketAddress)connection.getRemoteSocketAddress(), response);
				}
				
				else if (args[0].equals("CHORDSTORERETURN")) // CHORDSTORERETURN <added>
				{
					Pair<InetSocketAddress, String[]> pair = new Pair<InetSocketAddress, String[]>((InetSocketAddress)connection.getRemoteSocketAddress(), args);
					
//					messageQueue.add(pair);
					
					parent.notifyMessage(pair);
				}
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
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void sendChunk(InetSocketAddress address, String message, Chunk chunk)
	{
		try
		{
			Socket connection = new Socket();
			connection.connect(address);
			ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
			
			oos.writeObject(message);
			oos.writeObject(chunk);

			connection.close();

		}
		catch (IOException e1)
		{
			// TODO Auto-generated catch block
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
	
	public Pair<InetSocketAddress, Chunk> waitChunk(int timeout)
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
			
			return (Pair<InetSocketAddress, Chunk>) this.parent.notifyValue;
		}
	}
	
	public void start()
	{
		thread = new Thread(this, "ChordChannel");
		
		thread.start();
	}
	
}
