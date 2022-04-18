import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class RobotRunner {
    public static void main(String[] args, String[] robotArgs) {
        //config string format: "0 0 127.0.0.1 5000 1 127.0.0.1 5001 2 127.0.0.1 5002", ...
        int id = Integer.parseInt(args[0]);
        HashMap<Integer, RemoteNode> remoteNodes = buildRemoteList(args);

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

        String name = null;
        System.out.println("Please enter a name for your robot: ");
        try {
            name = input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Robot robot = new Robot(name, id, remoteNodes);
        int selection = robot.displayMenu();
        robot.sendAction(selection);
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
