package channel;

import message.InvalidPacketException;
import message.MessageHandler;
import peer.Chunk;
import peer.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

/**
 * Channel class
 */
public class Channel implements Runnable{

    /**
     * Channel types
     */
    public enum Type {
        MC, MDB, MDR
    }

    /**
     * Channel offset
     */
    private static final int CHANNEL_OFFSET = 265;

    /**
     * Channel max buf size to read from socket
     */
    private int MAX_BUF_SIZE;

    /**
     * Channel type
     */
    private Type type;

    /**
     * Channel Inet Address
     */
    private InetAddress address;

    /**
     * Channel port
     */
    private int port;

    /**
     * Channel socket
     */
    private MulticastSocket socket;

    /**
     * Channel constructor
     * @param address
     * @param port
     * @param type
     * @throws IOException
     */
    public Channel(String address, int port, Type type) throws IOException {
        this(address, port, type, Chunk.MAX_SIZE);
    }

    /**
     * Channel constructor
     * @param address
     * @param port
     * @param type
     * @param CHUNK_SIZE
     * @throws IOException
     */
    public Channel(String address, int port, Type type, int CHUNK_SIZE) throws IOException {

        this.MAX_BUF_SIZE = CHUNK_SIZE + CHANNEL_OFFSET;
        this.type = type;

        try {
            this.address = InetAddress.getByName(address);
            this.port = port;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        start();
    }

    /**
     * Start channel activity
     * @throws IOException
     */
    private void start() throws IOException {
        this.socket = new MulticastSocket(this.port);
        this.socket.setTimeToLive(1);
        this.socket.joinGroup(this.address);
        System.out.println("--- Started " + this.type + " Channel ---");
    }

    /**
     * Function that waits for messages from the socket
     */
    @Override
    public void run() {
        byte[] received_data = new byte[MAX_BUF_SIZE];

        DatagramPacket packet = new DatagramPacket(received_data, received_data.length);

        while(true) {
            try {
                this.socket.receive(packet);
                MessageHandler handler = new MessageHandler(packet);
                Peer.getInstance().getPool().execute(handler);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidPacketException e) {
                System.out.println("Invalid Packet Received: " + e);
            }
        }

    }

    /**
     * Returns Channel Inet Address
     * @return Inet Address
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Return Channel port
     * @return port
     */
    public int getPort() {
        return port;
    }
}