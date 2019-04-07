package protocol.backup;

import channel.Channel;
import message.Message;
import peer.Chunk;
import peer.FileManager;
import peer.Peer;

import java.io.IOException;
import java.net.DatagramPacket;

public class Backup {

    private Message msg;
    private FileManager fm;
    private String path;
    private Chunk chunk;

    public Backup(Message msg){
        System.out.println("PUTCHUNK received");
        this.msg = msg;
        this.fm = Peer.getInstance().getFileManager();


        if(msg.getSenderId() == Peer.getInstance().getId()) {
            return;
        }

        if (!this.fm.hasChunk(msg.getFileId(), msg.getChunkNo())) {
            path = Peer.getInstance().getBackupPath(msg.getFileId());
            this.fm.createFolder(path);
            start();
        }

    }

    private void start() {
        chunk = new Chunk(this.msg.getFileId(), this.msg.getChunkNo(), this.msg.getReplicationDeg(), this.msg.getBody());
        if(saveChunk()) {
            sendSTORED();
        }

    }

    private boolean saveChunk() {
        boolean success;
        try {
            success = fm.saveFile(Integer.toString(chunk.getChunkNo()), path, chunk.getData());
        } catch (IOException e) {
            System.out.println("Error storing chunk");
            return false;
        }

        if(success) {
            fm.addChunk(chunk);
        } else {
            return false;
        }
        return true;
    }

    private synchronized void sendSTORED() {
        Peer p = Peer.getInstance();
        String[] args = {
                p.getVersion(),
                Integer.toString(p.getId()),
                chunk.getFileId(),
                Integer.toString(chunk.getChunkNo())
        };

        Message msg = new Message(Message.MessageType.STORED, args);

        Channel c = p.getChannel(Channel.Type.MC);
        try {
            p.getSocket().send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, c.getAddress(), c.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
