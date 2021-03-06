package channel;

import java.io.IOException;
import java.io.ObjectInputStream;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import message.InvalidPacketException;
import message.Message;
import message.MessageHandler;
import peer.Peer;

/**
 * Channel class
 */
public class Channel implements Runnable{

    /**
     * Channel port
     */
    private int port;

    /**
     * Channel socket
     */
    private SSLServerSocket socket;

    /**
     * Channel constructor
     * @param port
     * @throws IOException
     */
    public Channel(int port) throws IOException {
        this.port = port;
        start();
    }

    /**
     * Start channel activity
     * @throws IOException
     */
    private void start() throws IOException {
        SSLServerSocketFactory ssf;

        ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        socket = (SSLServerSocket) ssf.createServerSocket(this.port);

        System.out.println("--- Started SSL Channel on port "+ this.port + " ---");
    }

    /**
     * Function that waits for messages from the socket
     */
    @Override
    public void run() {
        while(true) {
            try {
                SSLSocket connection = (SSLSocket) this.socket.accept();
				ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
				Message message = (Message) ois.readObject();
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
     * Return Channel port
     * @return port
     */
    public int getPort() {
        return port;
    }
}