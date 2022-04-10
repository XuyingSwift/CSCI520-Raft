import java.util.UUID;

public class Message {
    private String type, payload;
    private UUID guid;

    public Message(String type, String payload) {
        this.guid = UUID.randomUUID();
        this.type = type;
        this.payload = payload;
    }

    public UUID getGuid() { return guid; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
}
