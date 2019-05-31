package protocol.delete;


import chord.Chord;
import globals.Globals;
import peer.Chunk;
import peer.Peer;
import protocol.InvalidProtocolExecution;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static globals.Globals.getFileData;
import static message.SendMessage.sendDELETE;
import static message.SendMessage.sendPUTCHUNK;

/**
 * DeleteInitiator class
 */
public class DeleteInitiator {

    /**
     * Time interval between each DELETE message
     */
    private static final int TIME_INTERVAL = 500;

    /**
     * File path to be deleted
     */
    private String path;

    /**
     * File id
     */
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

        int number_of_chunks = (int) file.length() / (Chunk.MAX_SIZE) + 1;


        Peer.getInstance().getExecutor().schedule(()->{
            for(int n = 0; n < number_of_chunks; n++){
                String name = fileId + n;
                int hash = Peer.getInstance().getChord().hash(name);

                String[] message = Peer.getInstance().getChord().sendLookup(hash, true);

                if(message != null){
                    try {
                        InetAddress address = InetAddress.getByName(message[3]);
                        if (!message[3].equals(Peer.getInstance().getChord().getChordAddress())){
                            sendDELETE(fileId, address);
                        } else{
                            new Delete(fileId);
                        }
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                } else{
                    n--;
                }
            }

        }, 0, TimeUnit.MILLISECONDS);
    }
}
