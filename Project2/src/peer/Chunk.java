package peer;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Chunk class
 */
public class Chunk implements Serializable {

    /**
     * Chunk max data size
     */
    public final static int MAX_SIZE = 64*1000;

    /**
     * Chunk file id
     */
    private String fileId;

    /**
     * Chunk number
     */
    private int chunkNo;

    /**
     * Chunk desired replication degree
     */
    private int repDegree;

    /**
     * Chunk data
     */
    private byte[] data;

    /**
     * Set of peers id that backedup this chunk. The length of this set is the chunks perceived replication degree
     */
    private Set<Integer> perceivedRepDegree;

    /**
     * Chunk data size
     */
    private long size;

    /**
     * Chunk constructor
     * @param fileId
     * @param chunkNo
     * @param repDegree
     * @param data
     */
    public Chunk(String fileId, int chunkNo, int repDegree, byte[] data) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.repDegree = repDegree;
        this.data = data;
        this.size = data.length;
        this.perceivedRepDegree = new HashSet<>();
//        this.perceivedRepDegree.add(Peer.getInstance().getId());
    }

    /**
     * Chunk constructor
     * @param fileId
     * @param chunkNo
     * @param repDegree
     * @param perceivedRepDegree
     */
    public Chunk(String fileId, int chunkNo, int repDegree, Set<Integer> perceivedRepDegree) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.repDegree = repDegree;
        this.perceivedRepDegree = perceivedRepDegree;
    }

    /**
     * Returns chunk perceived replication degree
     * @return perceived replication degree
     */
    public int getPerceivedRepDegree() {
        return perceivedRepDegree.size();
    }

    /**
     * Return chunk file id
     * @return file id
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * Returns chunk number
     * @return number
     */
    public int getChunkNo() {
        return chunkNo;
    }

    /**
     * Returns chunk desired replication degree
     * @return desired replication degree
     */
    public int getRepDegree() {
        return repDegree;
    }

    /**
     * Returns chunk data
     * @return data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Return chunk data size
     * @return data size
     */
    public long getSize(){
        return size;
    }

    /**
     * Clears chunk data
     */
    public void clearData(){
        data = null;
    }

    /**
     * Sets chunk data
     * @param data
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Sets chunk perceived replication degree list
     * @param perceivedRepDegree
     */
    public void setPerceivedRepDegree(Set<Integer> perceivedRepDegree) {
        perceivedRepDegree.add(Peer.getInstance().getId());
        this.perceivedRepDegree = perceivedRepDegree;
    }

    /**
     * Removes peerId from perceived replication degree list
     * @param peerId
     */
    public void removePerceivedRepDegreePeer(int peerId){
        perceivedRepDegree.remove(peerId);
    }
}

