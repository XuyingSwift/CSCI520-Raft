public class ReplicatedLog {
    private int index;
    private int term;
    private String command;

    public ReplicatedLog(int index, int term, String command) {
        this.index = index;
        this.term = term;
        this.command = command;
    }

    public int getIndex() {
        return index;
    }

    public int getTerm() {
        return term;
    }

    public String getCommand() {
        return command;
    }

}
