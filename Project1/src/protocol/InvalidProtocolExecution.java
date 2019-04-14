package protocol;

/**
 * InvalidProtocolExecution class
 */
public class InvalidProtocolExecution extends Exception {

    public enum Protocol {BACKUP, DELETE, RECLAIM, RESTORE};

    /**
     * InvalidProtocolExecution constructor
     * @param protocol
     * @param msg
     */
    public InvalidProtocolExecution(Protocol protocol, String msg) {
        super(protocol + " PROTOCOL EXCEPTION: " + msg);
    }
}
