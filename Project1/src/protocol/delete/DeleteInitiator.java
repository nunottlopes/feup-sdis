package protocol.delete;


import channel.Channel;
import globals.Globals;
import message.Message;
import peer.Peer;
import protocol.InvalidProtocolExecution;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DeleteInitiator {

    public static final int MAX_DELETE_MESSAGES = 5;
    private static final int TIME_INTERVAL = 500;

    private String path;
    private String fileId;

    private CountDownLatch latch;

    public DeleteInitiator(String path){
        this.path = path;
    }

    public void run() throws InvalidProtocolExecution {
        File file = new File(path);
        if(!file.exists())
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.DELETE, "File not found!");

        fileId = Globals.generateFileId(file);

        String[] args = {
                Peer.getInstance().getVersion(),
                Integer.toString(Peer.getInstance().getId()),
                fileId
        };

        Message msg = new Message(Message.MessageType.DELETE, args);

        latch = new CountDownLatch(MAX_DELETE_MESSAGES);

        for (int i = 0 ; i < MAX_DELETE_MESSAGES; i++){
            Peer.getInstance().getExecutor().schedule(()->sendDELETE(msg), TIME_INTERVAL*i, TimeUnit.MILLISECONDS);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendDELETE(Message msg){
        Peer.getInstance().send(Channel.Type.MC, msg);
        latch.countDown();
    }
}
