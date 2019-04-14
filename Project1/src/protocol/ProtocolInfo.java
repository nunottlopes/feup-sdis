package protocol;

import peer.Peer;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolInfo implements Serializable {

    private boolean reclaimProtocol = false;
    private ConcurrentHashMap<String, Set<Integer>> chunksReceivedWhileReclaim;

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer> > > chunksRepDegree;
    private ConcurrentHashMap<String, Set<Integer> > chunksSent;

    //Initiator
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer> > > backupRepDegree_init;
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, byte[] > > restoredChunks_init;

    public ProtocolInfo() {
        chunksRepDegree = new ConcurrentHashMap<>();
        backupRepDegree_init = new ConcurrentHashMap<>();
        restoredChunks_init = new ConcurrentHashMap<>();
        chunksSent = new ConcurrentHashMap<>();
        chunksReceivedWhileReclaim = new ConcurrentHashMap<>();
    }

    public void startBackup(String fileId) {
        backupRepDegree_init.put(fileId, new ConcurrentHashMap<>());
    }

    public void endBackup(String fileId, int repDegree, String path) {
        Peer.getInstance().getFileManager().addBackedupChunk(fileId, backupRepDegree_init.get(fileId), repDegree, path);
        backupRepDegree_init.remove(fileId);
    }

    public void startRestore(String fileId) {
        restoredChunks_init.put(fileId, new ConcurrentHashMap<>());
    }

    public void endRestore(String fileId) {
        restoredChunks_init.remove(fileId);
    }

    public void incRepDegree(String fileId, int chunkNo, int peerId) {

//        System.out.println("\n> STORED received");
//        System.out.println("- Sender Id = " + peerId);
//        System.out.println("- File Id = " + fileId);
//        System.out.println("- Chunk No = " + chunkNo);

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

    public int getChunkRepDegree(String fileId, int chunkNo) {
        if(backupRepDegree_init.containsKey(fileId)) {
            backupRepDegree_init.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
            return backupRepDegree_init.get(fileId).get(chunkNo).size();
        } else {
            if(chunksRepDegree.containsKey(fileId)) {
                if(chunksRepDegree.get(fileId).containsKey(chunkNo)) {
                    return chunksRepDegree.get(fileId).get(chunkNo).size();
                }
            }
        }

        return 0;
    }

    public void chunkSent(String fileId, int chunkNo, byte[] body) {
        if(restoredChunks_init.containsKey(fileId)) {
            restoredChunks_init.get(fileId).putIfAbsent(chunkNo, body);
        }
        else {
            chunksSent.putIfAbsent(fileId, new HashSet<>());
            chunksSent.get(fileId).add(chunkNo);
        }
    }

    public int getChunksRecieved(String fileId) { return restoredChunks_init.get(fileId).size(); }

    public boolean hasReceivedChunk(String fileId, int chunkNo) {
        if(restoredChunks_init.containsKey(fileId)) {
            return restoredChunks_init.get(fileId).containsKey(chunkNo);
        }
        return false;
    }

    public boolean isChunkAlreadySent(String fileId, int chunkNo) {
        if(chunksSent.containsKey(fileId)) {
            return chunksSent.get(fileId).contains(chunkNo);
        }
        return false;
    }

    public void removeChunkFromSent(String fileId, int chunkNo) {
        chunksSent.get(fileId).remove(chunkNo);
    }

    public byte[] getChunkData(String fileId, int chunkNo) {
        return restoredChunks_init.get(fileId).get(chunkNo);
    }

    public boolean isReclaimProtocol() {
        return reclaimProtocol;
    }

    public void setReclaimProtocol(boolean reclaimProtocol) {
        this.reclaimProtocol = reclaimProtocol;
    }

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

    public boolean isChunkAlreadyReceivedWhileReclaim(String fileId, int chunkNo){
        if(chunksReceivedWhileReclaim.containsKey(fileId)){
            return chunksReceivedWhileReclaim.get(fileId).contains(chunkNo);
        }
        return false;
    }

    public void clearChunksReceivedWhileReclaim(){
        chunksReceivedWhileReclaim = new ConcurrentHashMap<>();
    }

    public void updateChunkRepDegree(String fileId, int chunkNo) {
        if(chunksRepDegree.containsKey(fileId) && chunksRepDegree.get(fileId).containsKey(chunkNo))
            chunksRepDegree.get(fileId).get(chunkNo).remove(Peer.getInstance().getId());
    }

    public void removeChunksRepDegree(String fileId) {
        chunksRepDegree.remove(fileId);
    }
}
