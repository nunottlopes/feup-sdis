import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;

public class Server implements RemoteInterface{
    private Hashtable<String, String> data = new Hashtable<>();

    public Server() {}

    public String register(String plate, String owner) {
        String ret;
        if(this.data.containsKey(plate)) ret = "-1";
        this.data.put(plate, owner);
        ret = Integer.toString(this.data.size());
        System.out.println("REGISTER " + plate + " " + owner + " :: " + ret);
        return ret;
    }

    public String lookup(String plate) {
        String ret = plate + " " + this.data.get(plate);
        if(!this.data.containsKey(plate)) ret = "NOT_FOUND";
        System.out.println("LOOKUP " + plate + " :: " + ret);
        return ret;
    }

    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Usage: java Server <remote_object_name>");
            return;
        }

        try {
            Server server = new Server();
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(server, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[0], stub);

            System.out.println("Server ready");

        } catch (Exception e) {
            System.out.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
