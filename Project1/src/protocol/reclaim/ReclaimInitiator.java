package protocol.reclaim;

import channel.Channel;
import message.Message;
import peer.Chunk;
import peer.ChunkComparator;
import peer.FileManager;
import peer.Peer;
import protocol.InvalidProtocolExecution;


import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ReclaimInitiator {

    private final FileManager fm;
    private long spaceReclaim; //in Bytes -> maximum disk space that can be used for storing chunks

    public ReclaimInitiator(long spaceReclaim){
        this.spaceReclaim = spaceReclaim * 1000;
        this.fm = Peer.getInstance().getFileManager();
    }

    public void run() throws InvalidProtocolExecution{
        if(spaceReclaim == 0){
            // Reclaim all the disk space being used by the service / Delete all chunks
            deleteAllChunks();
        }
        else if(fm.getUsed_mem() < spaceReclaim){
            // Only need to reduce free mem available for peer file manager
            fm.updateFreeMem(spaceReclaim);
        }
        else{
            // Choosing best chunks to be removed in order to limit peer memory
            removeNecessaryChunks();
        }
    }

    private void removeNecessaryChunks() {
        List<Chunk> chunks = fm.getAllStoredChunks();

        // SORT ORDER:
        // -> First the chunks that have greater perceived replication degree than desired replication degree
        // -> Second the chunks that have the same perceived replication degree and desired replication degree
        // -> Third the chunks that have less perceived replication degree than desired replication degree
        // NOTE: Inside each case the chunks are ordered by greater data size
        Collections.sort(chunks, new ChunkComparator());

        int i = 0;
        Chunk chunk;
        String path, fileId;
        int chunkNo;
        while (fm.getUsed_mem() > spaceReclaim){
            chunk = chunks.get(i);
            chunkNo = chunk.getChunkNo();
            fileId = chunk.getFileId();
            path = Peer.getInstance().getBackupPath(fileId);
            fm.removeChunkFile(path, Integer.toString(chunkNo));
            fm.removeChunk(chunk.getFileId(), chunkNo);
            fm.removeFolderIfEmpty(path);
            fm.updateFreeMem(spaceReclaim); //To avoid doing backup of delete filed
            sendREMOVED(fileId, chunkNo);
            i++;
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
                Peer.getInstance().getProtocolInfo().updateChunkRepDegree(fileId, chunkNo);
                sendREMOVED(fileId, chunkNo);
            }
            fm.getChunksStored().remove(fileId);
            fm.removeFileFolder(path);
        }
        fm.removeFolderIfEmpty(Peer.getInstance().getBackupFolder());
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
