package protocol.reclaim;

import channel.Channel;
import message.Message;
import peer.Chunk;
import peer.Peer;

import java.io.File;
import java.io.FileNotFoundException;
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

    public Reclaim(Message msg) {
        System.out.println("\n> REMOVED received");
        System.out.println("- Sender Id = " + msg.getSenderId());
        System.out.println("- File Id = " + msg.getFileId());
        System.out.println("- Chunk No = " + msg.getChunkNo());

        this.fileId = msg.getFileId();
        this.chunkNo = msg.getChunkNo();
        this.senderId = msg.getSenderId();

        start();
    }

    private void start() {
        if(!Peer.getInstance().getFileManager().hasChunk(fileId, chunkNo))
            return;

        Chunk chunk = Peer.getInstance().getFileManager().getChunk(fileId, chunkNo);
        chunk.removePerceivedRepDegreePeer(senderId);
        if(chunk.getPerceivedRepDegree() >= chunk.getRepDegree())
            return;

        this.repDegree = chunk.getRepDegree();

        //TODO:
        //Chamar protocolo BackupInitiator se não receber nenhum PUTCHUNK dentro de 0 e 400 ms (random)
        // chamar só para um chunk e não um file

//        Random r = new Random();
//        int delay = r.nextInt(400);
//        Peer.getInstance().getExecutor().schedule(() -> {
//            //if(!Peer.getInstance().getProtocolInfo().isChunkAlreadySent(fileId, chunkNo)) {
//
//                String[] args = {
//                        Peer.getInstance().getVersion(),
//                        Integer.toString(Peer.getInstance().getId()),
//                        fileId,
//                        Integer.toString(chunkNo),
//                        Integer.toString(repDegree)
//                };
//
//                File file = new File(Peer.getInstance().getBackupPath(fileId) + chunkNo);
//                byte[] body;
//                try {
//                    body = getFileData(file);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                    return;
//                }
//                Message msg = new Message(Message.MessageType.PUTCHUNK, args, body);
//                Peer.getInstance().send(Channel.Type.MDR, msg);
//
//            //} else {
//              //  Peer.getInstance().getProtocolInfo().removeChunkFromSent(fileId, chunkNo);
//            //}
//        }, delay, TimeUnit.MILLISECONDS);
    }
}
