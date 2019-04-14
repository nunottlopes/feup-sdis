package message;

import peer.Peer;

import java.io.*;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;

public class Message {

    public static byte CR = 0xD;
    public static byte LF = 0xA;
    public static String CRLF = "" + (char) CR + (char) LF;

    public enum MessageType {
        PUTCHUNK, STORED, GETCHUNK, CHUNK, DELETE, REMOVED, GETCHUNKENH
    }

    private MessageType type;
    private String version;
    private int senderId;
    private String fileId;
    private int chunkNo;
    private int replicationDeg;
    private int header_length;

    //getchunkenh
    private int port;

    private byte[] body;

    public Message(DatagramPacket packet) throws InvalidPacketException {
        if(!parseHeader(packet.getData())) {
            throw new InvalidPacketException("Invalid Message Header");
        }

        if (type == MessageType.PUTCHUNK || (type == MessageType.CHUNK && !Peer.getInstance().isEnhanced())) {
            if(!parseBody(packet.getData(), packet.getLength())) {
                throw new InvalidPacketException("Invalid Message Body");
            }
        }
    }

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

    public Message(MessageType type, String[] args, byte[] body) {
        this(type, args);
        this.body = body;
    }

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

    private boolean parseBody(byte[] data, int data_length) {
        int from = header_length + 2 * CRLF.length();
        int to   = data_length;

        if(from > to) return false;

        this.body = Arrays.copyOfRange(data, from, to);
        return true;
    }

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

    public MessageType getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getFileId() {
        return fileId;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public int getPort() {
        return port;
    }

    public int getReplicationDeg() {
        return replicationDeg;
    }

    public byte[] getBody() {
        return body;
    }

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
