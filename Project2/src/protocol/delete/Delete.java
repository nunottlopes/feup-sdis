package protocol.delete;

import message.Message;
import peer.FileManager;
import peer.Peer;

/**
 * Delete class
 */
public class Delete {

    /**
     * Peer file manager
     */
    private FileManager fm;

    /**
     * Delete constructor
     * @param msg
     */
    public Delete(Message msg) {
//        System.out.println("\n> DELETE received");
//        System.out.println("- Sender Id = " + msg.getSenderId());
//        System.out.println("- File Id = " + msg.getFileId());

        this.fm = Peer.getInstance().getFileManager();

        if(this.fm.hasStoredChunks(msg.getFileId())){
            removeFiles(msg.getFileId());
        }
        else{
            this.fm.removeBackedupChunks(msg.getFileId());
        }
    }

    public Delete(String fileId){
        this.fm = Peer.getInstance().getFileManager();

        if(this.fm.hasStoredChunks(fileId)){
            removeFiles(fileId);
        }
        else{
            this.fm.removeBackedupChunks(fileId);
        }
    }

    /**
     * Removes backedup files
     */
    private void removeFiles(String fileId){
        String path = Peer.getInstance().getBackupPath(fileId);
        if(this.fm.removeFileFolder(path, true)){
            this.fm.removeStoredChunks(fileId);
            Peer.getInstance().getProtocolInfo().removeChunksRepDegree(fileId);
        }
        fm.removeFolderIfEmpty(Peer.getInstance().getBackupFolder());
    }
}
