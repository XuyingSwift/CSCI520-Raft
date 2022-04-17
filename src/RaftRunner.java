import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Scanner;

public class RaftRunner {
    public final static int SLOW_FACTOR = 15;

    public static void main(String[] args) {
        //config string format: "0 0 127.0.0.1 5000 1 127.0.0.1 5001 2 127.0.0.1 5002", ...
        int id = Integer.parseInt(args[0]);
        HashMap<Integer, RemoteNode> remoteNodes = buildRemoteList(args);
        int port = remoteNodes.get(id).getPort();

        String robotName;
        Scanner input = new Scanner(System.in);

        System.out.println("Let's Start our game!");
        System.out.println("-------------------------");
        System.out.println("Enter an robot name");
        robotName = input.toString();

        Robot robot = new Robot(robotName, id, remoteNodes);
        int selection = displayMenu();
        robot.sendAction(selection);

        RaftNode node = new RaftNode(id, port, remoteNodes);
        //TODO: restore node state
        // if the file is not null, then I read in that restore, delete that file, restart from scratch
        BufferedReader inputB = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Press <enter> to continue...");
        try {
            inputB.readLine();
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
            inputB.readLine();
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


    private static int displayMenu() {
        int selection;
        Scanner input = new Scanner(System.in);

        System.out.println("Enter 1 for punch with left hand ");
        System.out.println("Enter 2 for punch with right hand ");
        System.out.println("Enter 3 for block with left hand ");
        System.out.println("Enter 4 for block with right hand ");
        System.out.println("Enter 0 for quiting the game");

        selection = input.nextInt();
        return selection;
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
}
