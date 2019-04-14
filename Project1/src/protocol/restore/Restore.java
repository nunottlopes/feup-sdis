package protocol.restore;

import channel.Channel;
import message.Message;
import peer.Peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static globals.Globals.getFileData;

public class Restore {
    String fileId;
    int chunkNo;

    public Restore(Message msg) {
//        System.out.println("\n> GETCHUNK received");
//        System.out.println("- Sender Id = " + msg.getSenderId());
//        System.out.println("- File Id = " + msg.getFileId());
//        System.out.println("- Chunk No = " + msg.getChunkNo());

        this.fileId = msg.getFileId();
        this.chunkNo = msg.getChunkNo();

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
                Message msg = new Message(Message.MessageType.CHUNK, args, body);

                if(Peer.getInstance().isEnhanced()) {
                    sendTCP(msg);
                } else {
                    Peer.getInstance().send(Channel.Type.MDR, msg);
                }

            } else {
                Peer.getInstance().getProtocolInfo().removeChunkFromSent(fileId, chunkNo);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void sendTCP(Message msg) {
//        String hostName = request.getTCPHost();
//        int portNumber = request.getTCPPort();
//
//        Socket serverSocket;
//
//        try {
//            serverSocket = new Socket(hostName, portNumber);
//            Log.log("Connected to TCPServer");
//            ObjectOutputStream oos = new ObjectOutputStream(serverSocket.getOutputStream());
//            oos.writeObject(msgToSend);
//            oos.close();
//            serverSocket.close();
//        } catch (IOException e) {
//            Log.logError("Couldn't send CHUNK via TCP");
//        }
//
//        Log.logWarning("S TCP: " + request.toString());
    }
}
