package protocol.reclaim;

import channel.Channel;
import message.Message;
import peer.Chunk;
import peer.Peer;
import protocol.InvalidProtocolExecution;
import protocol.backup.BackupInitiator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static globals.Globals.getFileData;


//Upon receiving this message, a peer that has a local copy of the chunk shall update its local count of this chunk. If this
//count drops below the desired replication degree of that chunk, it shall initiate the chunk backup subprotocol after a random
//delay uniformly distributed between 0 and 400 ms. If during this delay, a peer receives a PUTCHUNK message for the same file
//chunk, it should back off and restrain from starting yet another backup subprotocol for that file chunk.

public class Reclaim {

    String fileId;
    int chunkNo;
    int repDegree;
    int senderId;
    String filepath;

    public Reclaim(Message msg) {
        System.out.println("\n> REMOVED received");
        System.out.println("- Sender Id = " + msg.getSenderId());
        System.out.println("- File Id = " + msg.getFileId());
        System.out.println("- Chunk No = " + msg.getChunkNo());

        this.fileId = msg.getFileId();
        this.chunkNo = msg.getChunkNo();
        this.senderId = msg.getSenderId();
        this.filepath = Peer.getInstance().getBackupPath(fileId) + "/" + chunkNo;

        start();
    }

    private void start() {
        if(!Peer.getInstance().getFileManager().hasChunk(fileId, chunkNo))
            return;

        Chunk chunk = null;
        try {
            chunk = Peer.getInstance().getFileManager().getChunkFromFile(fileId, chunkNo);
        } catch (IOException e) {
            System.out.println(e);
        }
        chunk.removePerceivedRepDegreePeer(senderId);
        if(chunk.getPerceivedRepDegree() >= chunk.getRepDegree())
            return;

        this.repDegree = chunk.getRepDegree();

        Peer.getInstance().getProtocolInfo().setReclaimProtocol(true);


        Random r = new Random();
        int delay = r.nextInt(400);
        Chunk finalChunk = chunk;
        Peer.getInstance().getExecutor().schedule(() -> {
            if(!Peer.getInstance().getProtocolInfo().isChunkAlreadyReceivedWhileReclaim(fileId, chunkNo)) {
                ArrayList<Chunk> chunks = new ArrayList<>();
                chunks.add(finalChunk);
                System.out.println("\n> PUTCHUNK Sent");
                System.out.println("- Sender Id = " + senderId);
                System.out.println("- File Id = " + fileId);
                System.out.println("- Chunk No = " + chunkNo);
                BackupInitiator backupInitiator = new BackupInitiator(filepath, repDegree, chunks);
                try {
                    backupInitiator.run();
                } catch (InvalidProtocolExecution e) {
                    System.out.println(e);
                }
                Peer.getInstance().writePeerToFile();
            }
            else{
                System.out.println("DENIED");
            }
            System.out.println("AQUI");
            Peer.getInstance().getProtocolInfo().setReclaimProtocol(false);
            Peer.getInstance().getProtocolInfo().clearChunksReceivedWhileReclaim();
        }, delay, TimeUnit.MILLISECONDS);
    }
}
