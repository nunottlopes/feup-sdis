package message;

import peer.Peer;

import java.io.*;
import java.net.DatagramPacket;
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

    /**
     * CRLF - Header terminator
     */
    public static String CRLF = "" + (char) CR + (char) LF;

    /**
     * Messages Types
     */
    public enum MessageType {
        PUTCHUNK, STORED, GETCHUNK, CHUNK, DELETE, REMOVED, GETCHUNKENH
    }

    /**
     * Message type
     */
    private MessageType type;

    /**
     * Protocol version
     */
    private String version;

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

    /**
     * Message header length
     */
    private int header_length;

    /**
     * TCP port
     */
    private int port;

    /**
     * Message body
     */
    private byte[] body;

    /**
     * Message constructor
     * @param packet
     * @throws InvalidPacketException
     */
    public Message(DatagramPacket packet) throws InvalidPacketException {
        if(!parseHeader(packet.getData())) {
            throw new InvalidPacketException("Invalid Message Header");
        }

        if (type == MessageType.PUTCHUNK || (type == MessageType.CHUNK && !Peer.getInstance().isEnhanced())) {
            if(!parseBody(packet.getData(), packet.getLength())) {
                throw new InvalidPacketException("Invalid Message Body (" + type + ")");
            }
        }
    }

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
            case REMOVED:
                parseREMOVED(args);
            case GETCHUNKENH:
                parseGETCHUNKENH(args);
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
     * Parses message header
     * @param data
     * @return true if valid message header, false otherwise
     */
    private boolean parseHeader(byte[] data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String header = "";
        try {
            header = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(header.equals("")) return false;

        this.header_length = header.getBytes().length;

        String[] fields = header.trim().split(" +");


        switch (fields[0]) {
            case "PUTCHUNK":
                this.type = MessageType.PUTCHUNK;
                return parsePUTCHUNK(fields);
            case "STORED":
                this.type = MessageType.STORED;
                return parseSTORED(fields);
            case "GETCHUNK":
                this.type = MessageType.GETCHUNK;
                return parseGETCHUNK(fields);
            case "CHUNK":
                this.type = MessageType.CHUNK;
                return parseCHUNK(fields);
            case "DELETE":
                this.type = MessageType.DELETE;
                return parseDELETE(fields);
            case "REMOVED":
                this.type = MessageType.REMOVED;
                return parseREMOVED(fields);
            case "GETCHUNKENH":
                this.type = MessageType.GETCHUNKENH;
                return parseGETCHUNKENH(fields);
            default:
                return false;

        }
    }

    /**
     * Parses message body
     * @param data
     * @param data_length
     * @return true if valid body, false otherwise
     */
    private boolean parseBody(byte[] data, int data_length) {
        int from = header_length + 2 * CRLF.length();
        int to   = data_length;

        if(from > to) return false;

        this.body = Arrays.copyOfRange(data, from, to);
        return true;
    }

    /**
     * Parses PUTCHUNK messages
     * @param fields
     * @return true if valid message, false otherwise
     */
    private boolean parsePUTCHUNK(String[] fields) {
        if(fields.length != 6) return false;

        try {
            version = fields[1];
            senderId = Integer.parseInt(fields[2]);
            fileId = fields[3];
            chunkNo = Integer.parseInt(fields[4]);
            replicationDeg = Integer.parseInt(fields[5]);
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
        if(fields.length != 5) return false;

        try {
            version = fields[1];
            senderId = Integer.parseInt(fields[2]);
            fileId = fields[3];
            chunkNo = Integer.parseInt(fields[4]);
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
        if(fields.length != 5) return false;

        try {
            version = fields[1];
            senderId = Integer.parseInt(fields[2]);
            fileId = fields[3];
            chunkNo = Integer.parseInt(fields[4]);
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
        if(fields.length != 5) return false;

        try {
            version = fields[1];
            senderId = Integer.parseInt(fields[2]);
            fileId = fields[3];
            chunkNo = Integer.parseInt(fields[4]);
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
        if(fields.length != 4) return false;

        version = fields[1];
        try {
            senderId = Integer.parseInt(fields[2]);
            fileId = fields[3];
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
        if(fields.length != 5) return false;

        try {
            version = fields[1];
            senderId = Integer.parseInt(fields[2]);
            fileId = fields[3];
            chunkNo = Integer.parseInt(fields[4]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * Parses GETCHUNKENH messages
     * @param fields
     * @return true if valid message, false otherwise
     */
    private boolean parseGETCHUNKENH(String[] fields) {
        if(fields.length != 6) return false;

        try {
            version = fields[1];
            senderId = Integer.parseInt(fields[2]);
            fileId = fields[3];
            chunkNo = Integer.parseInt(fields[4]);
            port = Integer.parseInt(fields[5]);
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
     * Return message version
     * @return version
     */
    public String getVersion() {
        return version;
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
     * Return message port
     * @return port
     */
    public int getPort() {
        return port;
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

    /**
     * Returns header string
     * @return header string
     */
    public String getHeaderString() {
        String str;

        switch (type) {
            case PUTCHUNK:
                str = type + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + replicationDeg + " " + CRLF + CRLF;
                break;
            case STORED:
                str = type + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + CRLF + CRLF;
                break;
            case GETCHUNK:
                str = type + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + CRLF + CRLF;
                break;
            case CHUNK:
                str = type + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + CRLF + CRLF;
                break;
            case DELETE:
                str = type + " " + version + " " + senderId + " " + fileId + " " + CRLF + CRLF;
                break;
            case REMOVED:
                str = type + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + CRLF + CRLF;
                break;
            case GETCHUNKENH:
                str = type + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + port + " " + CRLF + CRLF;
                break;
            default:
                str = "";
                break;

        }

        return str;
    }

    /**
     * Returns message bytes
     * @return bytes
     */
    public byte[] getBytes() {
        byte[] header = getHeaderString().getBytes();

        if(body != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                out.write(header);
                out.write(body);
            } catch (IOException e) {
                System.out.println("Couldn't get Message bytes");
            }

            return out.toByteArray();
        }

        return header;
    }
}
