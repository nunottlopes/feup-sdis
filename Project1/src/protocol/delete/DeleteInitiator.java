package protocol.delete;


import globals.Globals;
import peer.Peer;
import protocol.InvalidProtocolExecution;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static message.SendMessage.sendDELETE;

/**
 * DeleteInitiator class
 */
public class DeleteInitiator {

    public static final int MAX_DELETE_MESSAGES = 5;
    private static final int TIME_INTERVAL = 500;

    private String path;
    private String fileId;

    /**
     * DeleteInitiator constructor
     * @param path
     */
    public DeleteInitiator(String path){
        this.path = path;
    }

    /**
     * Runs Delete protocol for initiator-peer
     * @throws InvalidProtocolExecution
     */
    public void run() throws InvalidProtocolExecution {
        File file = new File(path);
        if(!file.exists())
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.DELETE, "File not found!");

        fileId = Globals.generateFileId(file);

        CountDownLatch latch = new CountDownLatch(MAX_DELETE_MESSAGES);

        for (int i = 0 ; i < MAX_DELETE_MESSAGES; i++){
            Peer.getInstance().getExecutor().schedule(()->{
                sendDELETE(fileId);
                latch.countDown();
                }, TIME_INTERVAL*i, TimeUnit.MILLISECONDS);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
