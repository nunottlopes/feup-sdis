package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    void backup(String filepath, int replicationDegree, boolean enhanced) throws RemoteException;
    void restore(String filepath, boolean enhanced) throws RemoteException;
    void delete(String filepath) throws RemoteException;
    void reclaim(long spaceReclaim) throws RemoteException;
    String state() throws RemoteException;
}

