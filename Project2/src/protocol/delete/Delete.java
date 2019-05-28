package protocol.delete;

import message.Message;
import peer.FileManager;
import peer.Peer;

import static message.SendMessage.sendDELETE;

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
            sendDELETE(msg.getFileId(), Peer.getInstance().getChord().getSuccessor());
            removeFiles(msg.getFileId());
        }
    }

    public Delete(String fileId){
        this.fm = Peer.getInstance().getFileManager();

        if(this.fm.hasStoredChunks(fileId)){
            sendDELETE(fileId, Peer.getInstance().getChord().getSuccessor());
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
}
