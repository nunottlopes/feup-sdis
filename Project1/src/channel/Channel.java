package channel;

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

    public Channel(String address, int port, Type type) {
        this(address, port, type, 64000);
    }

    public Channel(String address, int port, Type type, int CHUNCK_SIZE) {

        this.MAX_BUF_SIZE = CHUNCK_SIZE + CHANNEL_OFFSET;
        this.type = type;

        try {
            this.address = InetAddress.getByName(address);
            this.port = port;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        start();
    }

    private void start() {
        try {
            this.socket = new MulticastSocket(this.port);
            this.socket.setTimeToLive(1);
            this.socket.joinGroup(this.address);
            System.out.println("--- Started " + this.type + " Channel ---");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        this.socket.close();
        System.out.println("--- Closed " + this.type + " Channel ---");
    }


    public void sendMessage(String message){

        try {
            this.socket.send(new DatagramPacket(message.getBytes(), message.getBytes().length,this.address, this.port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] received_data = new byte[MAX_BUF_SIZE];

        DatagramPacket packet = new DatagramPacket(received_data, received_data.length);

        while(true) {
            try {
                this.socket.receive(packet);
                // TODO: HANDLE MSG
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


}