package message;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


class MessageTest {

    public byte[] generateRandomBody(int size) {
        byte[] b = new byte[size];
        new Random().nextBytes(b);
        return b;
    }

    @Test
    public void parsePUTCHUNK() {
        String data = "PUTCHUNK 1.0 1 2 3 4 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        data = "   PUTCHUNK 1.0 1      2    3 4   " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        assertThrows(InvalidPacketException.class, () -> {
            String m = "PUTCHUNK 1.0 1 2 3 4";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "PUTCHUNK 1.0 1 2 3 4 6 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "PUTCHUNK 1.0 a 2 3 4" + Message.CRLF + Message.CRLF + generateRandomBody(20);
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "PUTCHUNK 1.0 1 2 3 a";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseSTORED() {
        String data = "STORED 1.0 2 3 4";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        data = "   STORED   1.0 1      2    3   ";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        data = "STORED 1.0 2 3 4 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        assertThrows(InvalidPacketException.class, () -> {
            String m = "STORED 1.0 1 2 3 4 6";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "STORED 1.0 1 2";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "STORED 1.0 a b c";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "STORED 1.0 1 2 aa";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseGETCHUNK() {
        String data = "GETCHUNK 1.0 1 2 3";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        data = "   GETCHUNK 1.0 1      2    3   ";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        data = "GETCHUNK 1.0 1 2 3 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        assertThrows(InvalidPacketException.class, () -> {
            String m = "GETCHUNK 1.0 1 2 3 4";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "GETCHUNK 1.0 1 2";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "GETCHUNK 1.0 aa 2 bb ";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "GETCHUNK 1.0 bb v 3";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseCHUNK() {
        String data = "CHUNK 1.0 1 2 3 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        data = "   CHUNK 1.0 1      2    3   " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        assertThrows(InvalidPacketException.class, () -> {
            String m = "CHUNK 1.0 1 2 3";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "CHUNK 1.0 1 2 3 4 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "CHUNK 1.0 1 2";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "CHUNK 1.0 aa 2 3 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "CHUNK 1.0 1 2 v";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseDELETE() {
        String data = "DELETE 1.0 1 2";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        data = "   DELETE 1.0 1      2     ";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        data = "DELETE 1.0 1 2" + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        assertThrows(InvalidPacketException.class, () -> {
            String m = "DELETE 1.0 1 2 3";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "DELETE 1.0 1";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "DELETE 1.0 a a";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "DELETE 1.0 a 1";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseREMOVED() {
        String data = "REMOVED 1.0 1 2 3";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        data = "   REMOVED 1.0 1      2    3   ";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        data = "REMOVED 1.0 1 2 3 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (InvalidPacketException e) {
            fail(e);
        }

        assertThrows(InvalidPacketException.class, () -> {
            String m = "REMOVED 1.0 1 2 3 4";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "REMOVED 1.0 1 2";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "REMOVED 1.0 1 a b" + Message.CRLF + Message.CRLF + generateRandomBody(20);
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "REMOVED 1.0 a 2";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseUnhandledMessage() {
        assertThrows(InvalidPacketException.class, () -> {
            String m = "   BBBB 1.0 1      2    3   ";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "AAAA 1.0 1 2 3";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "CHUNKA 1.0 1 2 3 4 " + Message.CRLF + Message.CRLF + "cljadjasjaslcjlsacjlascnacln";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(InvalidPacketException.class, () -> {
            String m = "   GETDELETE 1.0 1     2 3    4 " + Message.CRLF + Message.CRLF + "cljadjasjaslcjlsacjlascnacln";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });
    }

    @Test
    public void makeSTORED() {
        String[] args = {"1.0", "1", "file1", "1"};
        Message msg = new Message(Message.MessageType.STORED, args);

        assertEquals(Message.MessageType.STORED, msg.getType());
        assertEquals("1.0", msg.getVersion());
        assertEquals(1, msg.getSenderId());
        assertEquals("file1", msg.getFileId());
        assertEquals(1, msg.getChunkNo());
        assertEquals(0, msg.getReplicationDeg());
        assertNull(msg.getBody());

    }

    @Test
    public void makePUTCHUNK() {
        String[] args = {"1.0", "1", "file1", "1", "3"};
        Message msg = new Message(Message.MessageType.PUTCHUNK, args, generateRandomBody(300));

        assertEquals(Message.MessageType.PUTCHUNK, msg.getType());
        assertEquals("1.0", msg.getVersion());
        assertEquals(1, msg.getSenderId());
        assertEquals("file1", msg.getFileId());
        assertEquals(1, msg.getChunkNo());
        assertEquals(3, msg.getReplicationDeg());
        assertNotNull(msg.getBody());

    }
}
