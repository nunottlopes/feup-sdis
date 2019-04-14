package protocol.restore;

import channel.Channel;
import globals.Globals;
import message.Message;
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

public class RestoreInitiator {

    private static final int MAX_RETRANSMISSIONS = 3;

    private String path;
    private File file;

    private ThreadPoolExecutor pool;
    private TCPServer tcp;

    public RestoreInitiator(String filepath) {
        this.path = filepath;
        this.pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
    }

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
                String[] args = {
                        Peer.getInstance().getVersion(),
                        Integer.toString(Peer.getInstance().getId()),
                        fileId,
                        Integer.toString(chunkNo)
                };
                Message msg = new Message(Message.MessageType.GETCHUNK, args);

                int delay = 1;
                for(int j = 0; j < MAX_RETRANSMISSIONS; j++) {
                    Peer.getInstance().send(Channel.Type.MC, msg);

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

        Peer.getInstance().getFileManager().createFolders(Peer.getInstance().getRestorePath());
        FileOutputStream out;
        try {
            out = new FileOutputStream(Peer.getInstance().getRestorePath() + this.file.getName());
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

    private void startTCPServer() {
        this.tcp = new TCPServer();
        new Thread(this.tcp).start();
    }

    private void closeTCPServer() {
        this.tcp.close();
    }
}
