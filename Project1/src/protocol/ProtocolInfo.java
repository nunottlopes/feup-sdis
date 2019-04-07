package protocol;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ProtocolInfo {

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, AtomicLong> > chunksRepDegree;

    public ProtocolInfo() {
        chunksRepDegree = new ConcurrentHashMap<>();
    }

    public void startBackup(String fileId) {chunksRepDegree.putIfAbsent(fileId, new ConcurrentHashMap<>());}

    public void endBackup(String fileId) {chunksRepDegree.remove(fileId);}

    public void incRepDegree(String fileId, int chunkNo) {

        if(!chunksRepDegree.containsKey(fileId)) return;

        chunksRepDegree.get(fileId).putIfAbsent(chunkNo, new AtomicLong(0));
        chunksRepDegree.get(fileId).get(chunkNo).incrementAndGet();
    }

    public int getChunkRepDegree(String fileId, int chunkNo) {
        if(chunksRepDegree.containsKey(fileId)) {
            if(chunksRepDegree.get(fileId).containsKey(chunkNo)) {
                return chunksRepDegree.get(fileId).get(chunkNo).intValue();
            }
        }
        return 0;
    }
}
