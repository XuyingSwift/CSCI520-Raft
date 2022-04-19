import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class RobotClient {
    private String address;
    private int port;
    private Message message;

    public RobotClient(String address, int port, Message message) {
        this.address = address;
        this.port = port;
        this.message = message;
    }

    public void sendMessage() {
        String response = null;
        try {
            Socket socket = new Socket(address, port);
            System.out.println(Colors.ANSI_GREEN + "Client (" + Thread.currentThread().getName() + "): Connection made to " + address + ":" + port + Colors.ANSI_RESET);

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
            response = "fail";
            //System.out.println(Colors.ANSI_RESET);
            //e.printStackTrace();
        }

        System.out.println(response);
    }
}
