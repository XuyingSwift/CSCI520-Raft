import java.util.UUID;

public class Message {
    private String type, payload;
    private int sender;
    private UUID guid;

    public Message(int sender, String type, String payload) {
        this.guid = UUID.randomUUID();
        this.sender = sender;
        this.type = type;
        this.payload = payload;
    }

    public UUID getGuid() { return guid; }
    public int getSender() { return sender; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
}
