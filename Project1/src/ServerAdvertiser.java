import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ServerAdvertiser implements Runnable {
    private InetAddress multicastAddr;
    private Integer multicastPORT;
    private String msg;

    public ServerAdvertiser(InetAddress multicastAddr, Integer multicastPort, String msg) {
        this.multicastAddr = multicastAddr;
        this.multicastPORT = multicastPort;
        this.msg = msg;
    }

    @Override
    public void run() {
        MulticastSocket multicastSocket = null;
        try {
            multicastSocket = new MulticastSocket(this.multicastPORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DatagramPacket advertisement;
        advertisement = new DatagramPacket(this.msg.getBytes(), this.msg.getBytes().length, this.multicastAddr, this.multicastPORT);

        try {
            multicastSocket.send(advertisement);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}