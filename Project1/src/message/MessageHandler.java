package message;

import peer.Peer;
import protocol.backup.Backup;
import protocol.delete.Delete;
import protocol.reclaim.Reclaim;

import java.net.DatagramPacket;

public class MessageHandler implements Runnable {

    private Message msg;

    public MessageHandler(DatagramPacket packet) throws InvalidPacketException {
        this.msg = new Message(packet);

    }

    @Override
    public void run() {
        if(this.msg.getType() == Message.MessageType.PUTCHUNK && msg.getSenderId() != Peer.getInstance().getId()) {
            new Backup(this.msg);
        }
        if(this.msg.getType() == Message.MessageType.STORED && msg.getSenderId() != Peer.getInstance().getId()) {
            Peer.getInstance().getProtocolInfo().incRepDegree(this.msg.getFileId(), this.msg.getChunkNo(), this.msg.getSenderId());
        }
        if(this.msg.getType() == Message.MessageType.DELETE){
            new Delete(this.msg);
        }
        if(this.msg.getType() == Message.MessageType.REMOVED){
            new Reclaim(this.msg);
        }
        Peer.getInstance().writePeerToFile();
    }
}
