import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class MessagePasser extends Thread{
    private Socket socket;
    volatile private RaftNode node;
    public static String SUCCESS = "TRUE", FAIL = "FALSE";

    public MessagePasser(Socket socket, RaftNode node) {
        this.socket = socket;
        this.node = node;
    }

    public void run() {
        System.out.println(Colors.ANSI_PURPLE + System.lineSeparator() + "*");
        System.out.println("* Another node connected..." + Colors.ANSI_RESET);

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
            node.receiveMessage(message);

            System.out.println("MSG PASSER THREAD: Added message " + message.getGuid() + " from " + message.getSender() + " to queue, waiting for response...");
            while (!node.getMessageReplies().containsKey(message.getGuid())) {

            }

            String response = node.getMessageReplies().get(message.getGuid());
            node.getMessageReplies().remove(message.getGuid());
            System.out.println("MSG PASSER THREAD: Got response for " + message.getGuid() + " from " + message.getSender() + ": " + response);

            socketOut.println(response);
            socketOut.flush();

            socketIn.close();
            socketOut.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
