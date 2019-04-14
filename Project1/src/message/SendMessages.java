package message;

import channel.Channel;
import peer.Peer;

public class SendMessages {

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
}
