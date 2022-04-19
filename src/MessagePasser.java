import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashMap;

public class MessagePasser extends Thread{
    private Socket socket;
    volatile private RaftNode node;

    public MessagePasser(Socket socket, RaftNode node) {
        this.socket = socket;
        this.node = node;
    }

    public void run() {
        System.out.println(Colors.ANSI_PURPLE + "* Another node connected..." + Colors.ANSI_RESET);

        try {
            PrintStream socketOut = new PrintStream(socket.getOutputStream());
            BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String msg = socketIn.readLine();
            String messageJson = null;

            while (msg != null && msg.length() > 0) {
                if (messageJson == null) messageJson = msg;
                else messageJson += msg;

                msg = socketIn.readLine();
            }

            Gson gson = new Gson();
            Message message = gson.fromJson(messageJson, Message.class);

            //hacky code here
            if (message.getType().equals(RaftNode.COMMAND)) {
                JsonObject jsonPayload = new JsonParser().parse(message.getPayload()).getAsJsonObject();
                if (jsonPayload.get(RaftNode.COMMAND).getAsString().equals(Robot.START)) {
                    JsonObject addressInfo = new JsonObject();
                    addressInfo.addProperty("address", socket.getInetAddress().toString());
                    addressInfo.addProperty("port", jsonPayload.get("myPort").getAsString());

                    jsonPayload.add("addressInfo", addressInfo);
                    message.setPayload(jsonPayload.toString());
                }
            }
            //end

            node.receiveMessage(message);

            System.out.println(Colors.ANSI_PURPLE + "MessagePasser (" + Thread.currentThread().getName() + "): Added " + message.getType() + " message [" + message.getGuid() + "] from " + message.getSender() + " to queue, waiting for response..." + Colors.ANSI_RESET);
            while (!node.getMessageReplies().containsKey(message.getGuid())) {

            }

            String response = node.getMessageReplies().get(message.getGuid());
            node.getMessageReplies().remove(message.getGuid());
            System.out.println(Colors.ANSI_PURPLE + "MessagePasser (" + Thread.currentThread().getName() + "): " + message.getType() + " message [" + message.getGuid() + "] from " + message.getSender() + " was processed, response: " + response + Colors.ANSI_RESET);

            socketOut.println(response);
            socketOut.flush();

            socketIn.close();
            socketOut.close();
            socket.close();
        } catch (IOException e) {
            System.out.println(Colors.ANSI_RED + "WARNING MessagePasser (" + Thread.currentThread().getName() + "): Communication failed" + Colors.ANSI_RESET);
            //e.printStackTrace();
        }
    }
}
