public class Message {
    private String type, payload;

    public Message(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() { return type; }
    public String getPayload() { return payload; }
}
