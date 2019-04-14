package protocol.restore;

import globals.Globals;
import peer.Chunk;
import peer.Peer;
import protocol.InvalidProtocolExecution;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static message.SendMessage.sendGETCHUNK;
import static message.SendMessage.sendGETCHUNKENH;

/**
 * RestoreInitiator class
 */
public class RestoreInitiator {

    /**
     * Max number of retransmissions for GETCHUNK/GETCHUNKENH messages
     */
    private static final int MAX_RETRANSMISSIONS = 3;

    /**
     * Path for the file to be restored
     */
    private String path;

    /**
     * File to be restored
     */
    private File file;

    /**
     * Restore protocol pool
     */
    private ThreadPoolExecutor pool;

    /**
     * Restore protocol tcp for enhanced version
     */
    private TCPServer tcp;

    /**
     * RestoreInitiator constructor
     * @param filepath
     */
    public RestoreInitiator(String filepath) {
        this.path = filepath;
        this.pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
    }

    /**
     * Runs Restore protocol for initiator-peer
     * @throws InvalidProtocolExecution
     */
    public void run() throws InvalidProtocolExecution {
        this.file = new File(path);
        if(!this.file.exists())
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.RESTORE, "File not found!");

        String fileId = Globals.generateFileId(file);

        if(Peer.getInstance().isEnhanced())
            startTCPServer();

        Peer.getInstance().getProtocolInfo().startRestore(fileId);

        int n = (int)this.file.length() / (Chunk.MAX_SIZE) + 1;

        CountDownLatch latch = new CountDownLatch(n);

        for(int i = 0; i < n; i++) {
            int chunkNo = i;

            pool.execute(() -> {

                int delay = 1;
                for(int j = 0; j < MAX_RETRANSMISSIONS; j++) {
                    if(!Peer.getInstance().isEnhanced()) {
                        sendGETCHUNK(fileId, chunkNo);
                    } else {
                        sendGETCHUNKENH(fileId, chunkNo, tcp.getPort());
                    }

                    try {
                        TimeUnit.SECONDS.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    delay *= 2;
                    if(Peer.getInstance().getProtocolInfo().hasReceivedChunk(fileId, chunkNo)) {
                        break;
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

        if(Peer.getInstance().isEnhanced())
            closeTCPServer();


        if(Peer.getInstance().getProtocolInfo().getChunksRecieved(fileId) < n) {
            System.out.println("Couldn't restore " + this.file.getName());
            return;
        }

        Peer.getInstance().getFileManager().createFolders(Peer.getInstance().getRestoreFolder());
        FileOutputStream out;
        try {
            out = new FileOutputStream(Peer.getInstance().getRestoreFolder() + this.file.getName());
            for(int i = 0; i < n; i++) {
                out.write(Peer.getInstance().getProtocolInfo().getChunkData(fileId, i));
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        Peer.getInstance().getProtocolInfo().endRestore(fileId);
    }

    /**
     * Starts TCP Server
     */
    private void startTCPServer() {
        this.tcp = new TCPServer();
        new Thread(this.tcp).start();
    }

    /**
     * Closes TCP Server
     */
    private void closeTCPServer() {
        this.tcp.close();
    }
}
