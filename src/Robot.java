import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.*;

// we need socket programming
public class Robot {
    private String name;
    private int targetNode;
    private HashMap<Integer, RemoteNode> remoteNodes;
    public static final String BLOCK_RIGHT = "block with right hand", BLOCK_LEFT = "block with left hand",
             LOST = "lost", PUNCH_RIGHT = "punch with right hand", PUNCH_LEFT = "punch left with hand",
            BLOCKED = "blocked";
            ;
    private String state;

    public Robot (String name, int targetNode, HashMap<Integer, RemoteNode> remoteNodes) {
        this.name = name;
        this.state = null;
        this.targetNode = targetNode;
        this.remoteNodes = remoteNodes;
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
        try {
            Socket socket = new Socket(this.remoteNodes.get(this.targetNode).getAddress(), this.remoteNodes.get(this.targetNode).getPort());
            System.out.println(this.name + "made connection with " + this.targetNode);
            PrintStream socketOut = new PrintStream(socket.getOutputStream());
            BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // to send out the punch action you can only send out once a second
            socketOut.println(action);
            socketOut.println();

            for (String resp = socketIn.readLine(); resp != null; resp = socketIn.readLine()) {
                String reply = resp;
                System.out.println("returning message from the node"  + resp);
                checkStates(reply);
            }
            socketIn.close();
            socketOut.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void checkStates(String reply) {
        Random rand = new Random();
        int int_random = rand.nextInt(100);
        // replay is punch with left
        if (reply.equals(PUNCH_LEFT)) {
            if (this.state != BLOCK_RIGHT) {
                // here we check the 10% chance
                // KO. robot is knocked out
                if (int_random < 10) {
                    this.state = LOST;
                    System.out.println(this.name + " is Lost");
                    System.out.println("GAME OVER");
                    System.out.println("Do you want to restart a game?");
                    // if yes, then restart the game
                    displayMenu();
                }
            }else {
                this.state = BLOCKED;
                sendMessage(BLOCKED);
            }
        }

        if (reply.equals(PUNCH_RIGHT)) {
            // replay is punch with right hand
            if (this.state != BLOCK_LEFT) {
                if (int_random < 10) {
                    this.state = LOST;
                    System.out.println(this.name + " is Lost");
                    System.out.println("GAME OVER");
                    System.out.println("Do you want to restart a game?");
                    displayMenu();
                    // if yes, then restart the game
                }
            }else {
                this.state = BLOCKED;
                sendMessage(BLOCKED);
            }
        }

        if (reply.equals(BLOCKED)) {
            //  make the robot sleep for 3 second.
        }
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
