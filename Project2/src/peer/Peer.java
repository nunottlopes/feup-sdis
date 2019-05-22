package peer;

import channel.Channel;
import chord.Chord;
import message.Message;
import protocol.InvalidProtocolExecution;
import protocol.ProtocolInfo;
import protocol.backup.BackupInitiator;
import protocol.delete.DeleteInitiator;
import protocol.reclaim.ReclaimInitiator;
import protocol.restore.RestoreInitiator;
import rmi.RemoteInterface;


import java.io.*;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Peer class
 */
public class Peer implements RemoteInterface {

    /**
     * Pool max threads
     */
    private static final int MAX_THREADS = 200;

    /**
     * Peer storage root path
     */
    private static final String ROOT = "database/peer";

    /**
     * Backup folder, to save backed up chunks
     */
    private static final String BACKUP_FOLDER = "backup/";

    /**
     * Restore folder, to save restored files
     */
    private static final String RESTORE_FOLDER = "restore/";

    /**
     * State folder, to save Peer information
     */
    private static final String STATE_FOLDER = "state/";

    /**
     * Peer singleton instance
     */
    private static Peer instance;

    /**
     * Peer file manager
     */
    private FileManager fileManager;

    /**
     * Peer protocol info
     */
    private ProtocolInfo protocolInfo;

    /**
     * Protocol version
     */
    private String protocolVersion;

    /**
     * Peer version (enhanced or not)
     */
    private boolean enhanced;

    /**
     * Peer id
     */
    private int peerID;

    /**
     * Peer access point
     */
    private static String accessPoint;

    /**
     * Peer channels
     */
    private HashMap<Channel.Type, Channel> channels;

    /**
     * Peer socket
     */
    private Socket socket;

    /**
     * Peer pool
     */
    private ScheduledThreadPoolExecutor pool;

    /**
     * Peer executor
     */
    private ScheduledExecutorService executor;

    private long timeout = 1 * 5000;

    private int maxChordPeers = 32;
    private Chord chord;

    /**
     * Peer main function
     * Creates Peer singleton object
     * Starts RMI connection
     *
     * @param args
     * @throws IOException
     * @throws AlreadyBoundException
     */
    public static void main(String args[]) throws IOException, AlreadyBoundException {
        if(!checkArgs(args)) {
            System.out.println("Usage: Java Peer <protocol version> <peer id> " +
                    "<service access point> <MCReceiver port> " +
                    "<MDBReceiver port> <MDRReceiver port> <Chord port> [<ConnectionPeer address> <ConnectionPeer port>]");
            return;
        }

        Peer.instance = new Peer(args);

        // RMI Connection
        RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(Peer.instance, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.bind(Peer.accessPoint, stub);

        System.out.println("--- Peer ready ---");
    }

    /**
     * Peer singleton constructor
     * @param args
     * @throws IOException
     */
    public Peer(String args[]) throws IOException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        this.protocolVersion = args[0];

        if(this.protocolVersion.equals("1.0")) this.enhanced = false;
        else this.enhanced = true;

        this.peerID = Integer.parseInt(args[1]);
        this.accessPoint = args[2];

        if(!loadPeerFromFile()){
            fileManager = new FileManager();
            protocolInfo = new ProtocolInfo();
        }

        this.pool = new ScheduledThreadPoolExecutor(MAX_THREADS);
        this.executor = Executors.newScheduledThreadPool(1);

        if(args.length == 9){
            int port = Integer.parseInt(args[6]);
            InetSocketAddress connectionPeer = new InetSocketAddress(args[7], Integer.parseInt(args[8]));
            this.chord = new Chord(maxChordPeers, port, connectionPeer, true);
        } else{
            int port = Integer.parseInt(args[6]);
            this.chord = new Chord(maxChordPeers, port);
        }

        Channel MC = new Channel(Integer.parseInt(args[3]), Channel.Type.MC);
        Channel MDB = new Channel(Integer.parseInt(args[4]), Channel.Type.MDB);
        Channel MDR = new Channel(Integer.parseInt(args[5]), Channel.Type.MDR);

        new Thread(MC).start();
        new Thread(MDB).start();
        new Thread(MDR).start();

        channels = new HashMap<>();
        channels.put(Channel.Type.MC, MC);
        channels.put(Channel.Type.MDB, MDB);
        channels.put(Channel.Type.MDR, MDR);
    }

