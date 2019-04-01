import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Client {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int port;

    public Client(String destinationAddr, int port) throws Exception {
        this.serverAddress = InetAddress.getByName(destinationAddr);
        this.port = port;
        socket = new DatagramSocket();
    }

    private void register(String plate, String owner) throws Exception {
        String msg = "REGISTER " + plate + " " + owner;
        String response = this.send(msg);
        if(response.equals("-1")) response = "ERROR";
        System.out.println(msg + ": " + response);
    }

    private void lookup(String plate) throws Exception {
        String msg = "LOOKUP " + plate;
        String response = this.send(msg);
        if(response.equals("NOT_FOUND")) response = "ERROR";
        System.out.println(msg + ": " + response);
    }

    private String send(String msg) throws Exception {
        DatagramPacket packetSend = new DatagramPacket(msg.getBytes(), msg.getBytes().length, this.serverAddress, this.port);
        this.socket.send(packetSend);

        byte[] buf = new byte[256];
        DatagramPacket packetReceive = new DatagramPacket(buf, buf.length);
        this.socket.receive(packetReceive);
        String response = new String(packetReceive.getData()).trim();
        //System.out.println("Message from " + packet.getAddress().getHostAddress() + ": " + response);
        return response;
    }

    public static void main(String[] args) throws Exception {
        if(args.length < 4 || args.length > 5) {
            System.out.println("Usage: java Client <host_name> <port_number> <oper> <opnd>*");
            return;
        }

        Client client = new Client(args[0], Integer.parseInt(args[1]));

        switch (args[2]) {
            case "register":
                client.register(args[3], args[4]);
                break;
            case "lookup":
                client.lookup(args[3]);
                break;
            default:
        }
    }
}
