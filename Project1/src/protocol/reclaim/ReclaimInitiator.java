package protocol.reclaim;

import channel.Channel;
import message.Message;
import peer.Chunk;
import peer.FileManager;
import peer.Peer;
import protocol.InvalidProtocolExecution;

import java.util.concurrent.ConcurrentHashMap;

public class ReclaimInitiator {

    private final FileManager fm;
    private long spaceReclaim; //in Bytes -> maximum disk space that can be used for storing chunks

    public ReclaimInitiator(long spaceReclaim){
        this.spaceReclaim = spaceReclaim * 1000;
        this.fm = Peer.getInstance().getFileManager();
    }

    public void run() throws InvalidProtocolExecution {
        if(spaceReclaim == 0){
            // reclaim all the disk space being used by the service
            deleteAllChunks();
        }
        else if(fm.getUsed_mem() < spaceReclaim){
            // only need to reduce free mem available for peer file manager
            fm.updateFreeMem(spaceReclaim);
        }
        else{
            //TODO: algorithm to choose which chunks to delete in order to have the space reclaim asked
        }
    }

    private void deleteAllChunks() {
        String fileId, path;
        for(ConcurrentHashMap.Entry<String, ConcurrentHashMap<Integer, Chunk>> entry_file : fm.getChunksStored().entrySet()){
            fileId = entry_file.getKey();
            path = Peer.getInstance().getBackupPath(fileId);
            for(ConcurrentHashMap.Entry<Integer, Chunk> entry_chunk : entry_file.getValue().entrySet()){
                int chunkNo = entry_chunk.getValue().getChunkNo();
                fm.removeChunkFile(path, Integer.toString(chunkNo));
                sendREMOVED(fileId, chunkNo);
            }
            fm.getChunksStored().remove(fileId);
            this.fm.removeFileFolder(path);
        }
    }

    private void sendREMOVED(String fileId, int chunkNo) {
        String[] args = {
                Peer.getInstance().getVersion(),
                Integer.toString(Peer.getInstance().getId()),
                fileId,
                Integer.toString(chunkNo)
        };

        Message msg = new Message(Message.MessageType.REMOVED, args);

        Peer.getInstance().send(Channel.Type.MC, msg);

        System.out.println("\n> REMOVED sent");
        System.out.println("- File Id = " + fileId);
        System.out.println("- Chunk No = " + chunkNo);
    }
}
