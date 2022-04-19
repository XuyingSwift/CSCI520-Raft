import java.io.IOException;
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
                //start a thread to handle receiving the message
                MessagePasser passer = new MessagePasser(socket, node);
                passer.start();
            } catch (IOException ioException) {
                System.out.println(Colors.ANSI_PURPLE + "* Closing server socket..." + Colors.ANSI_RESET);
            }
        }
    }
}
