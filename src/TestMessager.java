public class TestMessager implements TestMessaging{
    private int i = 0;

    public String sendMessage(String clientMessage) {
        System.out.println("Received message: " + clientMessage);
        i++;
        return "Echo " + i + ": " + clientMessage;
    }
}
