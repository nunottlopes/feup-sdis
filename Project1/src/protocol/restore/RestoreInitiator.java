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

public class RestoreInitiator {

    private static final int AWAIT_TIME = 2000;
    private static final int MAX_TRIES  = 3;

    private String path;
    private File file;

    private ThreadPoolExecutor pool;

    public RestoreInitiator(String filepath) {
        this.path = filepath;
        this.pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
    }

    public void run() throws InvalidProtocolExecution {
        this.file = new File(path);
        if(!this.file.exists())
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.RESTORE, "File not found!");

        String fileId = Globals.generateFileId(file);

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
                Peer.getInstance().send(Channel.Type.MC, msg);

                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        boolean restored = false;
        for(int i = 0; i < MAX_TRIES; i++) {
            try {
                Thread.sleep(AWAIT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(Peer.getInstance().getProtocolInfo().getChunksRecieved(fileId) >= n) {
                restored = true;
                break;
            }
        }

        if(!restored) {
            System.out.println("Couldn't restore " + this.file.getName());
            return;
        }

        Peer.getInstance().getFileManager().createFolders(Peer.getInstance().getRestorePath());
        FileOutputStream out = null;
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
}
