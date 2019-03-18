import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Channel{

    private InetAddress address;
    private int port;
    private MulticastSocket socket;

    public Channel(String address, int port) throws IOException {
        this.address = InetAddress.getByName(address);
        this.port = port;

        this.socket = new MulticastSocket(this.port);
    }

    public void sendMessage(String message){
        try {
            this.socket.send(new DatagramPacket(message.getBytes(), message.getBytes().length, this.address, this.port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}