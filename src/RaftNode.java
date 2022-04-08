import com.google.gson.Gson;

import java.util.HashMap;

public class RaftNode implements Node{
    private final String append = "APPEND", requestVote = "REQ_VOTE";
    private int port, id;
    private Client client;
    HashMap<Integer, RemoteNode> remoteNodes;

    public RaftNode(int id, int port, HashMap<Integer, RemoteNode> remoteNodes) {
        this.id = id;
        this.port = port;
        client = new Client();
        this.remoteNodes = remoteNodes;
    }

    public int getPort() { return port; }

    public void sendAppendEntries(int dest) {
        Gson gson = new Gson();
        String payload = gson.toJson("foo" + id);
        Message message = new Message(append, payload);
        boolean result = client.sendMessage(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), message);
    }

    public void sendRequestVote(int dest) {
        Gson gson = new Gson();
        String payload = gson.toJson("bar" + id);
        Message message = new Message(requestVote, payload);
        boolean result = client.sendMessage(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), message);
    }

    private boolean receiveAppendEntries(String dummy) {
        System.out.println("append_entries: " + dummy);
        return true;
    }

    private boolean receiveRequestVote(String dummy) {
        System.out.println("request_vote: " + dummy);
        return true;
    }

    public Boolean receiveMessage(Message message) {
        boolean retVal = false;
        Gson gson = new Gson();

        if (message.getType().equals(append)) {
            String payload = gson.fromJson(message.getPayload(), String.class);
            retVal = receiveAppendEntries(payload);
        }
        else if (message.getType().equals(requestVote)) {
            String payload = gson.fromJson(message.getPayload(), String.class);
            retVal = receiveRequestVote(payload);
        }
        else {
            System.out.println(Colors.ANSI_RED + "***ERROR: UNSUPPORTED MESSAGE TYPE " + message.getType() + "***" + Colors.ANSI_RESET);
        }

        return retVal;
    }
}
