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

public class BackupInitiator {

    public static final int MAX_NUM_CHUNKS = 1000000;
    public static final int MAX_RETRANSMISSIONS = 5;

    private ThreadPoolExecutor pool;

    private String path;
    private int repDegree;
    private String fileId;
    private File file;
    private ArrayList<Chunk> chunks;

    public BackupInitiator(String path, int repDegree) {
        this.path = path;
        this.repDegree = repDegree;
        this.chunks = new ArrayList<>();
        this.pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(200);
    }

    public BackupInitiator(String path, int repDegree, ArrayList<Chunk> chunks){
        // Chunks only have one chunk
        this.path = path;
        this.repDegree = repDegree;
        this.chunks = chunks;
        this.fileId = chunks.get(0).getFileId();
        this.pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(200);
    }

    public void run() throws InvalidProtocolExecution {

        if(chunks.isEmpty()){
            file = new File(path);

            byte[] data;

            try {
                data = getFileData(file);
            } catch (FileNotFoundException e) {
                throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.BACKUP, "File not found!");
            }

            fileId = Globals.generateFileId(file);

            chunks = splitIntoChunks(data);
        }

        sendPUTCHUNKS();

//        if(!validBackup(repDegree, chunks.size())) return;
//
//        Peer.getInstance().getProtocolInfo().startBackup(fileId);
//
//        CountDownLatch latch = new CountDownLatch(chunks.size());
//
//        for(Chunk c : chunks) {
//            pool.execute(() -> {
//                int delay = 1;
//
//                String[] args = {
//                        Peer.getInstance().getVersion(),
//                        Integer.toString(Peer.getInstance().getId()),
//                        fileId,
//                        Integer.toString(c.getChunkNo()),
//                        Integer.toString(c.getRepDegree())
//                };
//                Message msg = new Message(Message.MessageType.PUTCHUNK, args, c.getData());
//
//
//                ProtocolInfo status = Peer.getInstance().getProtocolInfo();
//
//                for(int i = 0; i < MAX_RETRANSMISSIONS; i++) {
//                    Peer.getInstance().send(Channel.Type.MDB, msg);
//
//                    try {
//                        TimeUnit.SECONDS.sleep(delay);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    delay *= 2;
//                    int currentRepDegree = status.getChunkRepDegree(c.getFileId(), c.getChunkNo());
//                    if(currentRepDegree >= c.getRepDegree()) break;
//                }
//
//                latch.countDown();
//            });
//        }
//
//        try {
//            latch.await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//
//        Peer.getInstance().getProtocolInfo().endBackup(fileId, this.repDegree, this.path);

    }

    public void sendPUTCHUNKS() throws InvalidProtocolExecution{

        if(!validBackup(repDegree, chunks.size())) return;

        Peer.getInstance().getProtocolInfo().startBackup(fileId);

        CountDownLatch latch = new CountDownLatch(chunks.size());

        for(Chunk c : chunks) {
            pool.execute(() -> {
                int delay = 1;

                String[] args = {
                        Peer.getInstance().getVersion(),
                        Integer.toString(Peer.getInstance().getId()),
                        fileId,
                        Integer.toString(c.getChunkNo()),
                        Integer.toString(c.getRepDegree())
                };
                Message msg = new Message(Message.MessageType.PUTCHUNK, args, c.getData());


                ProtocolInfo status = Peer.getInstance().getProtocolInfo();

                for(int i = 0; i < MAX_RETRANSMISSIONS; i++) {
                    Peer.getInstance().send(Channel.Type.MDB, msg);

                    if(i != MAX_RETRANSMISSIONS-1){
                        try {
                            TimeUnit.SECONDS.sleep(delay);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        delay *= 2;
                        int currentRepDegree = status.getChunkRepDegree(c.getFileId(), c.getChunkNo());
                        if(currentRepDegree >= c.getRepDegree()) break;
                    }
                    else{
                        //TODO: PQ NÃO ENTRA AQUI
                        System.out.println(status.getChunkRepDegree(c.getFileId(), c.getChunkNo()));
                        System.out.println(c.getRepDegree());
                    }

                    // TODO: Impedir que um chunk que fez RECLAIM 0 volte a guardar os chunks que eliminou
                    // TODO: É suposto guardar no STATE os chunks que ele faz backup através do space reclaim
//                    try {
//                        TimeUnit.SECONDS.sleep(delay);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    delay *= 2;
//                    int currentRepDegree = status.getChunkRepDegree(c.getFileId(), c.getChunkNo());
//                    if(currentRepDegree >= c.getRepDegree()) break;
                }

                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        Peer.getInstance().getProtocolInfo().endBackup(fileId, this.repDegree, this.path);
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
