package peer;

import channel.Channel;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Peer {

    private static final int MAX_THREADS = 10;

    private String protocolVersion;
    private int peerID;
    private String accessPoint;

    private Channel MC;
    private Channel MDB;
    private Channel MDR;

    private ScheduledThreadPoolExecutor executor;

    public static void main(String args[]) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        Peer peer;
        if(!checkArgs(args))
            System.out.println("Usage: Java Peer <protocol version> <peer id> " +
                    "<service access point> <MCReceiver address> <MCReceiver port> <MDBReceiver address>" +
                    "<MDBReceiver port> <MDRReceiver address> <MDRReceiver port>");
        else
            peer = new Peer(args);
    }

    private static boolean checkArgs(String args[]) {
        if(args.length != 9)
            return false;
        else
            return true;
    }

    private Peer(String args[]) {
        this.protocolVersion = args[0];
        this.peerID = Integer.parseInt(args[1]);
        this.accessPoint = args[2];
        this.executor = new ScheduledThreadPoolExecutor(MAX_THREADS);

        this.MC = new Channel(args[3], Integer.parseInt(args[4]), Channel.Type.MC);
        this.MDB = new Channel(args[5], Integer.parseInt(args[6]), Channel.Type.MDB);
        this.MDR = new Channel(args[7], Integer.parseInt(args[8]), Channel.Type.MDR);

        startChannels(args[3], Integer.parseInt(args[4]),
                args[5], Integer.parseInt(args[6]),
                args[7], Integer.parseInt(args[8]));


    }

    private void startChannels(String MC_address, int MC_port, String MDB_address, int MDB_port, String MDR_address, int MDR_port) {
        this.MC = new Channel(MC_address, MC_port, Channel.Type.MC);
        this.MDB = new Channel(MDB_address, MDB_port, Channel.Type.MDB);
        this.MDR = new Channel(MDR_address, MDR_port, Channel.Type.MDR);

        new Thread(this.MC).start();
        new Thread(this.MDB).start();
        new Thread(this.MDR).start();

    }
}
