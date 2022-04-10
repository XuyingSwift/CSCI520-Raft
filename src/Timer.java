import java.util.Random;

public class Timer extends Thread{
    private int timeout;
    private final int MIN_TIMEOUT = 150, MAX_TIMEOUT = 300;

    public Timer() {
        reset();
    }

    public void reset() {
        Random rand = new Random();
        timeout = rand.nextInt((MAX_TIMEOUT - MIN_TIMEOUT) + 1) + MIN_TIMEOUT;
    }
}
