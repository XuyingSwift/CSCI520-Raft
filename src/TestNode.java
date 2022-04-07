import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class TestNode {
    public static void main(String[] args) {
        TestMessaging tester = new TestMessager();
        int myPort = Integer.parseInt(args[0]), otherPort = Integer.parseInt(args[1]);

        try {
            TestMessaging stub = (TestMessaging) UnicastRemoteObject.exportObject(tester, 0);
            Registry registry = LocateRegistry.createRegistry(myPort);
            registry.rebind("TestMessaging", stub);

            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Press <enter> to continue...");
            try {
                input.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            runTest(otherPort);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

        System.out.println("Goodbye");
    }

    private static void runTest(int remoteRegPort) throws RemoteException, NotBoundException {
        Registry remoteRegistry = LocateRegistry.getRegistry("127.0.0.1", remoteRegPort);
        TestMessaging remoteNode = (TestMessaging) remoteRegistry.lookup("TestMessaging");

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        boolean exit = false;
        while (!exit) {
            System.out.print("Enter a message, 'q' to quit: ");
            try {
                String userInput = input.readLine();

                if (userInput.equals("q")) {
                    exit = true;
                } else {
                    String response = remoteNode.sendMessage(userInput);
                    System.out.println(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
