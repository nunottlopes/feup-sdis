package globals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Globals class
 */
public class Globals {

    /**
     * Generates fileid given its file
     * @param f
     * @return fileid
     */
    public static String generateFileId(File f) {
        String file_id = f.getName() + f.lastModified() + f.length() + f.hashCode();
        return sha256(file_id);
    }

    /**
     * Hashes a string
     * @param s
     * @return Returns hashed string s
     */
    public static String sha256(String s) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
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

    /**
     * Reads file and returns its data
     * @param file
     * @return Returns data from a file
     * @throws FileNotFoundException
     */
    public static byte[] getFileData(File file) throws FileNotFoundException {
        FileInputStream in_stream;
        in_stream = new FileInputStream(file);

        byte[] data = new byte[(int) file.length()];

        try {
            in_stream.read(data);
            in_stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }
}
