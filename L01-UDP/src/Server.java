import java.net.*;
import java.util.Hashtable;

public class Server {
    private DatagramSocket socket;
    private int port;
    private Hashtable<String, String> data = new Hashtable<>();

    public Server(int port) throws SocketException {
        this.port = port;
        this.socket = new DatagramSocket(this.port);
    }

    private void listen() throws Exception {
        System.out.println("-- Running Server at " + InetAddress.getLocalHost() + " --");
        String req;

        while(true) {
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            this.socket.receive(packet);
            req = new String(packet.getData()).trim();
            System.out.println("Message from " + packet.getAddress().getHostAddress() + ": " + req);

            String response = this.handleRequest(req);
            DatagramPacket packetSend = new DatagramPacket(
                    response.getBytes(), response.getBytes().length,
                    packet.getAddress(), packet.getPort());
            this.socket.send(packetSend);
        }
    }

    private String handleRequest(String req) {
        String[] args = req.split(" ");
        switch (args[0]) {
            case "REGISTER":
                return register(args[1], args[2]);
            case "LOOKUP":
                return lookup(args[1]);
        }
        return "ERROR";
    }

    private String register(String plate, String owner) {
        if(this.data.containsKey(plate)) return "-1";
        this.data.put(plate, owner);
        return Integer.toString(this.data.size());
    }

    private String lookup(String plate) {
        if(!this.data.containsKey(plate)) return "NOT_FOUND";
        return plate + " " + this.data.get(plate);
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(Integer.parseInt(args[0]));
        server.listen();
    }
}
