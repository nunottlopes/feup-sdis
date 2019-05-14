package message;

import channel.Channel;
import peer.Peer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * SendMessage class
 */
public class SendMessage {

    /**
     * Sends REMOVED message on the MC channel
     * @param fileId
     * @param chunkNo
     */
    public static void sendREMOVED(String fileId, int chunkNo) {
        String[] args = {
                Peer.getInstance().getVersion(),
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo)
        };

        Message msg = new Message(Message.MessageType.REMOVED, args);

        Peer.getInstance().send(Channel.Type.MC, msg);

//        System.out.println("\n> REMOVED sent");
//        System.out.println("- File Id = " + fileId);
//        System.out.println("- Chunk No = " + chunkNo);
    }

    /**
     * Sends STORED message on the MC channel
     * @param fileId
     * @param chunkNo
     */
    public static void sendSTORED(String fileId, int chunkNo) {
        String[] args = {
                Peer.getInstance().getVersion(),
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo)
        };

        Message msg = new Message(Message.MessageType.STORED, args);

        Peer.getInstance().send(Channel.Type.MC, msg);

//        System.out.println("\n> STORED sent");
//        System.out.println("- File Id = " + chunk.getFileId());
//        System.out.println("- Chunk No = " + chunk.getChunkNo());
    }

    /**
     * Sends PUTCHUNK message on the MDB channel
     * @param fileId
     * @param chunkNo
     * @param repDegree
     * @param data
     */
    public static void sendPUTCHUNK(String fileId, int chunkNo, int repDegree, byte[] data){
        String[] args = {
                Peer.getInstance().getVersion(),
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo),
                Integer.toString(repDegree)
        };

        Message msg = new Message(Message.MessageType.PUTCHUNK, args, data);

        Peer.getInstance().send(Channel.Type.MDB, msg);

//        System.out.println("\n> PUTCHUNK sent");
//        System.out.println("- Sender Id = " + Peer.getInstance().getId());
//        System.out.println("- File Id = " + fileId);
//        System.out.println("- Chunk No = " + chunkNo);
    }

    /**
     * Sends DELETE message on the MC channel
     * @param fileId
     */
    public static void sendDELETE(String fileId){
        String[] args = {
                Peer.getInstance().getVersion(),
                Integer.toString(Peer.getInstance().getId()),
                fileId
        };

        Message msg = new Message(Message.MessageType.DELETE, args);

        Peer.getInstance().send(Channel.Type.MC, msg);

//        System.out.println("\n> DELETE sent");
//        System.out.println("- Sender Id = " + Peer.getInstance().getId());
//        System.out.println("- File Id = " + fileId);
    }

    /**
     * Sends CHUNK message on the MDR channel
     * @param fileId
     * @param chunkNo
     * @param body
     */
    public static void sendCHUNK(String fileId, int chunkNo, byte[] body){
        String[] args = {
                Peer.getInstance().getVersion(),
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo)
        };

        Message msg = new Message(Message.MessageType.CHUNK, args, body);

        Peer.getInstance().send(Channel.Type.MDR, msg);

//        System.out.println("\n> CHUNK sent");
//        System.out.println("- Sender Id = " + Peer.getInstance().getId());
//        System.out.println("- File Id = " + fileId);
//        System.out.println("- Chunk No = " + chunkNo);
    }

    /**
     * Sends CHUNK message on the MDR channel and uses TCP for enhanced version of restore protocol
     * @param fileId
     * @param chunkNo
     * @param body
     * @param addressTCP
     * @param portTCP
     */
    public static void sendCHUNK(String fileId, int chunkNo, byte[] body, InetAddress addressTCP, int portTCP){
        String[] args = {
                Peer.getInstance().getVersion(),
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo)
        };

        sendTCP(new Message(Message.MessageType.CHUNK, args, body), addressTCP, portTCP, chunkNo);
        Peer.getInstance().send(Channel.Type.MDR, new Message(Message.MessageType.CHUNK, args));

//        System.out.println("\n> CHUNK sent");
//        System.out.println("- Sender Id = " + Peer.getInstance().getId());
//        System.out.println("- File Id = " + fileId);
//        System.out.println("- Chunk No = " + chunkNo);
    }

    /**
     * Sends message using TCP
     * @param msg
     * @param addressTCP
     * @param portTCP
     * @param chunkNo
     */
    public static void sendTCP(Message msg, InetAddress addressTCP, int portTCP, int chunkNo) {
        Socket socket;

        try {
            socket = new Socket(addressTCP, portTCP);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(msg);
            out.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Error sending chunk " + chunkNo + " via TCP");
        }
    }

    /**
     * Sends GETCHUNK message on the MC channel
     * @param fileId
     * @param chunkNo
     */
    public static void sendGETCHUNK(String fileId, int chunkNo){
        String[] args = {
                Peer.getInstance().getVersion(),
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo)
        };

        Message msg = new Message(Message.MessageType.GETCHUNK, args);

        Peer.getInstance().send(Channel.Type.MC, msg);
    }

    /**
     * Sends GETCHUNKENH message on the MC channel
     * @param fileId
     * @param chunkNo
     * @param tcpPort
     */
    public static void sendGETCHUNKENH(String fileId, int chunkNo, int tcpPort){
        String[] args = {
                Peer.getInstance().getVersion(),
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo),
                Integer.toString(tcpPort)
        };

        Message msg = new Message(Message.MessageType.GETCHUNKENH, args);

        Peer.getInstance().send(Channel.Type.MC, msg);
    }
}