    /**
     * Checks if main args are valid
     * @param args
     * @return true if valid, false otherwise
     */
    private static boolean checkArgs(String args[]) {
        if(args.length != 7 && args.length != 9)
            return false;
        else
            return true;
    }

    /**
     * Return Peer instance
     * @return instance
     */
    public static Peer getInstance() {
        if(instance != null)
            return instance;
        else
            return null;
    }

    public Chord getChord() {
        return chord;
    }

    /**
     * Initializes backup protocol
     * @param filepath
     * @param replicationDegree
     * @param enhanced
     */
    public void backup(String filepath, int replicationDegree, boolean enhanced) {
        if(enhanced && !this.enhanced) {
            System.out.println("-- Incompatible peer version with backup enhancement --");
            return;
        }
        if(!enhanced && this.enhanced) {
            System.out.println("-- Incompatible peer version with vanilla backup --");
            return;
        }

        System.out.println("\n---- BACKUP SERVICE ---- FILE PATH = " + filepath + " | REPLICATION DEGREEE = " + replicationDegree);
        BackupInitiator backupInitiator = new BackupInitiator(filepath, replicationDegree);
        try {
            backupInitiator.run();
        } catch (InvalidProtocolExecution e) {
            System.out.println(e);
        }
        writePeerToFile();
        System.out.println("\n---- FINISHED BACKUP SERVICE ----");
    }

    /**
     * Initializes restore protocol
     * @param filepath
     * @param enhanced
     */
    public void restore(String filepath, boolean enhanced) {
        if(enhanced && !this.enhanced) {
            System.out.println("-- Incompatible peer version with restore enhancement --");
            return;
        }
        if(!enhanced && this.enhanced) {
            System.out.println("-- Incompatible peer version with vanilla restore --");
            return;
        }

        System.out.println("\n----- RESTORE SERVICE ----- FILE PATH = " + filepath);
        RestoreInitiator restoreInitiator = new RestoreInitiator(filepath);
        try {
            restoreInitiator.run();
        } catch (InvalidProtocolExecution e) {
            System.out.println(e);
        }
        writePeerToFile();
        System.out.println("---- FINISHED RESTORE SERVICE ----");
    }

    /**
     * Initializes delete protocol
     * @param filepath
     */
    public void delete(String filepath) {
        System.out.println("\n----- DELETE SERVICE ----- FILE PATH = " + filepath);
        DeleteInitiator deleteInitiator = new DeleteInitiator(filepath);
        try {
            deleteInitiator.run();
        } catch (InvalidProtocolExecution e) {
            System.out.println(e);
        }
        writePeerToFile();
        System.out.println("\n---- FINISHED DELETE SERVICE ----");
    }

    /**
     * Initializes reclaim protocol
     * @param spaceReclaim
     */
    public void reclaim(long spaceReclaim) {
        System.out.println("\n----- RECLAIM SERVICE ----- DISK SPACE RECLAIM = " + spaceReclaim);
        ReclaimInitiator reclaimInitiator = new ReclaimInitiator(spaceReclaim);
        try {
            reclaimInitiator.run();
        } catch (InvalidProtocolExecution e) {
            System.out.println(e);
        }
        writePeerToFile();
        System.out.println("\n---- FINISHED RECLAIM SERVICE ----");
    }

