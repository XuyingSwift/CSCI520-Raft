import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ForceTimeout extends Thread{
    private volatile boolean timeoutForced;

    public ForceTimeout() {
        timeoutForced = false;
    }

    public void run() {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("Enter 'T' at any time to force a timeout");

            try {
                String action = input.readLine();
                if (action.equals("T")) {
                    timeoutForced = true;
                }

                while (timeoutForced) {
                    Thread.onSpinWait();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isTimeoutForced() { return timeoutForced; }
    public void reset() { timeoutForced = false; }
}
