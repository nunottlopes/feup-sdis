package protocol.delete;

import message.Message;
import peer.FileManager;
import peer.Peer;

import java.util.concurrent.ThreadPoolExecutor;


/**
 * Delete class
 */
public class Delete {

    /**
     * Peer file manager
     */
    private FileManager fm;

    private ThreadPoolExecutor pool;

    /**
     * Delete constructor
     * @param msg
     */
    public Delete(Message msg) {
        this(msg.getFileId());
    }

    public Delete(String fileId){
        System.out.println("\n> DELETE received");
        // System.out.println("- Sender Id = " + msg.getSenderId());
        // System.out.println("- File Id = " + msg.getFileId());
        System.out.println("- File Id = " + fileId);

        this.fm = Peer.getInstance().getFileManager();

        if(this.fm.hasStoredChunks(fileId)){
            removeFiles(fileId);
        }
    }

    /**
     * Removes backedup files
     */
    private void removeFiles(String fileId){
        String path = Peer.getInstance().getBackupPath(fileId);
        if(this.fm.removeFileFolder(path, true)){
            this.fm.removeStoredChunks(fileId);
        }
        fm.removeFolderIfEmpty(Peer.getInstance().getBackupFolder());
    }

//    private void sendDeleteToSuccessors(String fileId, int repDegree, int hash){
//        for (int n = 0; n < repDegree; n++){
//            String[] message = Peer.getInstance().getChord().sendLookup(hash, true);
//
//            if(message != null){
//                try {
//                    InetAddress address = InetAddress.getByName(message[3]);
//                    if (!message[3].equals(Peer.getInstance().getChord().getChordAddress())){
//                        sendDELETE(fileId, address);
//                    }
//                } catch (UnknownHostException e) {
//                    e.printStackTrace();
//                }
//
//                hash = Math.floorMod(Integer.parseInt(message[2]) + 1, Peer.getInstance().getMaxChordPeers());
//            }
//        }
//    }
}
