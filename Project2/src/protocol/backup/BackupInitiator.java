package protocol.backup;

import chord.Chord;
import globals.Globals;
import peer.Chunk;
import peer.Peer;
import protocol.InvalidProtocolExecution;
import protocol.ProtocolInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static globals.Globals.getFileData;
import static globals.Globals.splitIntoChunks;
import static message.SendMessage.sendPUTCHUNK;

/**
 * BackupInitiator class
 */
public class BackupInitiator {

    /**
     * Max number of chunks that a file can be split to
     */
    public static final int MAX_NUM_CHUNKS = 1000000;

    /**
     * Max number of retransmissions of PUTCHUNK message
     */
    public static final int MAX_RETRANSMISSIONS = 5;

    /**
     * Backup protocol pool
     */
    private ThreadPoolExecutor pool;

    /**
     * Path for the file that is going to be backed up
     */
    private String path;

    /**
     * File desired replication degree
     */
    private int repDegree;

    /**
     * File id
     */
    private String fileId;

    /**
     * File to be backed up
     */
    private File file;

    /**
     * BackupInitiator constructor
     * @param path
     * @param repDegree
     */
    public BackupInitiator(String path, int repDegree) {
        this.path = path;
        this.repDegree = repDegree;
        this.pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
    }

    /**
     * Runs Backup protocol for initiator-peer
     * @throws InvalidProtocolExecution
     */
    public void run() throws InvalidProtocolExecution {

        file = new File(path);

        byte[] data;

        try {
            data = getFileData(file);
        } catch (FileNotFoundException e) {
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.BACKUP, "File not found!");
        }

        fileId = Globals.generateFileId(file);

        ArrayList<Chunk> chunks = splitIntoChunks(data, fileId, repDegree);

        if(!validBackup(repDegree, chunks.size())) return;

        Peer.getInstance().getProtocolInfo().startBackup(fileId);

        CountDownLatch latch = new CountDownLatch(chunks.size());

        for(Chunk c : chunks) {
            pool.execute(() -> {
                int delay = 1;

                ProtocolInfo status = Peer.getInstance().getProtocolInfo();

                for(int i = 0; i < MAX_RETRANSMISSIONS; i++) {
                    String name = fileId + c.getChunkNo();
                    int hash = Math.floorMod(Chord.sha1(name), Peer.getInstance().getMaxChordPeers());
                    
                    //System.out.println("Asked for chunk with hash " + hash);
                    for (int n = 0; n < repDegree; n++){
                        //System.out.println("LOOKING UP FOR HASH " + hash);
                        String[] message = Peer.getInstance().getChord().sendLookup(hash, true);

                        try {
                            TimeUnit.SECONDS.sleep((long) Math.random());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if(message != null){
                            System.out.println("HASH= " + hash + " HASH_PEER= "+ message[2]);
                        }

                        if(message != null){
                            try {
                                InetAddress address = InetAddress.getByName(message[3]);
                                if (!message[3].equals(Peer.getInstance().getChord().getAddress())){
                                    //if(!status.hasPeerSavedChunk(fileId, c.getChunkNo(), address))
                                        //System.out.println("SENDING CHUNK "+c.getChunkNo() + " rep index= "+n);
                                        sendPUTCHUNK(fileId, c.getChunkNo(), c.getRepDegree(), c.getData(), address);
                                } else{
                                    //System.out.println("SAVING CHUNK "+c.getChunkNo() + " rep index= "+n);
                                    new Backup(fileId, c.getChunkNo(), c.getRepDegree(), c.getData(), address);
                                }
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }

                            //System.out.println("CHORD RECEIVED MESSAGE HASH " +  message[2]);
                            hash = Integer.parseInt(message[2]) + 1;
                        }
                        else {
                            n--;
                            //System.out.println("NÃO ENVIOU FILE ID: "+fileId+" CHUNK NO: "+ c.getChunkNo());
                        }
                    }

                    if(i != MAX_RETRANSMISSIONS-1){
                        try {
                            TimeUnit.SECONDS.sleep(delay);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        delay *= 2;
                        int currentRepDegree = status.getChunkRepDegree(fileId, c.getChunkNo());
                        if(currentRepDegree >= repDegree){
                            //System.out.println("SUCESSO CHUNK "+c.getChunkNo() + " repdegree= " + currentRepDegree);
                            break;
                        }
                    }
                }

                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Peer.getInstance().getProtocolInfo().endBackup(fileId, repDegree, path);
    }

    /**
     * Checks if backup is valid
     * @param repDegree
     * @param n_chunks
     * @return true if valid, false otherwise
     * @throws InvalidProtocolExecution
     */
    private boolean validBackup(int repDegree, int n_chunks) throws InvalidProtocolExecution {
        if(repDegree > 9) {
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.BACKUP, "Max number of replication degree is 9");
        }
        if(n_chunks > MAX_NUM_CHUNKS) {
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.BACKUP,"Backup Error: Max file size is 64GBytes");
        }
        return true;
    }
}
