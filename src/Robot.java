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
            WIN = "win", LOST = "lost", START = "start", PUNCH_RIGHT = "punch with right hand", PUNCH_LEFT = "punch left with hand" ;
    private String state;


    public Robot (String name, int targetNode, HashMap<Integer, RemoteNode> remoteNodes) {
        this.name = name;
        this.state = null;
        this.targetNode = targetNode;
        this.remoteNodes = remoteNodes;
    }

    public void sendAction(int selection) {
        switch (selection) {
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
            String command = action;
            socketOut.println(command);
            socketOut.println();

            for (String resp = socketIn.readLine(); resp != null; resp = socketIn.readLine()) {
                System.out.println("returning message from the node");
            }

            socketIn.close();
            socketOut.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
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

    public void miss() {
        System.out.println(this.name + " missed");
    }

    public void hit() {
        System.out.println(this.name + " hit successfully");
    }

}
