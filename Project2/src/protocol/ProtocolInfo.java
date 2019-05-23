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
     * Saving chunks perceived replication degree
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer> > > chunksRepDegree;

    /**
     * Chunks sent in restore protocol
     */
    private ConcurrentHashMap<String, Set<Integer> > chunksSent;

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

    /**
     * Chunks number received while on protocol reclaim
     */
    private ConcurrentHashMap<String, Set<Integer>> chunksReceivedWhileReclaim;

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<InetAddress> > > peersWhoSavedChunk;

    /**
     * ProtocolInfo constructor
     */
    public ProtocolInfo() {
        chunksRepDegree = new ConcurrentHashMap<>();
        backupRepDegree_init = new ConcurrentHashMap<>();
        restoredChunks_init = new ConcurrentHashMap<>();
        chunksSent = new ConcurrentHashMap<>();
        chunksReceivedWhileReclaim = new ConcurrentHashMap<>();
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
     * Update perceived replication degree
     * @param fileId
     * @param chunkNo
     * @param peerId
     */
    public void incRepDegree(String fileId, int chunkNo, int peerId) {

        System.out.println("\n> STORED received");
        System.out.println("- Sender Id = " + peerId);
        System.out.println("- File Id = " + fileId);
        System.out.println("- Chunk No = " + chunkNo);

        if(backupRepDegree_init.containsKey(fileId)) {
            backupRepDegree_init.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
            backupRepDegree_init.get(fileId).get(chunkNo).add(peerId);
        }

        else {
            chunksRepDegree.putIfAbsent(fileId, new ConcurrentHashMap<>());
            chunksRepDegree.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
            chunksRepDegree.get(fileId).get(chunkNo).add(peerId);

            Peer.getInstance().getFileManager().updateStoredChunks(fileId, chunksRepDegree.get(fileId));
        }
    }

    /**
     * Return chunk perceived replication degree
     * @param fileId
     * @param chunkNo
     * @return perceived replication degree
     */
    public int getChunkRepDegree(String fileId, int chunkNo) {
//        if(backupRepDegree_init.containsKey(fileId)) {
//            backupRepDegree_init.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
//            return backupRepDegree_init.get(fileId).get(chunkNo).size();
//        } else {
//            if(chunksRepDegree.containsKey(fileId)) {
//                if(chunksRepDegree.get(fileId).containsKey(chunkNo)) {
//                    return chunksRepDegree.get(fileId).get(chunkNo).size();
//                }
//            }
//        }
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
    public void chunkSent(String fileId, int chunkNo, byte[] body) {
        if(restoredChunks_init.containsKey(fileId)) {
            restoredChunks_init.get(fileId).putIfAbsent(chunkNo, body);
        }
        else {
            chunksSent.putIfAbsent(fileId, new HashSet<>());
            chunksSent.get(fileId).add(chunkNo);
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
     * Checks if chunk was already sent in restore protocol
     * @param fileId
     * @param chunkNo
     * @return true if it was already sent, false otherwise
     */
    public boolean isChunkAlreadySent(String fileId, int chunkNo) {
        if(chunksSent.containsKey(fileId)) {
            return chunksSent.get(fileId).contains(chunkNo);
        }
        return false;
    }

    /**
     * Removes chunks from chunks sent in restore protocol
     * @param fileId
     * @param chunkNo
     */
    public void removeChunkFromSent(String fileId, int chunkNo) {
        chunksSent.get(fileId).remove(chunkNo);
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

    /**
     * Return true is Peer is executing reclaim protocol
     * @return true if it is, false otherwise
     */
    public boolean isReclaimProtocol() {
        return reclaimProtocol;
    }

    /**
     * Sets reclaim protocol status
     * @param reclaimProtocol
     */
    public void setReclaimProtocol(boolean reclaimProtocol) {
        this.reclaimProtocol = reclaimProtocol;
    }

    /**
     * Adds chunk to chunksReceivedWhileReclaim when it's on reclaim protocol
     * @param fileId
     * @param chunkNo
     */
    public void addChunksReceivedWhileReclaim(String fileId, int chunkNo){
        if (chunksReceivedWhileReclaim.containsKey(fileId)){
            chunksReceivedWhileReclaim.get(fileId).add(chunkNo);
        }
        else{
            Set<Integer> chunks = new HashSet<>();
            chunks.add(chunkNo);
            chunksReceivedWhileReclaim.put(fileId, chunks);
        }
    }

    /**
     * Checks if chunk was already sent on reclaim protocol
     * @param fileId
     * @param chunkNo
     * @return
     */
    public boolean isChunkAlreadyReceivedWhileReclaim(String fileId, int chunkNo){
        if(chunksReceivedWhileReclaim.containsKey(fileId)){
            return chunksReceivedWhileReclaim.get(fileId).contains(chunkNo);
        }
        return false;
    }

    /**
     * Removes chunks from chunksReceivedWhileReclaim
     */
    public void clearChunksReceivedWhileReclaim(){
        chunksReceivedWhileReclaim = new ConcurrentHashMap<>();
    }

    /**
     * Updates chunks perceived replication degree
     * @param fileId
     * @param chunkNo
     */
    public void updateChunkRepDegree(String fileId, int chunkNo) {
        if(chunksRepDegree.containsKey(fileId) && chunksRepDegree.get(fileId).containsKey(chunkNo))
            chunksRepDegree.get(fileId).get(chunkNo).remove(Peer.getInstance().getId());
    }

    /**
     * Updates chunks perceived replication degree
     * @param fileId
     * @param chunkNo
     * @param senderId
     */
    public void updateChunkRepDegree(String fileId, int chunkNo, int senderId) {
        if(chunksRepDegree.containsKey(fileId) && chunksRepDegree.get(fileId).containsKey(chunkNo))
            chunksRepDegree.get(fileId).get(chunkNo).remove(senderId);

    }

    /**
     * Removes file from chunksRepDegree
     * @param fileId
     */
    public void removeChunksRepDegree(String fileId) {
        chunksRepDegree.remove(fileId);
    }

    public void addPeerSavedChunk(String fileId, int chunkNo, InetAddress address) {
        System.out.println("\n> STORED received");
        System.out.println("- Sender Address = " + address);
        System.out.println("- File Id = " + fileId);
        System.out.println("- Chunk No = " + chunkNo);

        peersWhoSavedChunk.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
        peersWhoSavedChunk.get(fileId).get(chunkNo).add(address);
    }

    public boolean hasPeerSavedChunk(String fileId, int chunkNo, InetAddress address){
        if(peersWhoSavedChunk.containsKey(fileId) && peersWhoSavedChunk.get(fileId).containsKey(chunkNo)){
            return peersWhoSavedChunk.get(fileId).get(chunkNo).contains(address);
        }
        return false;
    }
}
