package peer;

import channel.Channel;
import message.Message;
import protocol.InvalidProtocolExecution;
import protocol.ProtocolInfo;
import protocol.backup.BackupInitiator;
import rmi.RemoteInterface;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Peer implements RemoteInterface {

    private static final int MAX_THREADS = 200;
    private static final String ROOT = "database/peer"; // Final root should be ROOT + PeerID
    private static final String BACKUP_FOLDER = "backup/";
    private static final String RESTORE_FOLDER = "restore/";

    private static Peer instance;
    private FileManager fileManager;
    private ProtocolInfo protocolInfo;


    private String protocolVersion;
    private int peerID;
    private static String accessPoint;

    private HashMap<Channel.Type, Channel> channels;
    private DatagramSocket socket;

    private ScheduledThreadPoolExecutor pool;
    private ScheduledExecutorService executor;

    public static void main(String args[]) throws IOException, AlreadyBoundException {
        if(!checkArgs(args)) {
            System.out.println("Usage: Java Peer <protocol version> <peer id> " +
                    "<service access point> <MCReceiver address> <MCReceiver port> <MDBReceiver address> " +
                    "<MDBReceiver port> <MDRReceiver address> <MDRReceiver port>");
            return;
        }

        Peer.instance = new Peer(args);

        // RMI Connection
        RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(Peer.instance, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.bind(Peer.accessPoint, stub);

        System.out.println("--- Peer ready ---");
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

    public Peer(String args[]) throws IOException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        fileManager = new FileManager();
        protocolInfo = new ProtocolInfo();

        this.protocolVersion = args[0];
        this.peerID = Integer.parseInt(args[1]);
        this.accessPoint = args[2];
        this.pool = new ScheduledThreadPoolExecutor(MAX_THREADS);
        this.executor = Executors.newScheduledThreadPool(1);

        this.socket = new DatagramSocket();


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


    public void backup(String filepath, int replicationDegree) {
        System.out.println("\n---- BACKUP SERVICE ---- FILE PATH = " + filepath + " | REPLICATION DEGREEE = " + replicationDegree);
        BackupInitiator backupInitiator = new BackupInitiator(filepath, replicationDegree);
        try {
            backupInitiator.run();
        } catch (InvalidProtocolExecution e) {
            System.out.println(e);
        }
        System.out.println("---- FINISHED BACKUP SERVICE ----\n");
    }

    public void restore(String filepath) {
        System.out.println("RESTORE SERVICE -> FILE PATH = " + filepath);
        // TODO:

    }

    public void delete(String filepath) {
        System.out.println("DELETE SERVICE -> FILE PATH = " + filepath);
        // TODO:

    }

    public void reclaim(long spaceReclaim) {
        System.out.println("RECLAIM SERVICE -> DISK SPACE RECLAIM = " + spaceReclaim);
        // TODO:

    }

    public String state() {
        System.out.println("STATE SERVICE");
        // TODO:

//        - For each file whose backup it has initiated:
//              The file pathname
//              The backup service id of the file
//              The desired replication degree
//              For each chunk of the file:
//                  Its id
//                  Its perceived replication degree
//        - For each chunk it stores:
//              Its id
//              Its size (in KBytes)
//              Its perceived replication degree
//        - The peer's storage capacity, i.e. the maximum amount of disk space that can be used to store chunks, and the amount of storage (both in KBytes) used to backup the chunks.

        return "RESPONSE";
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

    public String getBackupPath(String fileid) { return ROOT + peerID + "/" + BACKUP_FOLDER + fileid + "/"; }

    public String getRestorePath() {
        return ROOT + peerID + "/" + RESTORE_FOLDER;
    }

    public FileManager getFileManager() { return fileManager; }

    public ProtocolInfo getProtocolInfo() {
        return protocolInfo;
    }

    public synchronized void send(Channel.Type channel, Message msg, boolean delayed) {
        int delay = 0;

        if(delayed) {
            Random r = new Random();
            delay = r.nextInt(400);
        }

        Channel c = channels.get(channel);

        executor.schedule(() -> {
            try {
                this.socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, c.getAddress(), c.getPort()));
//                System.out.print("Sent: " + msg.getHeaderString());
            } catch (IOException e) {
                System.out.println("Error sending message to " + c.getAddress());
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
}
