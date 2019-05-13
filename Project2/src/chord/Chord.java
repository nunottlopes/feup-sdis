package chord;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Chord
{
	// private fingerTable
	private static Chord singleton = null;
	private int m;
	private int maxPeers;
	private int id;

	public static void main(String[] args)
	{
		singleton = new Chord(0, 20);
	}

	public Chord(int id, int maxPeers)
	{
		this.m = (int) Math.ceil(Math.log(maxPeers)/Math.log(2));
		this.maxPeers = (int)Math.pow(2, this.m);

		this.id = Math.floorMod(sha1(new Integer(id).toString()), this.maxPeers);

		run();
	}

	private void run()
	{
		System.out.println(this.m);
		System.out.println(this.id);

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

