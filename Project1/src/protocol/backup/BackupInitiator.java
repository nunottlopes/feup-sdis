package protocol.backup;

import peer.Peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BackupInitiator {

    private String path;
    private int repDegree;

    public BackupInitiator(String path, int repDegree) {
        this.path = path;
        this.repDegree = repDegree;
    }

    public void run() {
        File file = new File(path);


        FileInputStream in_stream = null;
        try {
            in_stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        byte[] data = new byte[(int) file.length()];

        try {
            in_stream.read(data);
            in_stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        int chunks_num= file_data.length / (Chunk.MAX_SIZE) +1;
    }

    private String generateFileId(File f) {
        String file_id = f.getName() + f.lastModified() + Peer.getInstance().getId();
        return sha256(file_id);
    }

    public String sha256(String s) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        return new String(hash);
    }
}
