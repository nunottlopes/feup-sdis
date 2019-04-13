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


public class Channel implements Runnable{

    public enum Type {
        MC, MDB, MDR
    }

    private static final int CHANNEL_OFFSET = 265;

    private int MAX_BUF_SIZE;
    private Type type;

    private InetAddress address;
    private int port;
    private MulticastSocket socket;

    public Channel(String address, int port, Type type) throws IOException {
        this(address, port, type, Chunk.MAX_SIZE);
    }

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

    private void start() throws IOException {
        this.socket = new MulticastSocket(this.port);
        this.socket.setTimeToLive(1);
        this.socket.joinGroup(this.address);
        System.out.println("--- Started " + this.type + " Channel ---");
    }

    public void close() {
        this.socket.close();
        System.out.println("--- Closed " + this.type + " Channel ---");
    }


    @Override
    public void run() {
        byte[] received_data = new byte[MAX_BUF_SIZE];

        DatagramPacket packet = new DatagramPacket(received_data, received_data.length);

        while(true) {
            try {
                this.socket.receive(packet);
                Peer.getInstance().getPool().execute(new MessageHandler(packet));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidPacketException e) {
                System.out.println("Invalid Packet Received: " + e);
            }
        }

    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}