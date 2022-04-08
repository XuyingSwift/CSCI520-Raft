import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class Client {
    public boolean sendMessage(String address, int port, Message message) {
        System.out.println(Colors.ANSI_PURPLE + "*");
        System.out.println("* Sending message to " + address + ":" + port);

        boolean success = true;
        try {
            Socket socket = new Socket(address, port);
            System.out.println("* Connection made");
            System.out.println("*" + Colors.ANSI_RESET);

            PrintStream socketOut = new PrintStream(socket.getOutputStream());
            BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Gson gson = new Gson();
            String messageJson = gson.toJson(message);
            //System.out.println(messageJson);
            socketOut.println(messageJson);
            socketOut.println();

            String resp = socketIn.readLine();
            while(resp != null) {
                //System.out.println(resp);
                if (resp.equals(MessagePasser.fail)) success = false;
                resp = socketIn.readLine();
            }

            socketIn.close();
            socketOut.close();
            socket.close();
        } catch (IOException e) {
            System.out.println(Colors.ANSI_RESET);
            e.printStackTrace();
        }

        return success;
    }
}
