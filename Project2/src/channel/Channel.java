package channel;

import message.InvalidPacketException;
import message.MessageHandler;
import peer.Chunk;
import peer.Peer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
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
    private ServerSocket socket;

    /**
     * Channel constructor
     * @param address
     * @param port
     * @param type
     * @throws IOException
     */
    public Channel(String address, int port, Type type) throws IOException {
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
        this.socket = new ServerSocket(this.port);
        System.out.println("--- Started " + this.type + " Channel ---");
    }

    /**
     * Function that waits for messages from the socket
     */
    @Override
    public void run() {
        while(true) {
            try {
                Socket connection = this.socket.accept();
				ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
				String message = (String) ois.readObject();
                MessageHandler handler = new MessageHandler(message, connection.getInetAddress());
                Peer.getInstance().getPool().execute(handler);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidPacketException e) {
                System.out.println("Invalid Packet Received: " + e);
            } catch (ClassNotFoundException e) {
				e.printStackTrace();
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