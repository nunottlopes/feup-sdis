package protocol.restore;

import message.Message;
import peer.Peer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCPServer class
 */
public class TCPServer implements Runnable {

    private ServerSocket ssocket;
    private boolean run;
    private int port = 0;

    /**
     * TCPServer constructor
     */
    public TCPServer() {
        open();
    }

    /**
     * Runs TCP server
     */
    @Override
    public void run() {
        while (run) {
            try {
                Socket client = ssocket.accept();
                handleMessage(client);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    /**
     * Handles message received in TCP server
     * @param socket
     */
    private void handleMessage(Socket socket) {
        Message msg;

        ObjectInputStream in;
        try {
            in = new ObjectInputStream(socket.getInputStream());
            msg = (Message) in.readObject();
            in.close();
            socket.close();
            Peer.getInstance().getProtocolInfo().chunkSent(msg.getFileId(), msg.getChunkNo(), msg.getBody());
        } catch (IOException e) {
            System.out.println("Error retrieving TCP message");
        } catch (ClassNotFoundException e) { }

    }

    /**
     * Opens TCP server
     */
    public void open() {
        try {
            ssocket = new ServerSocket(0);
            port = ssocket.getLocalPort();
            run = true;
        } catch (IOException e) {
            System.out.println("Error initializing TCPServer!");
        }

        System.out.println("--- Started TCP Server ---");
    }

    /**
     * Closes TCP server
     */
    public void close() {
        try {
            run = false;
            ssocket.close();
        } catch (IOException e) {
            System.out.println("Error closing TCPServer!");
        }
        System.out.println("--- Closed TCP Server ---");
    }

    /**
     * Return TCP server port
     * @return port
     */
    public int getPort() {
        return port;
    }
}
