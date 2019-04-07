package protocol;

public class InvalidProtocolExecution extends Exception {

    public enum Protocol {BACKUP, DELETE, RECLAIM, RESTORE};

    public InvalidProtocolExecution(Protocol protocol, String msg) {
        super(protocol + " PROTOCOL EXCEPTION: " + msg);
    }
}
