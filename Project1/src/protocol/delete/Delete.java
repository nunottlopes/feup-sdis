package protocol.delete;

import message.Message;
import peer.FileManager;
import peer.Peer;

public class Delete {

    private Message msg;
    private FileManager fm;

    public Delete(Message msg) {
        System.out.println("\n> DELETE received");
        System.out.println("- Sender Id = " + msg.getSenderId());
        System.out.println("- File Id = " + msg.getFileId());

        this.msg = msg;
        this.fm = Peer.getInstance().getFileManager();

        if(this.fm.hasStoredChunks(msg.getFileId())){
            removeFiles();
        }
        else{
            this.fm.removeBackedupChunks(msg.getFileId());
        }
    }

    private void removeFiles(){
        String path = Peer.getInstance().getBackupPath(msg.getFileId());
        if(this.fm.removeFileFolder(path)){
            this.fm.removeStoredChunks(msg.getFileId());
            Peer.getInstance().getProtocolInfo().removeChunksRepDegree(msg.getFileId());
        }
    }
}
