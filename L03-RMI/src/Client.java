import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class Client {

    public Client(){}

    public static void main(String[] args) {
        if(args.length < 4 || args.length > 5) {
            System.out.println("Usage: java Client <host_name> <remote_object_name> <oper> <opnd>*");
            return;
        }

        try {
            Registry registry = LocateRegistry.getRegistry(args[0]);
            RemoteInterface stub = (RemoteInterface) registry.lookup(args[1]);
            String response;
            switch (args[2].toLowerCase()) {
                case "register":
                    response = stub.register(args[3], args[4]);
                    if(response.equals("-1")) response = "ERROR";
                    System.out.println(args[2] + " " + args[3] + " " + args[4] + " :: " + response);
                    break;
                case "lookup":
                    response = stub.lookup(args[3]);
                    if(response.equals("NOT_FOUND")) response = "ERROR";
                    System.out.println(args[2] + " " + args[3] + " :: " + response);
                    break;
                default:
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
