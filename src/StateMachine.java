import java.util.Random;

public class StateMachine {
    public static final String BLOCK_RIGHT = "block with right hand", BLOCK_LEFT = "block with left hand",
            LOST = "lost", PUNCH_RIGHT = "punch with right hand", PUNCH_LEFT = "punch left with hand",
            BLOCKED = "blocked", START="start";
    private String state;

    public StateMachine() {
        this.state = START;
    }

    public String getState() {
        return state;
    }

    public void checkStates(String command) {
        Random rand = new Random();
        int int_random = rand.nextInt(100);
        // replay is punch with left
        if (command.equals(PUNCH_LEFT)) {
            if (this.state != BLOCK_RIGHT) {
                // here we check the 10% chance
                // KO. robot is knocked out
                if (int_random < 10) {
                    this.state = LOST;
                }
            }else {
                this.state = BLOCKED;
            }
        }

        if (command.equals(PUNCH_RIGHT)) {
            // replay is punch with right hand
            if (this.state != BLOCK_LEFT) {
                if (int_random < 10) {
                    this.state = LOST;
                }
            }else {
                this.state = BLOCKED;
            }
        }
    }
}



