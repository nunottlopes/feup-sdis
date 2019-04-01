import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

public class Server implements Runnable {
    Socket socket;
    private static Hashtable<String, String> data = new Hashtable<>();

    public Server(Socket socket) {
        this.socket = socket;
    }


    public static void main(String args[]) throws IOException {
        ServerSocket ssocket = new ServerSocket(Integer.parseInt(args[0]));

        while(true) {
            Socket socket = ssocket.accept();
            new Thread(new Server(socket)).start();
        }

    }

    @Override
    public void run() {
        try {
            PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            //out.println(in.readLine());
            out.println(handleRequest(in.readLine()));

            in.close();
            out.close();
            this.socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String handleRequest(String req) {
        System.out.print(req + " :: ");
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

    private String register(String plate, String owner) {
        if(this.data.containsKey(plate)) return "-1";
        this.data.put(plate, owner);
        return Integer.toString(this.data.size());
    }

    private String lookup(String plate) {
        if(!this.data.containsKey(plate)) return "NOT_FOUND";
        return plate + " " + this.data.get(plate);
    }
}
