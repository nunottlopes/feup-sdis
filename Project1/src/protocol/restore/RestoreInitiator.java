package protocol.restore;

import globals.Globals;
import protocol.InvalidProtocolExecution;

import java.io.File;

public class RestoreInitiator {

    private String path;
    private File file;

    public RestoreInitiator(String filepath) {
        this.path = filepath;
    }

    public void run() throws InvalidProtocolExecution {
        this.file = new File(path);
        if(!this.file.exists())
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.RESTORE, "File not found!");

        String fileId = Globals.generateFileId(file);
    }
}
