package globals;

import peer.Peer;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Globals {

    public static String generateFileId(File f) {
        String file_id = f.getName() + f.lastModified() + Peer.getInstance().getId();
        return sha256(file_id);
    }

    public static String sha256(String s) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        return new HexBinaryAdapter().marshal(hash);
    }
}
