package peer;

import channel.Channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Peer {

    private static final int MAX_THREADS = 200;
    private static final String ROOT = "peer"; // Final root should be ROOT + PeerID
    private static final String BACKUP_FOLDER = "backup/";
    private static final String RESTORE_FOLDER = "restore/";

    private static Peer instance;
    private FileManager fileManager;


    private String protocolVersion;
    private int peerID;
    private String accessPoint;

    private HashMap<Channel.Type, Channel> channels;
    private DatagramSocket socket;

    private ScheduledThreadPoolExecutor pool;

    public static void main(String args[]) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        if(!checkArgs(args))
            System.out.println("Usage: Java Peer <protocol version> <peer id> " +
                    "<service access point> <MCReceiver address> <MCReceiver port> <MDBReceiver address> " +
                    "<MDBReceiver port> <MDRReceiver address> <MDRReceiver port>");
        else
            Peer.instance = new Peer(args);
    }

    private static boolean checkArgs(String args[]) {
        if(args.length != 9)
            return false;
        else
            return true;
    }


    public static Peer getInstance() {
        if(instance != null)
            return instance;
        else
            return null;
    }


    private Peer(String args[]) {
        fileManager = new FileManager();

        this.protocolVersion = args[0];
        this.peerID = Integer.parseInt(args[1]);
        this.accessPoint = args[2];
        this.pool = new ScheduledThreadPoolExecutor(MAX_THREADS);

        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        Channel MC = new Channel(args[3], Integer.parseInt(args[4]), Channel.Type.MC);
        Channel MDB = new Channel(args[5], Integer.parseInt(args[6]), Channel.Type.MDB);
        Channel MDR = new Channel(args[7], Integer.parseInt(args[8]), Channel.Type.MDR);

        new Thread(MC).start();
        new Thread(MDB).start();
        new Thread(MDR).start();

        channels = new HashMap<>();
        channels.put(Channel.Type.MC, MC);
        channels.put(Channel.Type.MDB, MDB);
        channels.put(Channel.Type.MDR, MDR);
    }

    private void send(String message, Channel.Type channel) throws IOException {
        this.socket.send(new DatagramPacket(
                message.getBytes(),
                message.getBytes().length,
                channels.get(channel).getAddress(),
                channels.get(channel).getPort()));
    }

    public ScheduledThreadPoolExecutor getPool() {
        return pool;
    }

    public int getId() {
        return peerID;
    }

    public String getVersion() {
        return protocolVersion;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public Channel getChannel(Channel.Type type) {
        return channels.get(type);
    }

    public String getBackupPath(String fileid) { return ROOT + peerID + "/" + BACKUP_FOLDER + fileid + "/"; }

    public String getRestorePath() {
        return ROOT + peerID + "/" + RESTORE_FOLDER;
    }

    public FileManager getFileManager() { return fileManager; }
}
