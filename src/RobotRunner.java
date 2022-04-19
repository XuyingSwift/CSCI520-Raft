import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class RobotRunner {
    public static void main(String[] args) {
        //config string format: "0 0 127.0.0.1 5000 1 127.0.0.1 5001 2 127.0.0.1 5002", ...
        int id = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        HashMap<Integer, RemoteNode> remoteNodes = buildRemoteList(args);
        RobotServer robotServer = new RobotServer(port);

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        String name = null;
        System.out.println("Please enter a name for your robot: ");
        try {
            name = input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Robot robot = new Robot(name, id, remoteNodes, port);
        robotServer.start();
        int selection = -1;

        while (selection != 0) {
            selection = robot.displayMenu();

            if (selection != 0) {
                robot.sendAction(selection);
            }
            else {
                System.out.println("Goodbye");
            }
        }
    }

    private static HashMap<Integer, RemoteNode> buildRemoteList(String[] config) {
        HashMap<Integer, RemoteNode> remotes = new HashMap<>();

        int idx = 2;
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
