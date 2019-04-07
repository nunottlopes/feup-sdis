package message;

import protocol.backup.Backup;

import java.net.DatagramPacket;

public class MessageHandler implements Runnable {

    private Message msg;

    public MessageHandler(DatagramPacket packet) throws InvalidPacketException {
        this.msg = new Message(packet);

    }

    @Override
    public void run() {
        if(this.msg.getType() == Message.MessageType.PUTCHUNK) {
            new Backup(this.msg);
        }
    }
}
