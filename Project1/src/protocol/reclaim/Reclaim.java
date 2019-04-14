package protocol.reclaim;

import message.Message;
import peer.Chunk;
import peer.Peer;
import protocol.backup.BackupInitiator;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Reclaim class
 */
public class Reclaim {

    /**
     * File id
     */
    private String fileId;

    /**
     * Chunk number
     */
    private int chunkNo;

    /**
     * Desired replication degree
     */
    private int repDegree;

    /**
     * Sender id
     */
    private int senderId;

    /**
     * Path to the file that has been removed
     */
    private String filepath;

    /**
     * Chunk to be sent
     */
    private Chunk chunk;

    /**
     * Reclaim constructor
     * @param msg
     */
    public Reclaim(Message msg) {
//        System.out.println("\n> REMOVED received");
//        System.out.println("- Sender Id = " + msg.getSenderId());
//        System.out.println("- File Id = " + msg.getFileId());
//        System.out.println("- Chunk No = " + msg.getChunkNo());

        this.fileId = msg.getFileId();
        this.chunkNo = msg.getChunkNo();
        this.senderId = msg.getSenderId();
        this.filepath = Peer.getInstance().getBackupPath(fileId) + "/" + chunkNo;
        this.chunk = null;

        start();
    }

    /**
     * Starts Reclaim protocol
     */
    private void start() {
        Peer.getInstance().getProtocolInfo().updateChunkRepDegree(fileId, chunkNo, senderId);

        if(!Peer.getInstance().getFileManager().hasChunk(fileId, chunkNo)){
            return;
        }

        chunk = Peer.getInstance().getFileManager().getChunkFromFile(fileId, chunkNo);
        chunk.removePerceivedRepDegreePeer(senderId);

        if(chunk.getPerceivedRepDegree() >= chunk.getRepDegree()){
            return;
        }
        this.repDegree = chunk.getRepDegree();

        Peer.getInstance().getProtocolInfo().setReclaimProtocol(true);


        Random r = new Random();
        int delay = r.nextInt(400);
        Peer.getInstance().getExecutor().schedule(() -> {
            if(!Peer.getInstance().getProtocolInfo().isChunkAlreadyReceivedWhileReclaim(fileId, chunkNo)) {
                BackupInitiator backupInitiator = new BackupInitiator(filepath, repDegree, chunk);
                backupInitiator.run_one_chunk();
                Peer.getInstance().writePeerToFile();
            }
            Peer.getInstance().getProtocolInfo().setReclaimProtocol(false);
            Peer.getInstance().getProtocolInfo().clearChunksReceivedWhileReclaim();
        }, delay, TimeUnit.MILLISECONDS);
    }
}
