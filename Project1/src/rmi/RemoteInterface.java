package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    String backup(String filepath, int replicationDegree) throws RemoteException;
    String restore(String filepath) throws RemoteException;
    String delete(String filepath) throws RemoteException;
    String reclaim(int spaceReclaim) throws RemoteException;
    String state() throws RemoteException;
}