    /**
     * Gets Peer state
     * @return peer state to TestApp
     */
    public String state() {
        System.out.println("\n---- STATE SERVICE ----");
        String ret = "\nINITIATED BACKUP FILES";
        if(fileManager.getBackedupFiles().entrySet().size() == 0)
            ret += "\nNo files backup initialized here\n";
        for (Map.Entry<String, ConcurrentHashMap<Integer, Chunk>> item : fileManager.getBackedupFiles().entrySet()){
            String path_name = item.getKey();
            ConcurrentHashMap<Integer, Chunk> chunkMap = item.getValue();
            ConcurrentHashMap.Entry<Integer, Chunk> firstEntry = chunkMap.entrySet().iterator().next();
            String fileId = chunkMap.get(firstEntry.getKey()).getFileId();
            int desiredRepDegree = chunkMap.get(firstEntry.getKey()).getRepDegree();

            ret += "\n> File Pathname = " + path_name + "\n> File Id = " + fileId + "\n> Desired Replication Degree= " + desiredRepDegree + "\n";

            for(Integer chunkno : chunkMap.keySet()){
                ret += "\t- Chunk Id = "+ chunkno + "\n\t\t- Perceived Replication Degree = " + chunkMap.get(chunkno).getPerceivedRepDegree() + "\n";
            }

        }
        ret += "\nSTORED CHUNKS";
        if(fileManager.getChunksStored().entrySet().size() == 0)
            ret += "\nNo chunks stored here\n";
        for (Map.Entry<String, ConcurrentHashMap<Integer, Chunk>> item : fileManager.getChunksStored().entrySet()){
            String fileId = item.getKey();
            ConcurrentHashMap<Integer, Chunk> chunkMap = item.getValue();

            ret += "\n> File Id = " + fileId + "\n";

            for(Integer chunkno : chunkMap.keySet()){
                ret += "\t- Chunk Id = "+ chunkno + "\n\t\t- Chunk Size = "+ chunkMap.get(chunkno).getSize() + " Bytes\n\t\t- Perceived Replication Degree = " + chunkMap.get(chunkno).getPerceivedRepDegree() + "\n";
            }

        }
        ret += "\nPEER STORAGE\n> Peer Max Memory = " + (fileManager.getMaxMemory()/1000) + " KBytes\n> Used Memory = " + (fileManager.getUsed_mem()/1000) + " KBytes\n";
        System.out.println("---- FINISHED STATE SERVICE ----");
        return ret;
    }

    /**
     * Loads Peer previous state from file
     * @return
     */
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

    /**
     * Writes Peer state to file
     */
    public void writePeerToFile(){
        File directory = new File(ROOT + peerID + "/" + STATE_FOLDER);
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

    /**
     * Send message in the channel
     * @param channel
     * @param msg
     */
    public synchronized void send(Channel.Type channel, Message msg, InetAddress destination) {
        Channel c = channels.get(channel);

        //InetSocketAddress address = new InetSocketAddress(destination, c.getPort());
        //TODO:
        InetSocketAddress address = new InetSocketAddress(destination, 8002);
        try
        {
            Socket connection = new Socket();
            connection.connect(address, (int) this.timeout);
            ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());

            oos.writeObject(msg);

            connection.close();
        }
        catch (IOException e1)
        {
            System.out.println("Error sending message to " + address);
        }
    }

    /**
     * Returns ScheduledThreadPoolExecutor
     * @return pool
     */
    public ScheduledThreadPoolExecutor getPool() {
        return pool;
    }

    /**
     * Gets Peer id
     * @return id
     */
    public int getId() {
        return peerID;
    }

    /**
     * Gets Peer protocol version
     * @return version
     */
    public String getVersion() {
        return protocolVersion;
    }

    /**
     * Gets Peer File Manager state file path
     * @return file manager state file path
     */
    public String getStoredFileManagerFilePath(){
        return ROOT + peerID + "/" + STATE_FOLDER + "file_manager";
    }

    /**
     * Gets Peer Protocol Info state file path
     * @return protocol info state file path
     */
    public String getStoredProtocolInfoFilePath(){
        return ROOT + peerID + "/" + STATE_FOLDER + "protocol_info";
    }

    /**
     * Gets backup path for file
     * @param fileid
     * @return backup path
     */
    public String getBackupPath(String fileid) { return ROOT + peerID + "/" + BACKUP_FOLDER + fileid + "/"; }

    /**
     * Gets backup folder path
     * @return backup folder path
     */
    public String getBackupFolder(){
        return ROOT + peerID + "/" + BACKUP_FOLDER;
    }

    /**
     * Gets restore folder path
     * @return restore folder path
     */
    public String getRestoreFolder() {
        return ROOT + peerID + "/" + RESTORE_FOLDER;
    }

    /**
     * Returns Peer File Manager
     * @return file manager
     */
    public FileManager getFileManager() { return fileManager; }

    /**
     * Returns Peer Protocol Info
     * @return protocol info
     */
    public ProtocolInfo getProtocolInfo() {
        return protocolInfo;
    }

    /**
     * Returns Peer ScheduledExecutorService
     * @return executor
     */
    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    /**
     * Returns Peer version
     * @return true if version is enhanced, false otherwise
     */
    public boolean isEnhanced() {
        return this.enhanced;
    }

    public int getMaxChordPeers() {
        return maxChordPeers;
    }
}
