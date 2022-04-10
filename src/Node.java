public interface Node {
    void receiveMessage(Message message);
    int getPort();
}
