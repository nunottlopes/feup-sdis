package protocol.reclaim;

import peer.Chunk;
import peer.ChunkComparator;
import peer.FileManager;
import peer.Peer;
import protocol.InvalidProtocolExecution;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import static message.SendMessage.sendREMOVED;

/**
 * ReclaimInitiator class
 */
public class ReclaimInitiator {

    /**
     * Peer file manager
     */
    private final FileManager fm;

    /**
     * Peer max disk space that can be used for storing chunks
     * In Bytes
     */
    private long spaceReclaim;

    /**
     * ReclaimInitiator constructor
     * @param spaceReclaim
     */
    public ReclaimInitiator(long spaceReclaim){
        this.spaceReclaim = spaceReclaim * 1000;
        this.fm = Peer.getInstance().getFileManager();
    }

    /**
     * Runs Reclaim protocol for initiator-peer
     * @throws InvalidProtocolExecution
     */
    public void run() throws InvalidProtocolExecution{
        if(spaceReclaim == 0){
            // Remove all chunks. Faster than removeNecessaryChunks
            deleteAllChunks();
        }
        else if(fm.getUsed_mem() < spaceReclaim){
            // Only need to reduce free mem available for peer file manager
            fm.updateFreeMem(spaceReclaim);
        }
        else{
            removeNecessaryChunks();
        }
    }

    /**
     * Removes only the necessary best chunks in order to limit peer memory
     */
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
            fm.removeChunkFile(path, Integer.toString(chunkNo), true);
            fm.removeChunk(fileId, chunkNo);
            fm.removeFolderIfEmpty(path);
            fm.updateFreeMem(spaceReclaim); //To avoid doing backup of delete filed
            Peer.getInstance().getProtocolInfo().updateChunkRepDegree(fileId, chunkNo);
            sendREMOVED(fileId, chunkNo);
            i++;
        }
    }

    /**
     * Removes all chunks when Space Reclaim value is 0
     */
    private void deleteAllChunks() {
        String fileId, path;
        fm.setFree_mem(0);
        for(ConcurrentHashMap.Entry<String, ConcurrentHashMap<Integer, Chunk>> entry_file : fm.getChunksStored().entrySet()){
            fileId = entry_file.getKey();
            path = Peer.getInstance().getBackupPath(fileId);
            for(ConcurrentHashMap.Entry<Integer, Chunk> entry_chunk : entry_file.getValue().entrySet()){
                int chunkNo = entry_chunk.getValue().getChunkNo();
                Peer.getInstance().getProtocolInfo().updateChunkRepDegree(fileId, chunkNo);
                sendREMOVED(fileId, chunkNo);
            }
            fm.getChunksStored().remove(fileId);
            fm.removeFileFolder(path, false);
        }
        fm.removeFolderIfEmpty(Peer.getInstance().getBackupFolder());
    }
}
