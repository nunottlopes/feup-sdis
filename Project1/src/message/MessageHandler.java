package message;

import peer.Peer;
import protocol.backup.Backup;
import protocol.delete.Delete;
import protocol.reclaim.Reclaim;
import protocol.restore.Restore;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * MessageHandler class
 */
public class MessageHandler implements Runnable {

    private Message msg;
    InetAddress address;

    /**
     * MessageHandler constructor
     * @param packet
     * @throws InvalidPacketException
     */
    public MessageHandler(DatagramPacket packet) throws InvalidPacketException {
        this.msg = new Message(packet);
        this.address = packet.getAddress();
    }

    /**
     * Handles type of message received
     */
    @Override
    public void run() {
        if(this.msg.getType() == Message.MessageType.PUTCHUNK && msg.getSenderId() != Peer.getInstance().getId()) {
            if(Peer.getInstance().getProtocolInfo().isReclaimProtocol())
                Peer.getInstance().getProtocolInfo().addChunksReceivedWhileReclaim(msg.getFileId(), msg.getChunkNo());
            new Backup(this.msg);
        }
        if(this.msg.getType() == Message.MessageType.STORED && msg.getSenderId() != Peer.getInstance().getId()) {
            Peer.getInstance().getProtocolInfo().incRepDegree(this.msg.getFileId(), this.msg.getChunkNo(), this.msg.getSenderId());
        }
        if(this.msg.getType() == Message.MessageType.DELETE){
            new Delete(this.msg);
        }
        if(this.msg.getType() == Message.MessageType.REMOVED && msg.getSenderId() != Peer.getInstance().getId()){
            new Reclaim(this.msg);
        }
        if(this.msg.getType() == Message.MessageType.GETCHUNK && msg.getSenderId() != Peer.getInstance().getId()){
            new Restore(this.msg, address);
        }
        if(this.msg.getType() == Message.MessageType.GETCHUNKENH && msg.getSenderId() != Peer.getInstance().getId()){
            new Restore(this.msg, address);
        }
        if(this.msg.getType() == Message.MessageType.CHUNK && msg.getSenderId() != Peer.getInstance().getId()){
            Peer.getInstance().getProtocolInfo().chunkSent(this.msg.getFileId(), this.msg.getChunkNo(), this.msg.getBody());
        }
        Peer.getInstance().writePeerToFile();
    }
}
