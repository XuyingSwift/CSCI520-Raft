import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class RaftRunner {
    public final static int SLOW_FACTOR = 15;

    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]), port = Integer.parseInt(args[1]);

        String config[] = new String[] {"0", "127.0.0.1", "5000", "1", "127.0.0.1", "5001", "2", "127.0.0.1", "5002"};

        RaftNode node = new RaftNode(id, port, buildRemoteList(config));

        //start a thread to listen for messages on the port
        Server server = new Server(node);
        server.start();

        System.out.println(Colors.ANSI_PURPLE + "* ");
        System.out.println("* Started server on port " + node.getPort() + " to listen for messages");
        System.out.println("*" + Colors.ANSI_RESET);

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Press <enter> to continue...");
        try {
            input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        node.run();

        System.out.println("Press <enter> to quit...");
        try {
            input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.stopServer();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(Colors.ANSI_PURPLE + "* Shut down server");
        System.out.println("*" + Colors.ANSI_RESET);
    }

    private static HashMap<Integer, RemoteNode> buildRemoteList(String[] config) {
        HashMap<Integer, RemoteNode> remotes = new HashMap<>();

        int idx = 0;
        while (idx < config.length) {
            int curId = Integer.parseInt(config[idx]);
            idx++;
            String curAddress = config[idx];
            idx++;
            int curPort = Integer.parseInt(config[idx]);
            idx++;

            remotes.put(curId, new RemoteNode(curId, curAddress, curPort));
        }

        return remotes;
    }
}
