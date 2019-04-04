package message;

import java.net.DatagramPacket;

public class MessageHandler implements Runnable {

    private Message msg;

    public MessageHandler(DatagramPacket packet) throws InvalidPacketException {
        this.msg = new Message(packet);

    }

    @Override
    public void run() {

    }
}
