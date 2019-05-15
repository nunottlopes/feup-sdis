package chord;

import java.net.InetSocketAddress;

public class ChordTest
{
	public static void main(String[] args)
	{
		int id = Integer.parseInt(args[0]);
		int maxPeers = 32;
		int port = Integer.parseInt(args[1]);
		InetSocketAddress connectionPeer = null;
		Chord chord = null;
		
		if (args.length == 4)
		{
			connectionPeer = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
			chord = new Chord(id, maxPeers, port, connectionPeer);

		}
		else
		{
			chord = new Chord(id, maxPeers, port);
		}
		
		System.out.println("ID = " + chord.id);
		System.out.println("IP Address = " + chord.address);
		
		System.out.println("Finger Table:");
		for (int i = 0; i < chord.fingerTable.length; i++)
		{
			System.out.println(chord.fingerTable[i]);
		}

	}
	
}
