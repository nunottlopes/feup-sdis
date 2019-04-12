package protocol.reclaim;

import message.Message;

public class Reclaim {
    public Reclaim(Message msg) {
        System.out.println("\n> REMOVED received");
        System.out.println("- Sender Id = " + msg.getSenderId());
        System.out.println("- File Id = " + msg.getFileId());
        System.out.println("- Chunk No = " + msg.getChunkNo());

        //TODO: HANDLE REMOVED MESSAGE
    }
}
