import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

// we need socket programming
public class Robot {
    private String name;
    public static final String BLOCK_R = "block right", BLOCK_L = "block left",
            WIN = "win", LOST = "lost", START = "start", PUNCH_RIGHT = "punch right", PUNCH_LEFT = "punch left" ;
    public String state;
    public Robot (String name) {
        this.name = name;
        state = null;
    }

    public void sendAction() {
        int selection = displayMenu();
        switch (selection) {
            case 1:
                punchLeft();
                break;
            case 2:
                punchRight();
                break;
            case 3:
                blockLeft();
                break;
            case 4:
                blockRight();
                break;
            default:
                System.out.println("Make a choice!");
        }
    }

    public void receiveAction(String payload) {
        if (this.state =

        )
    }

    public int displayMenu() {
        int selection;
        Scanner input = new Scanner(System.in);

        System.out.println("Let's Start our game!");
        System.out.println("-------------------------");
        System.out.println("Enter an robot name");
        System.out.println("Enter 1 for punch with left hand ");
        System.out.println("Enter 2 for punch with right hand ");
        System.out.println("Enter 3 for block with left hand ");
        System.out.println("Enter 4 for block with right hand ");
        System.out.println("Enter 0 for quiting the game");

        selection = input.nextInt();
        return selection;
    }

    public void punchRight() {
        System.out.println(this.name + " punched with right hand ");
        // method to send to leader
    }

    public void punchLeft() {
        System.out.println(this.name + " punched with left hand ");
    }

    public void blockRight() {
        System.out.println(this.name + " blocked with right hand");
    }

    public void blockLeft() {
        System.out.println(this.name + " blocked with left hand");
    }

    public void miss() {
        System.out.println(this.name + " missed");
    }

    public void hit() {
        System.out.println(this.name + " hit successfully");
    }

}
