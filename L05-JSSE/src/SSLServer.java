import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Hashtable;

public class SSLServer implements Runnable {
    private static final boolean DEBUG = false;

    private static Hashtable<String, String> data = new Hashtable<>();

    public static void main(String args[]) throws IOException {
        if(args.length < 1) {
            System.out.println("Usage: java SSLServer <port> <cypher-suite>*");
            return;
        }

        if(DEBUG) {
            System.setProperty("javax.net.debug", "all");
        }

        SSLServerSocket ssocket;
        SSLServerSocketFactory ssf;

        ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        try {
            ssocket = (SSLServerSocket) ssf.createServerSocket(Integer.parseInt(args[0]));
        }
        catch( IOException e) {
            System.out.println("Server - Failed to create SSLServerSocket");
            e.getMessage();
            return;
        }


        if(args.length > 1 && !args[1].equals("")) {
//            System.out.println("Using Custom Cipher Suits");
            String[] cyphers = Arrays.copyOfRange(args, 1, args.length);
//            for(String cypher : cyphers) {
//                System.out.println(cypher);
//            }
            try {
                ssocket.setEnabledCipherSuites(cyphers);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return;
            }
        }

        while(true) {
            SSLSocket socket = (SSLSocket) ssocket.accept();
            new Thread(new SSLServer(socket)).start();
        }

    }




    SSLSocket socket;

    public SSLServer(SSLSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
//            System.out.println(in.readLine());
            out.println(handleRequest(in.readLine()));

            in.close();
            out.close();
            this.socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String handleRequest(String req) {
        System.out.print("SSLServer: " + req + " : ");

        String ret = "ERROR";
        String[] args = req.split(" ");
        switch (args[0]) {
            case "REGISTER":
                ret = register(args[1], args[2]);
                break;
            case "LOOKUP":
                ret = lookup(args[1]);
                break;
        }
        System.out.println(ret);
        return ret;
    }

    private synchronized String register(String plate, String owner) {
        if(this.data.containsKey(plate)) return "-1";
        this.data.put(plate, owner);
        return Integer.toString(this.data.size());
    }

    private synchronized String lookup(String plate) {
        if(!this.data.containsKey(plate)) return "NOT_FOUND";
        return plate + " " + this.data.get(plate);
    }
}
