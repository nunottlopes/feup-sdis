package peer;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Chunk class
 */
public class Chunk implements Serializable {
    static final long serialVersionUID = 41L;

    /**
     * Chunk max data size
     */
    public final static int MAX_SIZE = 200*1000;

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
    }

    /**
     * Sets chunk data
     * @param data
     */
    public void setData(byte[] data) {
        this.data = data;
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
}

