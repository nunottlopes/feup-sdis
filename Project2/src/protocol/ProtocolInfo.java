package protocol;

import peer.Peer;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProtocolInfo class
 */
public class ProtocolInfo implements Serializable {

    /**
     * Backup Initiator perceived replication degree
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer> > > backupRepDegree_init;

    /**
     * Restore Initiator file data to be saved
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, byte[] > > restoredChunks_init;

    /**
     * Running Reclaim protocol state
     */
    private boolean reclaimProtocol = false;

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<InetAddress> > > peersWhoSavedChunk;



    /**
     * ProtocolInfo constructor
     */
    public ProtocolInfo() {
        backupRepDegree_init = new ConcurrentHashMap<>();
        restoredChunks_init = new ConcurrentHashMap<>();
        peersWhoSavedChunk = new ConcurrentHashMap<>();
    }

    /**
     * Start Backup
     * @param fileId
     */
    public void startBackup(String fileId) {
        backupRepDegree_init.put(fileId, new ConcurrentHashMap<>());
        peersWhoSavedChunk.put(fileId, new ConcurrentHashMap<>());
    }

    /**
     * End Backup
     * @param fileId
     * @param repDegree
     * @param path
     */
    public void endBackup(String fileId, int repDegree, String path) {
        Peer.getInstance().getFileManager().addBackedupChunk(fileId, backupRepDegree_init.get(fileId), repDegree, path);
        backupRepDegree_init.remove(fileId);
        peersWhoSavedChunk.remove(fileId);
    }

    /**
     * Start Restore
     * @param fileId
     */
    public void startRestore(String fileId) {
        restoredChunks_init.put(fileId, new ConcurrentHashMap<>());
    }

    /**
     * End Restore
     * @param fileId
     */
    public void endRestore(String fileId) {
        restoredChunks_init.remove(fileId);
    }


    /**
     * Return chunk perceived replication degree
     * @param fileId
     * @param chunkNo
     * @return perceived replication degree
     */
    public int getChunkRepDegree(String fileId, int chunkNo) {
        if(peersWhoSavedChunk.get(fileId).containsKey(chunkNo))
            return peersWhoSavedChunk.get(fileId).get(chunkNo).size();

        return 0;
    }

    /**
     * Adds chunk to restored chunks or sent chunks for restored protocol
     * @param fileId
     * @param chunkNo
     * @param body
     */
    public void addReceivedChunk(String fileId, int chunkNo, byte[] body) {

        System.out.println("\n> CHUNK received");
        System.out.println("- File Id = " + fileId);
        System.out.println("- Chunk No = " + chunkNo);

        if(restoredChunks_init.containsKey(fileId)) {
            restoredChunks_init.get(fileId).putIfAbsent(chunkNo, body);
        }
    }

    /**
     * Returns chunks received in restore protocol
     * @param fileId
     * @return
     */
    public int getChunksRecieved(String fileId) { return restoredChunks_init.get(fileId).size(); }

    /**
     * Checks if peer has received chunk from restore protocol
     * @param fileId
     * @param chunkNo
     * @return true if it has received, false otherwise
     */
    public boolean hasReceivedChunk(String fileId, int chunkNo) {
        if(restoredChunks_init.containsKey(fileId)) {
            return restoredChunks_init.get(fileId).containsKey(chunkNo);
        }
        return false;
    }

    /**
     * Return restored chunk data
     * @param fileId
     * @param chunkNo
     * @return chunk data
     */
    public byte[] getChunkData(String fileId, int chunkNo) {
        return restoredChunks_init.get(fileId).get(chunkNo);
    }

    public void addPeerSavedChunk(String fileId, int chunkNo, InetAddress address, int peerid) {
        System.out.println("\n> STORED received");
//        System.out.println("- Sender Address = " + address);
        System.out.println("- File Id = " + fileId);
        System.out.println("- Chunk No = " + chunkNo);

        peersWhoSavedChunk.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
        peersWhoSavedChunk.get(fileId).get(chunkNo).add(address);

        backupRepDegree_init.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
        backupRepDegree_init.get(fileId).get(chunkNo).add(peerid);
    }

    public boolean hasPeerSavedChunk(String fileId, int chunkNo, InetAddress address){
        if(peersWhoSavedChunk.containsKey(fileId) && peersWhoSavedChunk.get(fileId).containsKey(chunkNo)){
            return peersWhoSavedChunk.get(fileId).get(chunkNo).contains(address);
        }
        return false;
    }
}
