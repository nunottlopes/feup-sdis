package message;

public class Message {
    public enum MessageType {
        PUTCHUNK
    }

    private MessageType type;
    private String version;
    private int senderId;
    private int fileId;
    private int chunckNo;
    private int replicationDeg;

    public Message(){

    }

    public String getVersion() {
        return version;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getFileId() {
        return fileId;
    }

    public int getChunckNo() {
        return chunckNo;
    }

    public int getReplicationDeg() {
        return replicationDeg;
    }

    public MessageType getType() {
        return type;
    }
}
