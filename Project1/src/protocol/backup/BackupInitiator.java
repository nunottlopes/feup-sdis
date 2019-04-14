package protocol.backup;

import channel.Channel;
import globals.Globals;
import message.Message;
import peer.Chunk;
import peer.Peer;
import protocol.InvalidProtocolExecution;
import protocol.ProtocolInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static globals.Globals.getFileData;
import static message.SendMessages.sendPUTCHUNK;

public class BackupInitiator {

    public static final int MAX_NUM_CHUNKS = 1000000;
    public static final int MAX_RETRANSMISSIONS = 5;

    private ThreadPoolExecutor pool;

    private String path;
    private int repDegree;
    private String fileId;
    private File file;
    private Chunk chunk;

    public BackupInitiator(String path, int repDegree) {
        this.path = path;
        this.repDegree = repDegree;
        this.pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
    }

    public BackupInitiator(String path, int repDegree, Chunk chunk){
        this(path, repDegree);
        this.chunk = chunk;
        this.fileId = chunk.getFileId();
    }

    public void run() throws InvalidProtocolExecution {

        file = new File(path);

        byte[] data;

        try {
            data = getFileData(file);
        } catch (FileNotFoundException e) {
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.BACKUP, "File not found!");
        }

        fileId = Globals.generateFileId(file);

        ArrayList<Chunk> chunks = splitIntoChunks(data);

        if(!validBackup(repDegree, chunks.size())) return;

        Peer.getInstance().getProtocolInfo().startBackup(fileId);

        CountDownLatch latch = new CountDownLatch(chunks.size());

        for(Chunk c : chunks) {
            pool.execute(() -> {
                int delay = 1;

                ProtocolInfo status = Peer.getInstance().getProtocolInfo();

                for(int i = 0; i < MAX_RETRANSMISSIONS; i++) {
                    sendPUTCHUNK(fileId, chunk.getChunkNo(), chunk.getRepDegree(), chunk.getData());

                    if(i != MAX_RETRANSMISSIONS-1){
                        try {
                            TimeUnit.SECONDS.sleep(delay);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        delay *= 2;
                        int currentRepDegree = status.getChunkRepDegree(fileId, c.getChunkNo());
                        if(currentRepDegree >= repDegree) break;
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

    public void run_one_chunk() {

        CountDownLatch latch = new CountDownLatch(1);

        pool.execute(() -> {
            int delay = 1;

            ProtocolInfo status = Peer.getInstance().getProtocolInfo();

            for(int i = 0; i < MAX_RETRANSMISSIONS; i++) {
                sendPUTCHUNK(fileId, chunk.getChunkNo(), chunk.getRepDegree(), chunk.getData());

                if(i != MAX_RETRANSMISSIONS-1){
                    try {
                        TimeUnit.SECONDS.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    delay *= 2;
                    int currentRepDegree = status.getChunkRepDegree(chunk.getFileId(), chunk.getChunkNo());
                    if(currentRepDegree >= repDegree) break;
                }
            }

            latch.countDown();

        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private boolean validBackup(int repDegree, int n_chunks) throws InvalidProtocolExecution {
        if(repDegree > 9) {
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.BACKUP, "Max number of replication degree is 9");
        }
        if(n_chunks > MAX_NUM_CHUNKS) {
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.BACKUP,"Backup Error: Max file size is 64GBytes");
        }
        return true;
    }

    private ArrayList<Chunk> splitIntoChunks(byte[] data) {
        ArrayList<Chunk> ret = new ArrayList<>();

        int n = data.length / (Chunk.MAX_SIZE) + 1;

        for(int i = 0; i < n; i++) {

            byte[] chunk_data;

            if(i == n-1) {
                if(data.length % Chunk.MAX_SIZE ==0) {
                    chunk_data= new byte[0];
                } else {
                    chunk_data= Arrays.copyOfRange(data, i*Chunk.MAX_SIZE, i*Chunk.MAX_SIZE + (data.length % Chunk.MAX_SIZE));
                }
            } else {
                chunk_data= Arrays.copyOfRange(data, i*Chunk.MAX_SIZE, i*Chunk.MAX_SIZE + Chunk.MAX_SIZE);
            }
            Chunk chunk=new Chunk(fileId, i, repDegree, chunk_data);
            ret.add(chunk);
        }
        return ret;
    }
}
