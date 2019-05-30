package globals;

import peer.Chunk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

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
        String file_id = f.getName() + f.length() + f.hashCode();
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

    /**
     * Splits file to backup in chunks
     * @param data
     * @param fileId
     * @param repDegree
     * @return
     */
    public static ArrayList<Chunk> splitIntoChunks(byte[] data, String fileId, int repDegree) {
        ArrayList<Chunk> ret = new ArrayList<>();

        int n = data.length / (Chunk.MAX_SIZE) + 1;

        for(int i = 0; i < n; i++) {

            byte[] chunk_data;

            if(i == n-1) {
                if(data.length % Chunk.MAX_SIZE ==0) {
                    chunk_data= new byte[0];
                } else {
                    chunk_data= Arrays.copyOfRange(data, i*Chunk.MAX_SIZE, i*Chunk.MAX_SIZE + (data.length % Chunk.MAX_SIZE));
                }
            } else {
                chunk_data= Arrays.copyOfRange(data, i*Chunk.MAX_SIZE, i*Chunk.MAX_SIZE + Chunk.MAX_SIZE);
            }
            Chunk chunk=new Chunk(fileId, i, repDegree, chunk_data);
            ret.add(chunk);
        }
        return ret;
    }
}
