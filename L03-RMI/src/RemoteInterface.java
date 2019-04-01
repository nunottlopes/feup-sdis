import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    String register(String plate, String owner) throws RemoteException;
    String lookup(String plate) throws RemoteException;
}
