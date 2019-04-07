package test;

import rmi.RemoteInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

    public static void main(String[] args) {
        if(args.length < 2 || args.length > 4) {
            System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }

        try {
            String peerAccessPoint = args[0];
            String subProtocol = args[1];
            String filePath;

            Registry registry = LocateRegistry.getRegistry();
            RemoteInterface stub = (RemoteInterface) registry.lookup(peerAccessPoint);

            switch (subProtocol.toLowerCase()) {
                case "backup":
                    if(args.length != 4){
                        System.out.println("Usage: java TestApp <peer_ap> BACKUP <file_path> <replication_degree>");
                        return;
                    }
                    filePath = args[2];
                    int replicationDegree = Integer.parseInt(args[3]);
                    stub.backup(filePath, replicationDegree);
                    break;
                case "restore":
                    if(args.length != 3){
                        System.out.println("Usage: java TestApp <peer_ap> RESTORE <file_path>");
                        return;
                    }
                    filePath = args[2];
                    stub.restore(filePath);
                    break;
                case "delete":
                    if(args.length != 3){
                        System.out.println("Usage: java TestApp <peer_ap> DELETE <file_path>");
                        return;
                    }
                    filePath = args[2];
                    stub.delete(filePath);
                    break;
                case "reclaim":
                    if(args.length != 3){
                        System.out.println("Usage: java TestApp <peer_ap> RECLAIM <disk_space_to_reclaim>");
                        return;
                    }
                    int spaceReclaim = Integer.parseInt(args[2]);
                    stub.reclaim(spaceReclaim);
                    break;
                case "state":
                    if(args.length != 2){
                        System.out.println("Usage: java TestApp <peer_ap> STATE");
                        return;
                    }
                    stub.state();
                    break;
                default:
                    System.out.println("ERROR: <sub_protocol> can only be BACKUP, RESTORE, DELETE, RECLAIM OR STATE");
                    break;
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
