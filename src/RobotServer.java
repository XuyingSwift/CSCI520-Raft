import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class RobotServer extends Thread {
    private ServerSocket server;
    private boolean running;
    volatile private RaftNode node;

    public RobotServer(int port) {
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        running = false;
    }

    public void stopServer() {
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        running = false;
        this.interrupt();
    }

    public void run() {
        running = true;

        while(running) {
            try {
                Socket socket = server.accept();
                System.out.println(Colors.ANSI_PURPLE + "* Another node connected..." + Colors.ANSI_RESET);
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

                JsonObject messagePayload = new JsonParser().parse(message.getPayload()).getAsJsonObject();
                if (messagePayload.get(RaftNode.COMMAND).getAsString().equals(StateMachine.LOST)) {
                    System.out.println(Colors.ANSI_GREEN + "You were knocked out!!!" + Colors.ANSI_RESET);
                }

                socketOut.println("ACK");
                socketOut.flush();

                socketIn.close();
                socketOut.close();
                socket.close();
            } catch (IOException ioException) {
                System.out.println(Colors.ANSI_PURPLE + "* Closing server socket..." + Colors.ANSI_RESET);
            }
        }
    }
}
