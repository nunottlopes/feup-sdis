package peer;

import channel.Channel;
import message.Message;
import protocol.InvalidProtocolExecution;
import protocol.ProtocolInfo;
import protocol.backup.BackupInitiator;
import rmi.RemoteInterface;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;

public class Peer implements RemoteInterface {

    private static final int MAX_THREADS = 200;
    private static final String ROOT_CHUNKS = "database/chunks/peer"; // Final chunk root should be ROOT_CHUNKS + PeerID
    private static final String BACKUP_FOLDER = "backup/";
    private static final String RESTORE_FOLDER = "restore/";
    private static final String ROOT_PEERS = "database/peers/";

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

        this.protocolVersion = args[0];
        this.peerID = Integer.parseInt(args[1]);
        this.accessPoint = args[2];

        if(!loadPeerFromFile()){
            fileManager = new FileManager();
            protocolInfo = new ProtocolInfo();
        }

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
        writePeerToFile();
        System.out.println("---- FINISHED BACKUP SERVICE ----");
    }

    public void restore(String filepath) {
        System.out.println("RESTORE SERVICE -> FILE PATH = " + filepath);
        // TODO:

        writePeerToFile();
    }

    public void delete(String filepath) {
        System.out.println("DELETE SERVICE -> FILE PATH = " + filepath);
        // TODO:

        writePeerToFile();
    }

    public void reclaim(long spaceReclaim) {
        System.out.println("RECLAIM SERVICE -> DISK SPACE RECLAIM = " + spaceReclaim);
        // TODO:

        writePeerToFile();
    }

    public String state() {
        System.out.println("\n---- STATE SERVICE ----");
        String ret = "INITIATED BACKUP FILES";
        if(fileManager.getBackedupFiles().entrySet().size() == 0)
            ret += "\nNo files backup initialized here\n";
        for (Map.Entry<String, ConcurrentHashMap<Integer, Chunk>> item : fileManager.getBackedupFiles().entrySet()){
            String path_name = item.getKey();
            ConcurrentHashMap<Integer, Chunk> chunkMap = item.getValue();
            String fileId = chunkMap.get(0).getFileId();
            int desiredRepDegree = chunkMap.get(0).getRepDegree();

            ret += "\n> File Pathname = " + path_name + "\n> File ID = " + fileId + "\n> Desired Replication Degree= " + desiredRepDegree + "\n";

            for(Integer chunkno : chunkMap.keySet()){
                ret += "\t- Chunk ID = "+ chunkno + "\n\t\t- Perceived Replication Degree = " + chunkMap.get(chunkno).getPerceivedRepDegree() + "\n";
            }

        }
        ret += "\n\nSTORED CHUNKS";
        if(fileManager.getChunksStored().entrySet().size() == 0)
            ret += "\nNo chunks stored here";
        for (Map.Entry<String, ConcurrentHashMap<Integer, Chunk>> item : fileManager.getChunksStored().entrySet()){
            String fileId = item.getKey();
            ConcurrentHashMap<Integer, Chunk> chunkMap = item.getValue();

            ret += "\n> File ID = " + fileId + "\n";

            for(Integer chunkno : chunkMap.keySet()){
                ret += "\t- Chunk ID = "+ chunkno + "\n\t\t- Chunk Size = "+ chunkMap.get(chunkno).getSize() + " Bytes\n\t\t- Perceived Replication Degree = " + chunkMap.get(chunkno).getPerceivedRepDegree() + "\n";
            }

        }
        ret += "\n\nPEER STORAGE\n> Peer Max Memory = " + (fileManager.getMaxMemory()/1000) + " KBytes\n> Used Memory = " + (fileManager.getUsed_mem()/1000) + " KBytes\n";
        System.out.println("---- FINISHED STATE SERVICE ----");
        return ret;
    }

    public boolean loadPeerFromFile(){
        try {
            FileInputStream fis_filemanager = new FileInputStream(new File(getStoredFileManagerFilePath()));
            ObjectInputStream ois_filemanager = new ObjectInputStream(fis_filemanager);
            fileManager = (FileManager) ois_filemanager.readObject();

            FileInputStream fis_protocolinfo = new FileInputStream(new File(getStoredProtocolInfoFilePath()));
            ObjectInputStream ois_protocolinfo = new ObjectInputStream(fis_protocolinfo);
            protocolInfo = (ProtocolInfo) ois_protocolinfo.readObject();

        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void writePeerToFile(){
        File directory = new File(ROOT_PEERS);
        if(!directory.exists())
            directory.mkdirs();
        try {
            FileOutputStream fos_filemanager = new FileOutputStream(new File(getStoredFileManagerFilePath()));
            ObjectOutputStream oos_filemanager = new ObjectOutputStream(fos_filemanager);
            oos_filemanager.writeObject(fileManager);

            FileOutputStream fos_protocolinfo = new FileOutputStream(new File(getStoredProtocolInfoFilePath()));
            ObjectOutputStream oos_protocolinfo = new ObjectOutputStream(fos_protocolinfo);
            oos_protocolinfo.writeObject(protocolInfo);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public String getStoredFileManagerFilePath(){
        return ROOT_PEERS + "peer" + peerID + "-file_manager";
    }

    public String getStoredProtocolInfoFilePath(){
        return ROOT_PEERS + "peer" + peerID + "-protocol_info";
    }

    public String getBackupPath(String fileid) { return ROOT_CHUNKS + peerID + "/" + BACKUP_FOLDER + fileid + "/"; }

    public String getRestorePath() {
        return ROOT_CHUNKS + peerID + "/" + RESTORE_FOLDER;
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
