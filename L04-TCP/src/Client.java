import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static PrintWriter out;
    private static BufferedReader in;

    public static void register(String plate, String owner) throws IOException {
        String msg = "REGISTER " + plate + " " + owner;
        out.println(msg);
        String response = in.readLine();
        if(response.equals("-1")) response = "ERROR";
        System.out.println(msg + " :: " + response);
    }

    public static void lookup(String plate) throws IOException {
        String msg = "LOOKUP " + plate;
        out.println(msg);
        String response = in.readLine();
        if(response.equals("NOT_FOUND")) response = "ERROR";
        System.out.println(msg + " :: " + response);
    }

    public static void main(String[] args) throws IOException {
        if(args.length < 4 || args.length > 5) {
            System.out.println("Usage: java Client <host_name> <port_number> <oper> <opnd>*");
            return;
        }

        Socket socket = new Socket(args[0], Integer.parseInt(args[1]));
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


        switch (args[2]) {
            case "register":
                register(args[3], args[4]);
                break;
            case "lookup":
                lookup(args[3]);
                break;
            default:
        }
    }
}
