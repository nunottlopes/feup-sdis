package message;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Message class
 */
public class Message implements Serializable {

    static final long serialVersionUID = 40L;

    /**
     * CR
     */
    public static byte CR = 0xD;

    /**
     * LF
     */
    public static byte LF = 0xA;

//    /**
//     * CRLF - Header terminator
//     */
//    public static String CRLF = "" + (char) CR + (char) LF;

    /**
     * Messages Types
     */
    public enum MessageType {
        PUTCHUNK, STORED, GETCHUNK, CHUNK, DELETE, REMOVED
    }

    /**
     * Message type
     */
    private MessageType type;

    /**
     * Sender id
     */
    private int senderId;

    /**
     * File id
     */
    private String fileId;

    /**
     * Chunk number
     */
    private int chunkNo;

    /**
     * Desired replication degree
     */
    private int replicationDeg;

//    /**
//     * Message header length
//     */
//    private int header_length;

    /**
     * Message body
     */
    private byte[] body;

    /**
     * Message constructor
     * @param type
     * @param args
     */
    public Message(MessageType type, String[] args) {
        this.type = type;
        ArrayList<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(args));
        list.add(0, this.type.toString());
        args = list.toArray(new String[list.size()]);

        switch (type) {
            case PUTCHUNK:
                parsePUTCHUNK(args);
            case STORED:
                parseSTORED(args);
            case GETCHUNK:
                parseGETCHUNK(args);
            case CHUNK:
                parseCHUNK(args);
            case REMOVED:
                parseREMOVED(args);
            case DELETE:
                parseDELETE(args);
            default:
        }
    }

    /**
     * Message constructor
     * @param type
     * @param args
     * @param body
     */
    public Message(MessageType type, String[] args, byte[] body) {
        this(type, args);
        this.body = body;
    }


    /**
     * Parses PUTCHUNK messages
     * @param fields
     * @return true if valid message, false otherwise
     */
    private boolean parsePUTCHUNK(String[] fields) {
        if(fields.length != 5) return false;

        try {
            senderId = Integer.parseInt(fields[1]);
            fileId = fields[2];
            chunkNo = Integer.parseInt(fields[3]);
            replicationDeg = Integer.parseInt(fields[4]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * Parses STORED messages
     * @param fields
     * @return true if valid message, false otherwise
     */
    private boolean parseSTORED(String[] fields) {
        if(fields.length != 4) return false;

        try {
            senderId = Integer.parseInt(fields[1]);
            fileId = fields[2];
            chunkNo = Integer.parseInt(fields[3]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * Parses GETCHUNK messages
     * @param fields
     * @return true if valid message, false otherwise
     */
    private boolean parseGETCHUNK(String[] fields) {
        if(fields.length != 4) return false;

        try {
            senderId = Integer.parseInt(fields[1]);
            fileId = fields[2];
            chunkNo = Integer.parseInt(fields[3]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * Parses CHUNK messages
     * @param fields
     * @return true if valid message, false otherwise
     */
    private boolean parseCHUNK(String[] fields) {
        if(fields.length != 4) return false;

        try {
            senderId = Integer.parseInt(fields[1]);
            fileId = fields[2];
            chunkNo = Integer.parseInt(fields[3]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * Parses REMOVED messages
     * @param fields
     * @return true if valid message, false otherwise
     */
    private boolean parseREMOVED(String[] fields) {
        if(fields.length != 4) return false;

        try {
            senderId = Integer.parseInt(fields[1]);
            fileId = fields[2];
            chunkNo = Integer.parseInt(fields[3]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * Parses DELETE messages
     * @param fields
     * @return true if valid message, false otherwise
     */
    private boolean parseDELETE(String[] fields) {
        if(fields.length != 3) return false;

        try {
            senderId = Integer.parseInt(fields[1]);
            fileId = fields[2];
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * Return message type
     * @return type
     */
    public MessageType getType() {
        return type;
    }


    /**
     * Return message sender id
     * @return sender id
     */
    public int getSenderId() {
        return senderId;
    }

    /**
     * Return message file id
     * @return file id
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * Return message chunk number
     * @return chunk number
     */
    public int getChunkNo() {
        return chunkNo;
    }

    /**
     * Return message replication degree
     * @return replication degree
     */
    public int getReplicationDeg() {
        return replicationDeg;
    }

    /**
     * Return message body
     * @return body
     */
    public byte[] getBody() {
        return body;
    }

}
