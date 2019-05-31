package protocol.reclaim;

import peer.Chunk;
import peer.FileManager;
import peer.Peer;
import protocol.InvalidProtocolExecution;
import java.util.List;

import chord.Chord;

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
        if(fm.getUsed_mem() < spaceReclaim){
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

        Chunk chunk;
        String path, fileId;
        int chunkNo, hash, i;
        for (i = 0; fm.getUsed_mem() > spaceReclaim && i < chunks.size(); i++){
            chunk = chunks.get(i);
            chunkNo = chunk.getChunkNo();
            fileId = chunk.getFileId();
            hash = Math.floorMod(Chord.sha1(fileId+chunkNo), Peer.getInstance().getMaxChordPeers());

            if (Peer.getInstance().getChord().amISuccessor(hash))
                continue;

            path = Peer.getInstance().getBackupPath(fileId);
            fm.removeChunkFile(path, Integer.toString(chunkNo), true);
            fm.removeChunk(fileId, chunkNo);
            fm.removeFolderIfEmpty(path);
            fm.updateFreeMem(spaceReclaim);
        }

        if (i == chunks.size()) { // Failed
            System.out.println("Could not reclaim for "+ spaceReclaim + " bytes");
        }
    }
}
