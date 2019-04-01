package protocol.backup;

import peer.Peer;

public class Backup implements Runnable {

    private Peer parent_peer;
    private String request_message;

    public Backup(Peer parent_peer, String request_message){
        this.parent_peer = parent_peer;
        this.request_message = request_message;

        System.out.println("Starting Backup Service");
    }


    @Override
    public void run() {

    }
}
