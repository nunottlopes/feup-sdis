package protocol.restore;

import channel.Channel;
import message.Message;
import peer.Peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static globals.Globals.getFileData;

public class Restore {
    String fileId;
    int chunkNo;
    int portTCP;
    InetAddress addressTCP;

    public Restore(Message msg, InetAddress address) {
        if(msg.getType() == Message.MessageType.GETCHUNKENH && !Peer.getInstance().isEnhanced()) {
            System.out.println("-- Incompatible peer version with restore enhancement --");
            return;
        }
        if(msg.getType() == Message.MessageType.GETCHUNK && Peer.getInstance().isEnhanced()) {
            System.out.println("-- Incompatible peer version with vanilla restore --");
            return;
        }

//        System.out.println("\n> " + msg.getType() + " received");
//        System.out.println("- Sender Id = " + msg.getSenderId());
//        System.out.println("- File Id = " + msg.getFileId());
//        System.out.println("- Chunk No = " + msg.getChunkNo());

        this.fileId = msg.getFileId();
        this.chunkNo = msg.getChunkNo();
        if(Peer.getInstance().isEnhanced()) {
            this.portTCP = msg.getPort();
            this.addressTCP = address;
        }

        start(Peer.getInstance().getFileManager().hasChunk(fileId, chunkNo));
    }

    private void start(boolean send) {
        Random r = new Random();
        int delay = r.nextInt(400);
        Peer.getInstance().getExecutor().schedule(() -> {
            if(!Peer.getInstance().getProtocolInfo().isChunkAlreadySent(fileId, chunkNo) && send) {
                String[] args = {
                        Peer.getInstance().getVersion(),
                        Integer.toString(Peer.getInstance().getId()),
                        fileId,
                        Integer.toString(chunkNo)
                };

                File file = new File(Peer.getInstance().getBackupPath(fileId) + chunkNo);
                byte[] body;
                try {
                    body = getFileData(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }

                if(Peer.getInstance().isEnhanced()) {
                    sendTCP(new Message(Message.MessageType.CHUNK, args, body));
                    Peer.getInstance().send(Channel.Type.MDR, new Message(Message.MessageType.CHUNK, args));
                } else {
                    Peer.getInstance().send(Channel.Type.MDR, new Message(Message.MessageType.CHUNK, args, body));
                }

            } else {
                Peer.getInstance().getProtocolInfo().removeChunkFromSent(fileId, chunkNo);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void sendTCP(Message msg) {
        Socket socket;

        try {
            socket = new Socket(addressTCP, portTCP);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(msg);
            out.close();
            socket.close();
        } catch (IOException e) {
//            e.printStackTrace();
            System.out.println("Error sending chunk " + chunkNo + " via TCP");
        }
    }
}
