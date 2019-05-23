package protocol.backup;

import message.Message;
import peer.Chunk;
import peer.FileManager;
import peer.Peer;


import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static message.SendMessage.sendSTORED;

/**
 * Backup class
 */
public class Backup {

    private boolean self_backup = false;
    /**
     * Peer file manager
     */
    private FileManager fm;

    /**
     * File path
     */
    private String path;

    /**
     * Chunk to be saved
     */
    private Chunk chunk;

    private InetAddress address;

    /**
     * Backup constructor
     * @param msg
     */
    public Backup(Message msg, InetAddress address) {

        System.out.println("\n> PUTCHUNK received");
        System.out.println("- Sender Id = " + msg.getSenderId());
        System.out.println("- File Id = " + msg.getFileId());
        System.out.println("- Chunk No = " + msg.getChunkNo());

        this.address = address;
        this.fm = Peer.getInstance().getFileManager();

        if (!this.fm.hasChunk(msg.getFileId(), msg.getChunkNo())) {
            path = Peer.getInstance().getBackupPath(msg.getFileId());
            start(msg.getFileId(), msg.getChunkNo(), msg.getReplicationDeg(), msg.getBody());
        } else {
            chunk = this.fm.getChunk(msg.getFileId(), msg.getChunkNo());
            sendSTORED(chunk.getFileId(), chunk.getChunkNo(), address);
        }
    }

    public Backup(String fileId, int chunkNo, int repDegree, byte[] body, InetAddress address) {
        this.address = address;
        this.fm = Peer.getInstance().getFileManager();
        this.self_backup = true;

        if (!this.fm.hasChunk(fileId, chunkNo)) {
            path = Peer.getInstance().getBackupPath(fileId);
            start(fileId, chunkNo, repDegree, body);
        }
    }

    /**
     * Starts Backup protocol
     */
    private void start(String fileId, int chunkNo, int repDegree, byte[] body) {
        chunk = new Chunk(fileId, chunkNo, repDegree, body);

        Random r = new Random();
        int delay = r.nextInt(400);
        Peer.getInstance().getExecutor().schedule(() -> {
            if (saveChunk()) {
                if(!this.self_backup)
                    sendSTORED(chunk.getFileId(), chunk.getChunkNo(), address);
            }
        }, delay, TimeUnit.MILLISECONDS);

    }

    /**
     * Saves chunk
     * @return true if success, false otherwise
     */
    private boolean saveChunk() {
        boolean success;
        try {
            success = this.fm.saveChunkFile(Integer.toString(chunk.getChunkNo()), path, chunk.getData());
        } catch (IOException e) {
            System.out.println("Error storing chunk");
            return false;
        }

        if(success) {
            this.fm.addChunk(chunk);
        } else {
            return false;
        }
        return true;
    }
}
