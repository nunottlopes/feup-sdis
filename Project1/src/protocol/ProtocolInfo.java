package protocol;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolInfo {

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer>> > chunksRepDegree;
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

    public void endBackup(String fileId) {
        backingUp = false;
        backupRepDegree.remove(fileId);
    }

    public void incRepDegree(String fileId, int chunkNo, int peerId) {
        if(backingUp) {
            backupRepDegree.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
            backupRepDegree.get(fileId).get(chunkNo).add(peerId);
        }

        else {
            chunksRepDegree.get(fileId).putIfAbsent(chunkNo, new HashSet<>());
            chunksRepDegree.get(fileId).get(chunkNo).add(peerId);
        }
    }

    public int getChunkRepDegree(String fileId, int chunkNo) {
        if(backingUp) {
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
}
