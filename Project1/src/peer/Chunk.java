package peer;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Chunk implements Serializable {

    public final static int MAX_SIZE = 64*1000;

    private String fileId;
    private int chunkNo;
    private int repDegree;
    private byte[] data;
    private Set<Integer> perceivedRepDegree;
    private long size;

    public Chunk(String fileId, int chunkNo, int repDegree, byte[] data) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.repDegree = repDegree;
        this.data = data;
        this.size = data.length;
        this.perceivedRepDegree = new HashSet<>();
        this.perceivedRepDegree.add(Peer.getInstance().getId());
    }

    public Chunk(String fileId, int chunkNo, int repDegree, Set<Integer> perceivedRepDegree) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.repDegree = repDegree;
        this.perceivedRepDegree = perceivedRepDegree;
    }

    public int getPerceivedRepDegree() {
        return perceivedRepDegree.size();
    }

    public String getFileId() {
        return fileId;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public int getRepDegree() {
        return repDegree;
    }

    public byte[] getData() {
        return data;
    }

    public long getSize(){
        return size;
    }

    public void clearData(){
        data = null;
    }

    public void addPerceivedRepDegreePeerId(int peerId){
        this.perceivedRepDegree.add(peerId);
    }

    public void setPerceivedRepDegree(Set<Integer> perceivedRepDegree) {
        perceivedRepDegree.add(Peer.getInstance().getId());
        this.perceivedRepDegree = perceivedRepDegree;
    }

    public void removePerceivedRepDegreePeer(int peerId){
        perceivedRepDegree.remove(peerId);
    }
}

