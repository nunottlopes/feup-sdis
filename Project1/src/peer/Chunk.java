package peer;

public class Chunk {

    public final static int MAX_SIZE = 64*1000;

    private String fileId;
    private int chunkNo;
    private int repDegree;
    private byte[] data;

    public Chunk(String fileId, int chunkNo, int repDegree, byte[] data) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.repDegree = repDegree;
        this.data = data;
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
}
