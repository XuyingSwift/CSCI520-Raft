import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class Client extends Thread{
    String address;
    int port;
    Message message;
    RaftNode node;

    public Client(String address, int port, Message message, RaftNode node) {
        this.address = address;
        this.port = port;
        this.message = message;
        this.node = node;
    }

    public void run() {
        String response = sendMessage();
        System.out.println("CLIENT THREAD: Message " + message.getGuid() + " to " + message.getDestination() + ": " + response);

        Gson gson = new Gson();
        JsonObject responseJson = gson.fromJson(response, JsonObject.class);

        if (message.getType().equals(RaftNode.REQ_VOTE) && responseJson.get("result").getAsBoolean()) {
            node.addVote(message.getTerm());
        }
        else if (message.getType().equals(RaftNode.APPEND) && responseJson.get("result").getAsBoolean()) {
            node.increaseNextIndex(message.getDestination(), responseJson.get("newNextIndex").getAsInt());
        }
        else if (message.getType().equals(RaftNode.APPEND) && !responseJson.get("result").getAsBoolean()) {
            if (responseJson.get("reason").getAsString().equals("log_inconsistency")) {
                node.decrementNextIndex(message.getDestination());
            }
            else if (responseJson.get("reason").getAsString().equals("old_term")) {
                //TODO: turn node into a follower
            }
        }
    }

    private String sendMessage() {
        System.out.println(Colors.ANSI_PURPLE + "*");
        System.out.println("* Sending message to " + address + ":" + port);

        String response = null;
        try {
            Socket socket = new Socket(address, port);
            System.out.println("* Connection made");
            System.out.println("*" + Colors.ANSI_RESET);

            PrintStream socketOut = new PrintStream(socket.getOutputStream());
            BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Gson gson = new Gson();
            String messageJson = gson.toJson(message);
            socketOut.println(messageJson);
            socketOut.println();

            String msg = socketIn.readLine();
            while(msg != null && msg.length() > 0) {
                if (response == null) response = msg;
                else response += msg;

                msg = socketIn.readLine();
            }

            socketIn.close();
            socketOut.close();
            socket.close();
        } catch (IOException e) {
            System.out.println(Colors.ANSI_RED + "WARNING: Could not communicate with node " + message.getDestination() + Colors.ANSI_RESET);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("result", false);
            responseJson.addProperty("reason", "exception");
            response = responseJson.getAsString();
            //System.out.println(Colors.ANSI_RESET);
            //e.printStackTrace();
        }

        return response;
    }
}
