import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.*;

// we need socket programming
public class Robot {
    private String name;
    private int targetNode, id;
    private HashMap<Integer, RemoteNode> remoteNodes;
    public static final String BLOCK_RIGHT = "block with right hand", BLOCK_LEFT = "block with left hand",
            LOST = "lost", PUNCH_RIGHT = "punch with right hand", PUNCH_LEFT = "punch left with hand",
            BLOCKED = "blocked";
    private String state;

    public Robot (String name, int id, HashMap<Integer, RemoteNode> remoteNodes) {
        this.name = name;
        this.state = null;
        this.remoteNodes = remoteNodes;
        this.id = id;
        this.targetNode = 0;
    }

    public void sendAction(int selection) {
        switch (selection) {
            case 0:
                System.out.println("GAME OVER");
                System.exit(0);
            case 1:
                punchLeft();
                break;
            case 2:
                punchRight();
                break;
            case 3:
                blockLeft();
                break;
            case 4:
                blockRight();
                break;
            default:
                System.out.println("Make a choice!");
        }
    }

    public boolean sendMessage(String action) {
        System.out.println( this.name + " Sending message to node " + this.targetNode);
        String response = null;
        JsonObject jsonObject = null;

        while (jsonObject == null || jsonObject.get(RaftNode.TYPE).equals(RaftNode.REDIRECT)) {
            try {
                Socket socket = new Socket(this.remoteNodes.get(this.targetNode).getAddress(), this.remoteNodes.get(this.targetNode).getPort());
                System.out.println(this.name + "made connection with " + this.targetNode);
                PrintStream socketOut = new PrintStream(socket.getOutputStream());
                BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // to send out the punch action you can only send out once a second
                // send out Json part
                Gson gson = new Gson();
                HashMap<String, String> actionInfo = new HashMap<>();
                actionInfo.put(RaftNode.COMMAND, action);
                String actionMessage = gson.toJson(actionInfo);
                Message message = new Message(this.id, RaftNode.COMMAND, actionMessage);
                String messageJson = gson.toJson(message);
                socketOut.println(messageJson);
                socketOut.println();
                String msg = socketIn.readLine();
                while(msg != null && msg.length() > 0) {
                    if (response == null) response = msg;
                    else response += msg;

                    msg = socketIn.readLine();
                }
                jsonObject = new JsonParser().parse(response).getAsJsonObject();
                if (jsonObject.get(RaftNode.TYPE).equals(RaftNode.REDIRECT)) {
                    this.targetNode = jsonObject.get(RaftNode.CURRENT_LEADER).getAsInt();
                }
                socketIn.close();
                socketOut.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return true;
    }


    public void punchRight() {
        System.out.println(this.name + " punched with right hand ");
        // method to send to leader
        sendMessage(PUNCH_RIGHT);
    }

    public void punchLeft() {
        System.out.println(this.name + " punched with left hand ");
        sendMessage(PUNCH_LEFT);
    }

    public void blockRight() {
        System.out.println(this.name + " blocked with right hand");
        sendMessage(BLOCK_LEFT);
    }

    public void blockLeft() {
        System.out.println(this.name + " blocked with left hand");
        sendMessage(BLOCK_LEFT);
    }


    public int displayMenu() {
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
}
