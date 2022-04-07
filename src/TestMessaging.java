import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TestMessaging extends Remote {
    String sendMessage(String clientMessage) throws RemoteException;
}
