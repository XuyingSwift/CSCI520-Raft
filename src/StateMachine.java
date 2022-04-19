import java.util.Random;

public class StateMachine {
    public static final String BLOCK_RIGHT = "block with right hand", BLOCK_LEFT = "block with left hand",
            LOST = "lost", PUNCH_RIGHT = "punch with right hand", PUNCH_LEFT = "punch left with hand",
            BLOCKED = "blocked", START="start", WIN = "WIN";
    private String state;

    public StateMachine() {
        this.state = START;
    }

    public String getState() {
        return state;
    }

    public void checkStates(String command) {
        if (command.equals(BLOCK_LEFT) || command.equals(BLOCK_RIGHT) || command.equals(LOST)) {
            this.state = command;
        }
    }

    public void checkStates (String command, String opponentState) {
        Random rand = new Random();
        int int_random = rand.nextInt(100);
        // replay is punch with left
        if (command.equals(PUNCH_LEFT)) {
            this.state = PUNCH_LEFT;
            if (!opponentState.equals(BLOCK_RIGHT)) {
                // here we check the 10% chance
                // KO. robot is knocked out
                if (int_random < 30) {
                    this.state = WIN;
                }
            }else {
                this.state = BLOCKED;
            }
        }

        if (command.equals(PUNCH_RIGHT)) {
            // replay is punch with right hand
            this.state = PUNCH_RIGHT;
            if (!opponentState.equals(BLOCK_LEFT)) {
                if (int_random < 30) {
                    this.state = WIN;
                }
            }else {
                this.state = BLOCKED;
            }
        }
    }
}



