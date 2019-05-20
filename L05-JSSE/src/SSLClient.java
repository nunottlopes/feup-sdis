import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

public class SSLClient {
    private static final boolean DEBUG = false;

    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) throws IOException {
        if(args.length < 4) {
            System.out.println("Usage: java SSLClient <host> <port> <oper> <opnd>* <cypher-suite>*");
            return;
        }

        if(DEBUG) {
            System.setProperty("javax.net.debug", "all");
        }

        SSLSocket socket;
        SSLSocketFactory ssf;

        ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try {
            socket = (SSLSocket) ssf.createSocket(args[0], Integer.parseInt(args[1]));
        }
        catch( IOException e) {
            System.out.println("Client - Failed to create SSL Socket");
            e.getMessage();
            return;
        }

        int mainArgsLen = -1;
        switch (args[2].toLowerCase()) {
            case "register":
                mainArgsLen = 5;
                break;
            case "lookup":
                mainArgsLen = 4;
                break;
        }

        if(mainArgsLen != -1 && args.length > mainArgsLen && !args[mainArgsLen].equals("")) {
//            System.out.println("Using custom Cipher Suites");
            String[] cyphers = Arrays.copyOfRange(args, mainArgsLen, args.length);
//            for(String cypher : cyphers) {
//                System.out.println(cypher);
//            }
            try {
                socket.setEnabledCipherSuites(cyphers);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return;
            }
        }

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        switch (args[2].toLowerCase()) {
            case "register":
                register(args[3], args[4]);
                break;
            case "lookup":
                lookup(args[3]);
                break;
            default:
        }
    }

    public static void register(String plate, String owner) throws IOException {
        String msg = "REGISTER " + plate + " " + owner;
        out.println(msg);
        String response = in.readLine();
        if(response.equals("-1")) response = "ERROR";
        System.out.println("SSLClient: " + msg + " : " + response);
    }

    public static void lookup(String plate) throws IOException {
        String msg = "LOOKUP " + plate;
        out.println(msg);
        String response = in.readLine();
        if(response.equals("NOT_FOUND")) response = "ERROR";
        System.out.println("SSLClient: " + msg + " : " + response);
    }
}
