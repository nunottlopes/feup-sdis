package protocol.delete;

import message.Message;
import peer.FileManager;

public class Delete {

    private Message msg;
    private FileManager fm;

    public Delete(Message msg) {
        this.msg = msg;
    }
}
