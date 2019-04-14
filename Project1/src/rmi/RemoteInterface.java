package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote Interface
 */
public interface RemoteInterface extends Remote {
    /**
     * Backup protocol
     * @param filepath
     * @param replicationDegree
     * @param enhanced
     * @throws RemoteException
     */
    void backup(String filepath, int replicationDegree, boolean enhanced) throws RemoteException;

    /**
     * Restore protocol
     * @param filepath
     * @param enhanced
     * @throws RemoteException
     */
    void restore(String filepath, boolean enhanced) throws RemoteException;

    /**
     * Delete protocol
     * @param filepath
     * @throws RemoteException
     */
    void delete(String filepath) throws RemoteException;

    /**
     * Reclaim protocol
     * @param spaceReclaim
     * @throws RemoteException
     */
    void reclaim(long spaceReclaim) throws RemoteException;

    /**
     * Peer state
     * @return
     * @throws RemoteException
     */
    String state() throws RemoteException;
}

