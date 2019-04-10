package message;

import peer.Peer;
import protocol.backup.Backup;

import java.net.DatagramPacket;

public class MessageHandler implements Runnable {

    private Message msg;

    public MessageHandler(DatagramPacket packet) throws InvalidPacketException {
        this.msg = new Message(packet);

    }

    @Override
    public void run() {
        if(this.msg.getType() == Message.MessageType.PUTCHUNK && msg.getSenderId() != Peer.getInstance().getId()) {
//            System.out.print("Received: " + this.msg.getHeaderString());
            new Backup(this.msg);
        }
        if(this.msg.getType() == Message.MessageType.STORED && msg.getSenderId() != Peer.getInstance().getId()) {
//            System.out.print("Received: " + this.msg.getHeaderString());
            Peer.getInstance().getProtocolInfo().incRepDegree(this.msg.getFileId(), this.msg.getChunkNo(), this.msg.getSenderId());
            Peer.getInstance().getFileManager().updateChunkPerceivedRepDegree(msg.getFileId(), msg.getChunkNo(), msg.getSenderId());
        }
    }
}
