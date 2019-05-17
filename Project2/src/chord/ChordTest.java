package chord;

import java.net.InetSocketAddress;

public class ChordTest
{
	public static void main(String[] args)
	{
		int maxPeers = 32;
		int port;
		InetSocketAddress connectionPeer = null;
		
		Chord chord = null;
		
		if (args.length == 3)
		{
			port = Integer.parseInt(args[0]);
			connectionPeer = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
			
			chord = new Chord(maxPeers, port, connectionPeer);

		}
		else if (args.length == 4)
		{
			port = Integer.parseInt(args[1]);
			connectionPeer = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
			
			chord = new Chord(maxPeers, port, connectionPeer, true);
			
			for (int i = 0; i < maxPeers; i++)
			{
				String[] messageArgs = chord.lookup(null, i, true);
				
				System.out.println("lookup(" + i + ") = " + messageArgs[2] + "\n");
			}
			
			System.exit(0);
		}
		else
		{
			port = Integer.parseInt(args[0]);
			chord = new Chord(maxPeers, port);
		}
		

	}
	
}
