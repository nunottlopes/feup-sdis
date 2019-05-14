package chord;

import java.net.InetSocketAddress;

import peer.Chunk;

public class ChordTest
{
	public static void main(String[] args)
	{
		Chord chord = new Chord(Integer.parseInt(args[0]), 32);
		
		System.out.println(chord.m);
		System.out.println(chord.id);
		
		System.out.println(chord.address);
		
		if (args.length == 1)
			chord.channel.open();
		
		if (args.length == 2)
		{
			chord.channel.open();

			byte[] data = new byte[2];
			data[0] = 4;
			data[1] = 5;
			
			Chunk chunk = new Chunk("file1", 0, 2, data);
			
			System.out.println("Tried to store chunk");
//			System.out.println("Return: " + chord.addChunk(chunk.getFileId()+chunk.getChunkNo(), chunk));
			
			
			String key = "ola";
			String message = "CHORDLOOKUP" + " " + chord.address.getAddress() + " " + chord.address.getPort() + " " + key;
			
			chord.channel.sendMessage(new InetSocketAddress(chord.address.getAddress(), Integer.parseInt(args[1])), message);
		}			
		
	}
	
}
