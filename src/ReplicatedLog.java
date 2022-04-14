public class ReplicatedLog {
    private int index;
    private int term;
    private String command;

    public ReplicatedLog(int term, String command) {
        this.term = term;
        this.command = command;
    }

    public int getTerm() {
        return term;
    }

    public String getCommand() {
        return command;
    }

}
