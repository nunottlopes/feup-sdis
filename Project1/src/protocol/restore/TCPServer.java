package protocol.restore;

import message.Message;
import peer.Peer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer implements Runnable {

    public static int TCP_PORT = 6565;

    private ServerSocket ssocket;
    private boolean run;

    public TCPServer() {
        open();
    }

    @Override
    public void run() {
        while (run) {
            try {
                Socket client = ssocket.accept();
                handleMessage(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessage(Socket socket) {
        Message msg = null;


        ObjectInputStream in;
        try {
            in = new ObjectInputStream(socket.getInputStream());
            msg = (Message) in.readObject();
            in.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Error retrieving TCP message");
        } catch (ClassNotFoundException e) { }


        Peer.getInstance().getProtocolInfo().chunkSent(msg.getFileId(), msg.getChunkNo(), msg.getBody());
    }

    public void open() {
        try {
            ssocket = new ServerSocket(TCP_PORT + Peer.getInstance().getId());
            run = true;
        } catch (IOException e) {
            System.out.println("Error initializing TCPServer!");
        }
    }

    public void close() {
        try {
            run = false;
            ssocket.close();
        } catch (IOException e) {
            System.out.println("Error closing TCPServer!");
        }
    }
}
