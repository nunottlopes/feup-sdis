package message;

import peer.Peer;
import protocol.backup.Backup;
import protocol.delete.Delete;
import protocol.restore.Restore;

import java.net.InetAddress;

/**
 * MessageHandler class
 */
public class MessageHandler implements Runnable {

    /**
     * Message
     */
    private Message msg;

    /**
     * Inet Address from datagram packet
     */
    InetAddress address;

    /**
     * MessageHandler constructor
     * @throws InvalidPacketException
     */
    public MessageHandler(Message message, InetAddress address) throws InvalidPacketException {
        this.msg = message;
        this.address = address;
    }

    /**
     * Handles type of message received
     */
    @Override
    public void run() {
        if(this.msg.getType() == Message.MessageType.PUTCHUNK && msg.getSenderId() != Peer.getInstance().getId()) {
            new Backup(this.msg, address);
        }
        else if(this.msg.getType() == Message.MessageType.STORED && msg.getSenderId() != Peer.getInstance().getId()) {
            Peer.getInstance().getProtocolInfo().addPeerSavedChunk(this.msg.getFileId(), this.msg.getChunkNo(), this.address, this.msg.getSenderId());
        }
        else if(this.msg.getType() == Message.MessageType.DELETE){
            new Delete(this.msg);
        }
        else if(this.msg.getType() == Message.MessageType.GETCHUNK && msg.getSenderId() != Peer.getInstance().getId()){
            new Restore(this.msg, address);
        }
        else if(this.msg.getType() == Message.MessageType.CHUNK && msg.getSenderId() != Peer.getInstance().getId()){
            Peer.getInstance().getProtocolInfo().addReceivedChunk(this.msg.getFileId(), this.msg.getChunkNo(), this.msg.getBody());
        }
        Peer.getInstance().writePeerToFile();
    }
}
