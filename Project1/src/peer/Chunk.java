package peer;

public class Chunk {

    private final static int MAX_SIZE = 64*1000;

    private int fileId;
    private int chunkNo;
    private int repDegree;
    private byte[] data;

    public Chunk(int fileId, int chunkNo, int repDegree, byte[] data) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.repDegree = repDegree;
        this.data = data;
    }

    public int getFileId() {
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
}
