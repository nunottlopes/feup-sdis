package protocol.delete;

import message.Message;
import peer.FileManager;
import peer.Peer;

/**
 * Delete class
 */
public class Delete {

    /**
     * Message that has information for delete protocol
     */
    private Message msg;

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

        this.msg = msg;
        this.fm = Peer.getInstance().getFileManager();

        if(this.fm.hasStoredChunks(msg.getFileId())){
            removeFiles();
        }
        else{
            this.fm.removeBackedupChunks(msg.getFileId());
        }
    }

    /**
     * Removes backedup files
     */
    private void removeFiles(){
        String path = Peer.getInstance().getBackupPath(msg.getFileId());
        if(this.fm.removeFileFolder(path, true)){
            this.fm.removeStoredChunks(msg.getFileId());
            Peer.getInstance().getProtocolInfo().removeChunksRepDegree(msg.getFileId());
        }
        fm.removeFolderIfEmpty(Peer.getInstance().getBackupFolder());
    }
}
