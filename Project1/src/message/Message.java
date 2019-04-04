package message;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.util.Arrays;

public class Message {

    public static byte CR = 0xD;
    public static byte LF = 0xA;
    public static String CRLF = "" + (char) CR + (char) LF;

    public enum MessageType {
        PUTCHUNK, STORED, GETCHUNK, CHUNK, DELETE, REMOVED
    }

    private MessageType type;
    private String version;
    private int senderId;
    private int fileId;
    private int chunckNo;
    private int replicationDeg;
    private int header_length;

    private byte[] body;

    public Message(DatagramPacket packet) throws InvalidPacketException {
        if(!parseHeader(packet.getData())) {
            throw new InvalidPacketException("Invalid Message Header");
        }

        if (type == MessageType.PUTCHUNK || type == MessageType.CHUNK) {
            if(!parseBody(packet.getData(), packet.getData().length)) {
                throw new InvalidPacketException("Invalid Message Body");
            }
        }
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
        System.out.println("Received: PUTCHUNK " + version + " " + senderId + " " + fileId + " " + chunckNo + " " + replicationDeg);

        try {
            version = fields[1];
            senderId = Integer.parseInt(fields[2]);
            fileId = Integer.parseInt(fields[3]);
            chunckNo = Integer.parseInt(fields[4]);
            replicationDeg = Integer.parseInt(fields[5]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    private boolean parseSTORED(String[] fields) {
        if(fields.length != 5) return false;
        System.out.println("Received: STORED " + version + " " + senderId + " " + fileId + " " + chunckNo);

        try {
            version = fields[1];
            senderId = Integer.parseInt(fields[2]);
            fileId = Integer.parseInt(fields[3]);
            chunckNo = Integer.parseInt(fields[4]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    private boolean parseGETCHUNK(String[] fields) {
        if(fields.length != 5) return false;
        System.out.println("Received: GETCHUNK " + version + " " + senderId + " " + fileId + " " + chunckNo);

        try {
            version = fields[1];
            senderId = Integer.parseInt(fields[2]);
            fileId = Integer.parseInt(fields[3]);
            chunckNo = Integer.parseInt(fields[4]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    private boolean parseCHUNK(String[] fields) {
        if(fields.length != 5) return false;
        System.out.println("Received: CHUNK " + version + " " + senderId + " " + fileId + " " + chunckNo);

        try {
            version = fields[1];
            senderId = Integer.parseInt(fields[2]);
            fileId = Integer.parseInt(fields[3]);
            chunckNo = Integer.parseInt(fields[4]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    private boolean parseDELETE(String[] fields) {
        if(fields.length != 4) return false;
        System.out.println("Received: DELETE " + version + " " + senderId + " " + fileId);

        version = fields[1];
        try {
            senderId = Integer.parseInt(fields[2]);
            fileId = Integer.parseInt(fields[3]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    private boolean parseREMOVED(String[] fields) {
        if(fields.length != 5) return false;
        System.out.println("Received: REMOVED " + version + " " + senderId + " " + fileId + " " + chunckNo);

        try {
            version = fields[1];
            senderId = Integer.parseInt(fields[2]);
            fileId = Integer.parseInt(fields[3]);
            chunckNo = Integer.parseInt(fields[4]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
