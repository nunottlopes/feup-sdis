package protocol.backup;

import message.Message;
import peer.Chunk;
import peer.FileManager;
import peer.Peer;


import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static message.SendMessages.sendSTORED;

public class Backup {

    private Message msg;
    private FileManager fm;
    private String path;
    private Chunk chunk;

    public Backup(Message msg) {

//        System.out.println("\n> PUTCHUNK received");
//        System.out.println("- Sender Id = " + msg.getSenderId());
//        System.out.println("- File Id = " + msg.getFileId());
//        System.out.println("- Chunk No = " + msg.getChunkNo());

        this.msg = msg;
        this.fm = Peer.getInstance().getFileManager();

        if (!this.fm.hasChunk(msg.getFileId(), msg.getChunkNo())) {
            path = Peer.getInstance().getBackupPath(msg.getFileId());
            //this.fm.createFolders(path);
            start();
        } else {
            chunk = this.fm.getChunk(msg.getFileId(), msg.getChunkNo());
            sendSTORED(chunk.getFileId(), chunk.getChunkNo());
        }
    }

    private void start() {
        chunk = new Chunk(this.msg.getFileId(), this.msg.getChunkNo(), this.msg.getReplicationDeg(), this.msg.getBody());

        if(Peer.getInstance().isEnhanced()) {
            Random r = new Random();
            int delay = r.nextInt(400);
            Peer.getInstance().getExecutor().schedule(() -> {
                if (Peer.getInstance().getProtocolInfo().getChunkRepDegree(chunk.getFileId(), chunk.getChunkNo()) < chunk.getRepDegree()) {
                    if (saveChunk()) {
                        sendSTORED(chunk.getFileId(), chunk.getChunkNo());
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
        } else {
            if (saveChunk()) {
                Random r = new Random();
                int delay = r.nextInt(400);
                Peer.getInstance().getExecutor().schedule(() -> sendSTORED(chunk.getFileId(), chunk.getChunkNo()), delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private boolean saveChunk() {
        boolean success;
        try {
            success = this.fm.saveChunkFile(Integer.toString(chunk.getChunkNo()), path, chunk.getData());
        } catch (IOException e) {
            System.out.println("Error storing chunk");
            return false;
        }

        if(success) {
            this.fm.addChunk(chunk);
        } else {
            return false;
        }
        return true;
    }
}
