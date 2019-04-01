import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Client {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int port;

    private static String multicastIP = "233.0.113.0";
    private static Integer multicastPORT = 4445;

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

    private static String[] listenMulticast() throws IOException {
        InetAddress addr = InetAddress.getByName(multicastIP);
        byte[] buf = new byte[256];

        MulticastSocket clientSocket = new MulticastSocket(multicastPORT);
        clientSocket.joinGroup(addr);

        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            clientSocket.receive(packet);

            String req = new String(packet.getData()).trim();
            return req.split(" ");
        }
    }

    public static void main(String[] args) throws Exception {
        String[] serverData = listenMulticast();
        Pattern pattern = Pattern.compile("/(.*)");
        Matcher matcher = pattern.matcher(serverData[0]);
        String ip;

        if(matcher.find()) ip = matcher.group(1);
        else return;

        Client client = new Client(ip, Integer.parseInt(serverData[1]));

        switch (args[0]) {
            case "register":
                client.register(args[1], args[2]);
                break;
            case "lookup":
                client.lookup(args[1]);
                break;
            default:
        }
    }
}
