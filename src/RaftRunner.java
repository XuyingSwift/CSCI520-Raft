import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Scanner;

public class RaftRunner {
    public final static int SLOW_FACTOR = 15;

    public static void main(String[] args, String[] robotArgs) {
        //config string format: "0 0 127.0.0.1 5000 1 127.0.0.1 5001 2 127.0.0.1 5002", ...
        int id = Integer.parseInt(args[0]);
        HashMap<Integer, RemoteNode> remoteNodes = buildRemoteList(args);
        int port = remoteNodes.get(id).getPort();

        RaftNode node = new RaftNode(id, port, remoteNodes);

        //TODO: restore node state
        // if the file is not null, then I read in that restore, delete that file, restart from scratch
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Press <enter> to continue...");
        try {
            input.readLine();
            System.out.print("Starting in ");
            for (int i = 7; i >= 1; i--) {
                System.out.print(i + "..");
                Thread.sleep(1000);
            }
            System.out.print(System.lineSeparator());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        //start a thread to listen for messages on the port
        Server server = new Server(node);
        server.start();
        System.out.println(Colors.ANSI_PURPLE + "* Started server on port " + node.getPort() + " to listen for messages" + Colors.ANSI_RESET);

        try {
            node.run();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        int idx = 1;
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

    private static HashMap<Integer, RemoteNode> buildRemoteRobotList(String[] config) {
        HashMap<Integer, RemoteNode> remotes = new HashMap<>();

        int idx = 1;
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
