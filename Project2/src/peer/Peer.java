package peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import channel.Channel;
import chord.Chord;
import message.Message;
import protocol.InvalidProtocolExecution;
import protocol.ProtocolInfo;
import protocol.backup.BackupInitiator;
import protocol.delete.DeleteInitiator;
import protocol.restore.RestoreInitiator;
import protocol.reclaim.ReclaimInitiator;
import rmi.RemoteInterface;

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
     * Peer id
     */
    private int peerID;

    /**
     * Peer access point
     */
    private static String accessPoint;

    /**
     * Peer pool
     */
    private ScheduledThreadPoolExecutor pool;

    /**
     * Peer executor
     */
    private ScheduledExecutorService executor;

    private long timeout = 1 * 5000;

    private int maxChordPeers = 128;
    private Chord chord;

    private Channel channel;

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
            System.out.println("Usage: Java Peer <peer id> " +
                    "<service access point> <TCP port> <Chord port> [<ConnectionPeer address> <ConnectionPeer port>]");
            return;
        }

        try {
            Peer.instance = new Peer(args);
        } catch (IOException e) {
            System.exit(1);
        }

        // RMI Connection
        try{
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(Peer.instance, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(Peer.accessPoint, stub);
            System.out.println("--- Client service ready ---");
        } catch (Exception e) {
            System.out.println("--- Client service unavailable ---");
        }

        System.out.println("--- Peer ready ---");
    }

    /**
     * Peer singleton constructor
     * @param args
     * @throws IOException
     */
    public Peer(String args[]) throws IOException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        System.setProperty("javax.net.ssl.trustStore", "keys/truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        System.setProperty("javax.net.ssl.keyStore", "keys/keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        this.peerID = Integer.parseInt(args[0]);
        this.accessPoint = args[1];

        if(!loadPeerFromFile()){
            fileManager = new FileManager();
            protocolInfo = new ProtocolInfo();
        }

        this.pool = new ScheduledThreadPoolExecutor(MAX_THREADS);
        this.executor = Executors.newScheduledThreadPool(1);

        if(args.length == 6){
            int port = Integer.parseInt(args[3]);
            InetSocketAddress connectionPeer = new InetSocketAddress(args[4], Integer.parseInt(args[5]));
            this.chord = new Chord(maxChordPeers, port, connectionPeer);
        } else{
            int port = Integer.parseInt(args[3]);
            this.chord = new Chord(maxChordPeers, port);
        }
        System.out.println("--- Chord Id is " + this.chord.getId() + " ---");
        this.channel = new Channel(Integer.parseInt(args[2]));

        new Thread(this.channel).start();
    }

    /**
     * Checks if main args are valid
     * @param args
     * @return true if valid, false otherwise
     */
    private static boolean checkArgs(String args[]) {
        if(args.length != 4 && args.length != 6)
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

    /**
     * Initializes restore protocol
     * @param filepath
     * @param enhanced
     */
    public void restore(String filepath, boolean enhanced) {
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
        System.out.println("---- FINISHED DELETE SERVICE ----");
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
                ret += "\t- Chunk Id = "+ chunkno + "\n";
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
                ret += "\t- Chunk Id = "+ chunkno + "\n\t\t- Chunk Size = "+ chunkMap.get(chunkno).getSize() + " Bytes\n";
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
     * @param msg
     */
    public synchronized void send(Message msg, InetAddress destination) {

        InetSocketAddress address = new InetSocketAddress(destination, this.channel.getPort());

        SSLSocket socket;
        SSLSocketFactory ssf;

        ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try {
            socket = (SSLSocket) ssf.createSocket();
            socket.connect(address, (int) this.timeout);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(msg);
            socket.close();
        }
        catch( IOException e) {
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


    public int getMaxChordPeers() {
        return maxChordPeers;
    }


}
