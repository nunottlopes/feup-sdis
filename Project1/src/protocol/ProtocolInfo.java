package protocol;

import peer.Peer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolInfo {

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer> > > chunksRepDegree;
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer> > > backupRepDegree;

    private boolean backingUp;

    public ProtocolInfo() {
        chunksRepDegree = new ConcurrentHashMap<>();
        backupRepDegree = new ConcurrentHashMap<>();
        backingUp = false;
    }

    public void startBackup(String fileId) {
        backingUp = true;
        backupRepDegree.putIfAbsent(fileId, new ConcurrentHashMap<>());
    }

    public void endBackup(String fileId, int repDegree, String path) {
        backingUp = false;
        Peer.getInstance().getFileManager().addBackedupChunk(fileId, backupRepDegree.get(fileId), repDegree, path);
        backupRepDegree.remove(fileId);
    }

    public void incRepDegree(String fileId, int chunkNo, int peerId) {
        if(backingUp) {
            backupRepDegree.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
            backupRepDegree.get(fileId).get(chunkNo).add(peerId);
        }

        else {
            if(Peer.getInstance().getFileManager().hasChunk(fileId, chunkNo)) {
                chunksRepDegree.putIfAbsent(fileId, new ConcurrentHashMap<>());
                chunksRepDegree.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
                chunksRepDegree.get(fileId).get(chunkNo).add(peerId);
            }
        }

        Peer.getInstance().getFileManager().updateChunkPerceivedRepDegree(fileId, chunkNo, peerId);
    }

    public int getChunkRepDegree(String fileId, int chunkNo) {
        if(backingUp) {
            backupRepDegree.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
            return backupRepDegree.get(fileId).get(chunkNo).size();
        } else {
            if(chunksRepDegree.containsKey(fileId)) {
                if(chunksRepDegree.get(fileId).containsKey(chunkNo)) {
                    return chunksRepDegree.get(fileId).get(chunkNo).size();
                }
            }
        }

        return 0;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer>>> getBackupRepDegree() {
        return backupRepDegree;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer>>> getChunksRepDegree() {
        return chunksRepDegree;
    }
}
