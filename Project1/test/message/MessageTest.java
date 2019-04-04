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
        } catch (Exception e) {
            fail(e);
        }

        data = "   PUTCHUNK 1.0 1      2    3 4   " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        assertThrows(Exception.class, () -> {
            String m = "PUTCHUNK 1.0 1 2 3 4 6 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "   PUTCHUNK 1.0 1     2 3    4 6 ";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "PUTCHUNK 1.0 a 2 3 4 6 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "PUTCHUNK 1.0 1 2 3 a b ";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseSTORED() {
        String data = "STORED 1.0 2 3 4";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        data = "   STORED   1.0 1      2    3   ";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        data = "STORED 1.0 2 3 4 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        assertThrows(Exception.class, () -> {
            String m = "STORED 1.0 1 2 3 4 6";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "   STORED 1.0 1     2 3    6";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "STORED 1.0 a b c 4 6";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "STORED 1.0 1 2 aa 6";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseGETCHUNK() {
        String data = "GETCHUNK 1.0 1 2 3";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        data = "   GETCHUNK 1.0 1      2    3   ";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        data = "GETCHUNK 1.0 1 2 3 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        assertThrows(Exception.class, () -> {
            String m = "GETCHUNK 1.0 1 2 3 4";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "   GETCHUNK 1.0 1     2 3    4 ";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "GETCHUNK 1.0 aa 2 bb 4";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "GETCHUNK 1.0 bb v 3 4 ";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseCHUNK() {
        String data = "CHUNK 1.0 1 2 3 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        data = "   CHUNK 1.0 1      2    3   " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        assertThrows(Exception.class, () -> {
            String m = "CHUNK 1.0 1 2 3 4 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "   CHUNK 1.0 1     2 3    4 6";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "CHUNK 1.0 aa 2 3 4 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "CHUNK 1.0 1 2 v 4 6";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseDELETE() {
        String data = "DELETE 1.0 1 2";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        data = "   DELETE 1.0 1      2     ";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        data = "DELETE 1.0 1 2" + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        assertThrows(Exception.class, () -> {
            String m = "DELETE 1.0 1 2 3";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "   DELETE 1.0 1     2 3    ";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "DELETE 1.0 a 2 3";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "DELETE 1.0 b 2 3";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseREMOVED() {
        String data = "REMOVED 1.0 1 2 3";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        data = "   REMOVED 1.0 1      2    3   ";
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        data = "REMOVED 1.0 1 2 3 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
        try {
            new Message(new DatagramPacket(data.getBytes(), data.getBytes().length));
        } catch (Exception e) {
            fail(e);
        }

        assertThrows(Exception.class, () -> {
            String m = "REMOVED 1.0 1 2 3 4";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "   REMOVED 1.0 1     2 3    4";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "REMOVED 1.0 1 a b 4 " + Message.CRLF + Message.CRLF + generateRandomBody(20);
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "REMOVED 1.0 a 2 3 b";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }

    @Test
    public void parseUnhandledMessage() {
        assertThrows(Exception.class, () -> {
            String m = "   BBBB 1.0 1      2    3   ";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "AAAA 1.0 1 2 3";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "CHUNKA 1.0 1 2 3 4 " + Message.CRLF + Message.CRLF + "cljadjasjaslcjlsacjlascnacln";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

        assertThrows(Exception.class, () -> {
            String m = "   GETDELETE 1.0 1     2 3    4 " + Message.CRLF + Message.CRLF + "cljadjasjaslcjlsacjlascnacln";
            new Message(new DatagramPacket(m.getBytes(), m.getBytes().length));
        });

    }
}
