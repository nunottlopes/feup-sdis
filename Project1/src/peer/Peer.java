import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Peer{

    private static final int MAXIMUM_NUMBER_THREADS = 10;

    private String protocolVersion;
    private int peerID;
    private String accessPoint;

    private Channel MC;
    private Channel MDB;
    private Channel MDR;

    private ScheduledExecutorService thread = Executors.newScheduledThreadPool(MAXIMUM_NUMBER_THREADS);

    public static void main(String args[]) throws IOException {
        Peer peer;
        if(!checkArgs(args))
            System.out.println("Usage: Java Peer <protocol version> <peer id> <service access point> <MCReceiver address> <MCReceiver port> <MDBReceiver address> <MDBReceiver port> <MDRReceiver address> <MDRReceiver port>");
        else
            peer = new Peer(args);
    }

    private Peer(String args[]) throws IOException{
        this.protocolVersion = args[0];
        this.peerID = Integer.parseInt(args[1]);
        this.accessPoint = args[2];

        this.MC = new Channel(args[3], Integer.parseInt(args[4]));
        this.MDB = new Channel(args[5], Integer.parseInt(args[6]));
        this.MDR = new Channel(args[7], Integer.parseInt(args[8]));
    }

    private static boolean checkArgs(String args[]){
        if(args.length != 9)
            return false;
        else
            return true;
    }
}
