import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class MessagePasser extends Thread{
    private Socket socket;
    private Node node;
    public static String success = "TRUE", fail = "FALSE";

    public MessagePasser(Socket socket, Node node) {
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

            //System.out.println(messageJson);
            Gson gson = new Gson();
            Message message = gson.fromJson(messageJson, Message.class);
            Boolean result = node.receiveMessage(message);

            socketOut.println(result ? success : fail);
            socketOut.flush();

            socketIn.close();
            socketOut.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
