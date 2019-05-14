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
    String fileId;

    /**
     * Chunk number
     */
    int chunkNo;

    /**
     * TCP port
     */
    int portTCP;

    /**
     * TCP Inet Address
     */
    InetAddress addressTCP;

    /**
     * Restore constructor
     * @param msg
     * @param address
     */
    public Restore(Message msg, InetAddress address) {
        if(msg.getType() == Message.MessageType.GETCHUNKENH && !Peer.getInstance().isEnhanced()) {
            System.out.println("-- Incompatible peer version with restore enhancement --");
            return;
        }
        if(msg.getType() == Message.MessageType.GETCHUNK && Peer.getInstance().isEnhanced()) {
            System.out.println("-- Incompatible peer version with vanilla restore --");
            return;
        }

//        System.out.println("\n> " + msg.getType() + " received");
//        System.out.println("- Sender Id = " + msg.getSenderId());
//        System.out.println("- File Id = " + msg.getFileId());
//        System.out.println("- Chunk No = " + msg.getChunkNo());

        this.fileId = msg.getFileId();
        this.chunkNo = msg.getChunkNo();
        if(Peer.getInstance().isEnhanced()) {
            this.portTCP = msg.getPort();
            this.addressTCP = address;
        }

        start(Peer.getInstance().getFileManager().hasChunk(fileId, chunkNo));
    }

    /**
     * Starts Restore protocol
     */
    private void start(boolean send) {
        Random r = new Random();
        int delay = r.nextInt(400);
        Peer.getInstance().getExecutor().schedule(() -> {
            if(!Peer.getInstance().getProtocolInfo().isChunkAlreadySent(fileId, chunkNo) && send) {

                File file = new File(Peer.getInstance().getBackupPath(fileId) + chunkNo);
                byte[] body;
                try {
                    body = getFileData(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }

                if(Peer.getInstance().isEnhanced()) {
                    sendCHUNK(fileId, chunkNo, body, addressTCP, portTCP);
                } else {
                    sendCHUNK(fileId, chunkNo, body);
                }

            } else {
                Peer.getInstance().getProtocolInfo().removeChunkFromSent(fileId, chunkNo);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

}
