package message;

import peer.Peer;

import java.net.InetAddress;

/**
 * SendMessage class
 */
public class SendMessage {

    /**
     * Sends STORED message
     * @param fileId
     * @param chunkNo
     */
    public static void sendSTORED(String fileId, int chunkNo, InetAddress destination) {
        String[] args = {
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo)
        };

        Message msg = new Message(Message.MessageType.STORED, args);

        Peer.getInstance().send(msg, destination);

        System.out.println("\n> STORED sent");
        System.out.println("- File Id = " + fileId);
        System.out.println("- Chunk No = " + chunkNo);
    }

    /**
     * Sends PUTCHUNK message
     * @param fileId
     * @param chunkNo
     * @param repDegree
     * @param data
     */
    public static void sendPUTCHUNK(String fileId, int chunkNo, int repDegree, byte[] data, InetAddress destination){
        String[] args = {
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo),
                Integer.toString(repDegree)
        };

        Message msg = new Message(Message.MessageType.PUTCHUNK, args, data);

        Peer.getInstance().send(msg, destination);

        System.out.println("\n> PUTCHUNK sent");
        System.out.println("- Address destination = " + destination);
        System.out.println("- Sender Id = " + Peer.getInstance().getId());
        System.out.println("- File Id = " + fileId);
        System.out.println("- Chunk No = " + chunkNo);
    }

    /**
     * Sends DELETE message
     * @param fileId
     */
    public static void sendDELETE(String fileId, InetAddress destination){
        String[] args = {
                Integer.toString(Peer.getInstance().getId()),
                fileId
        };

        Message msg = new Message(Message.MessageType.DELETE, args);

        Peer.getInstance().send(msg, destination);

//        System.out.println("\n> DELETE sent");
//        System.out.println("- Sender Id = " + Peer.getInstance().getId());
//        System.out.println("- File Id = " + fileId);
    }

    /**
     * Sends CHUNK message
     * @param fileId
     * @param chunkNo
     * @param body
     */
    public static void sendCHUNK(String fileId, int chunkNo, byte[] body, InetAddress destination){
        String[] args = {
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo)
        };

        Message msg = new Message(Message.MessageType.CHUNK, args, body);

        Peer.getInstance().send(msg, destination);

        System.out.println("\n> CHUNK sent");
        System.out.println("- Sender Id = " + Peer.getInstance().getId());
        System.out.println("- File Id = " + fileId);
        System.out.println("- Chunk No = " + chunkNo);
    }

    /**
     * Sends GETCHUNK message
     * @param fileId
     * @param chunkNo
     */
    public static void sendGETCHUNK(String fileId, int chunkNo, InetAddress destination){
        String[] args = {
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo)
        };

        Message msg = new Message(Message.MessageType.GETCHUNK, args);

        Peer.getInstance().send(msg, destination);

        System.out.println("\n> GETCHUNK sent");
        System.out.println("- Sender Id = " + Peer.getInstance().getId());
        System.out.println("- File Id = " + fileId);
        System.out.println("- Chunk No = " + chunkNo);
    }
}
