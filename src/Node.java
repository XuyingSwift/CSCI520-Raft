public interface Node {
    Boolean receiveMessage(Message message);
    int getPort();
}
