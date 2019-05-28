package message;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Message class
 */
public class Message implements Serializable {

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
        PUTCHUNK, STORED, GETCHUNK, CHUNK, DELETE
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

//    /**
//     * Parses message header
//     * @return true if valid message header, false otherwise
//     */
//    private boolean parseHeader(String message) {
//        ByteArrayInputStream stream = new ByteArrayInputStream(message.getBytes());
//        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
//
//        String header = "";
//        try {
//            header = reader.readLine();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        if(header.equals("")) return false;
//
//        this.header_length = header.getBytes().length;
//
//        String[] fields = header.trim().split(" +");
//
//
//        switch (fields[0]) {
//            case "PUTCHUNK":
//                this.type = MessageType.PUTCHUNK;
//                return parsePUTCHUNK(fields);
//            case "STORED":
//                this.type = MessageType.STORED;
//                return parseSTORED(fields);
//            case "GETCHUNK":
//                this.type = MessageType.GETCHUNK;
//                return parseGETCHUNK(fields);
//            case "CHUNK":
//                this.type = MessageType.CHUNK;
//                return parseCHUNK(fields);
//            case "DELETE":
//                this.type = MessageType.DELETE;
//                return parseDELETE(fields);
//            default:
//                return false;
//
//        }
//    }

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

//    /**
//     * Returns header string
//     * @return header string
//     */
//    public String getHeaderString() {
//        String str;
//
//        switch (type) {
//            case PUTCHUNK:
//                str = type + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + replicationDeg + " " + CRLF + CRLF;
//                break;
//            case STORED:
//                str = type + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + CRLF + CRLF;
//                break;
//            case GETCHUNK:
//                str = type + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + CRLF + CRLF;
//                break;
//            case CHUNK:
//                str = type + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + CRLF + CRLF;
//                break;
//            case DELETE:
//                str = type + " " + version + " " + senderId + " " + fileId + " " + CRLF + CRLF;
//                break;
//            default:
//                str = "";
//                break;
//
//        }
//
//        return str;
//    }

//    /**
//     * Returns message bytes
//     * @return bytes
//     */
//    public byte[] getBytes() {
//        byte[] header = getHeaderString().getBytes();
//
//        if(body != null) {
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            try {
//                out.write(header);
//                out.write(body);
//            } catch (IOException e) {
//                System.out.println("Couldn't get Message bytes");
//            }
//
//            return out.toByteArray();
//        }
//
//        return header;
//    }
}
