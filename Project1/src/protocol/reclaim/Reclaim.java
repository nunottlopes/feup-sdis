package protocol.reclaim;

import message.Message;
import peer.Chunk;
import peer.Peer;
import protocol.InvalidProtocolExecution;
import protocol.backup.BackupInitiator;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Reclaim {

    String fileId;
    int chunkNo;
    int repDegree;
    int senderId;
    String filepath;
    Chunk chunk;

    public Reclaim(Message msg) {
        System.out.println("\n> REMOVED received");
        System.out.println("- Sender Id = " + msg.getSenderId());
        System.out.println("- File Id = " + msg.getFileId());
        System.out.println("- Chunk No = " + msg.getChunkNo());

        this.fileId = msg.getFileId();
        this.chunkNo = msg.getChunkNo();
        this.senderId = msg.getSenderId();
        this.filepath = Peer.getInstance().getBackupPath(fileId) + "/" + chunkNo;
        this.chunk = null;

        start();
    }

    private void start() {
        if(!Peer.getInstance().getFileManager().hasChunk(fileId, chunkNo)){
            //System.out.println("Does not have chunk asked");
            return;
        }
        chunk = Peer.getInstance().getFileManager().getChunkFromFile(fileId, chunkNo);

        chunk.removePerceivedRepDegreePeer(senderId);
        if(chunk.getPerceivedRepDegree() >= chunk.getRepDegree()){
            //System.out.println("SAME PERCEIVED AND DESIRED REP DEGREE");
            //System.out.println(chunk.getPerceivedRepDegree());
            //System.out.println(chunk.getRepDegree());
            return;
        }
        this.repDegree = chunk.getRepDegree();

        Peer.getInstance().getProtocolInfo().setReclaimProtocol(true);


        Random r = new Random();
        int delay = r.nextInt(400);
        Peer.getInstance().getExecutor().schedule(() -> {
            if(!Peer.getInstance().getProtocolInfo().isChunkAlreadyReceivedWhileReclaim(fileId, chunkNo)) {
                System.out.println("\n> PUTCHUNK Sent");
                System.out.println("- Sender Id = " + senderId);
                System.out.println("- File Id = " + fileId);
                System.out.println("- Chunk No = " + chunkNo);
                BackupInitiator backupInitiator = new BackupInitiator(filepath, repDegree, chunk);
                try {
                    backupInitiator.run_one_chunk();
                } catch (InvalidProtocolExecution e) {
                    System.out.println(e);
                }
                Peer.getInstance().writePeerToFile();
            }
            Peer.getInstance().getProtocolInfo().setReclaimProtocol(false);
            Peer.getInstance().getProtocolInfo().clearChunksReceivedWhileReclaim();
        }, delay, TimeUnit.MILLISECONDS);
    }
}
