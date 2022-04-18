import java.util.UUID;

public class Message {
    private String type, payload, command;
    private int sender, destination, term;
    private UUID guid;

    public Message(int sender, int destination, int term, String type, String payload) {
        this.guid = UUID.randomUUID();
        this.sender = sender;
        this.destination = destination;
        this.term = term;
        this.type = type;
        this.payload = payload;
    }

    public Message(int sender, String type, String payload) {
        this.type = type;
        this.payload = payload;
        this.guid = UUID.randomUUID();
        this.sender = sender;
    }

    public UUID getGuid() { return guid; }
    public int getSender() { return sender; }
    public int getDestination() { return destination; }
    public int getTerm() { return term; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
}
