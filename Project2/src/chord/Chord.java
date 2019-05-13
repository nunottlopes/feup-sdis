package chord;

public class Chord
{
	// private fingerTable

	private int id;

	private int m;

	private MessageDigest digest = null;

	public Chord(int id, int maxPeers)
	{
		int m = Math.ceil(Math.log(maxPeers)/Math.log(2));

		digest = MessageDigest.getInstance("SHA-1");
	}


	public static String sha1(String s) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

