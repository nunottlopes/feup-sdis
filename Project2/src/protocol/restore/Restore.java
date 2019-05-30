package protocol.restore;

import message.Message;
import peer.Peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static globals.Globals.getFileData;
import static message.SendMessage.sendCHUNK;

/**
 * Restore class
 */
public class Restore {

    /**
     * File id
     */
    private String fileId;

    /**
     * Chunk number
     */
    private int chunkNo;


    private InetAddress address;

    /**
     * Restore constructor
     * @param msg
     * @param address
     */
    public Restore(Message msg, InetAddress address) {

        System.out.println("\n> " + msg.getType() + " received");
        System.out.println("- Sender Id = " + msg.getSenderId());
        System.out.println("- File Id = " + msg.getFileId());
        System.out.println("- Chunk No = " + msg.getChunkNo());

        this.fileId = msg.getFileId();
        this.chunkNo = msg.getChunkNo();
        this.address = address;

        if(Peer.getInstance().getFileManager().hasChunk(this.fileId, this.chunkNo)){
            start();
        }
    }

    public Restore(String fileId, int chunkNo, InetAddress address){
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.address = address;

        if(Peer.getInstance().getFileManager().hasChunk(fileId, chunkNo)){
            start();
        }
    }

    /**
     * Starts Restore protocol
     */
    private void start() {
        Random r = new Random();
        int delay = r.nextInt(400);
        Peer.getInstance().getExecutor().schedule(() -> {
            if(!Peer.getInstance().getProtocolInfo().isChunkAlreadySent(fileId, chunkNo)) {

                File file = new File(Peer.getInstance().getBackupPath(fileId) + chunkNo);
                byte[] body;
                try {
                    body = getFileData(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }

//                if(address.getHostAddress().equals(Peer.getInstance().getChord().getAddress())){
//                    Peer.getInstance().getProtocolInfo().chunkSent(fileId, chunkNo, body);
//                } else {
//                    sendCHUNK(fileId, chunkNo, body, address);
//                }
                sendCHUNK(fileId, chunkNo, body, address);
            } else {
                Peer.getInstance().getProtocolInfo().removeChunkFromSent(fileId, chunkNo);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

}
