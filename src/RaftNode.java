import com.google.gson.Gson;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class RaftNode implements Node{
    private final String append = "APPEND", requestVote = "REQ_VOTE";
    private int port, id;
    private Client client;
    HashMap<Integer, RemoteNode> remoteNodes;
    volatile Queue<Message> messageQueue;
    volatile HashMap<UUID, Boolean> messageResponses;

    public RaftNode(int id, int port, HashMap<Integer, RemoteNode> remoteNodes) {
        this.id = id;
        this.port = port;
        client = new Client();
        messageQueue = new LinkedList<>();
        messageResponses = new HashMap<>();
        this.remoteNodes = remoteNodes;
    }

    public void run() {
        //testing block
        if (id == 0) {
            for (int i = 0; i < 3; i++) {
                if (i != id) {
                    sendAppendEntries(i);
                    sendRequestVote(i);
                }
            }
        }
        //end testing block

        while (true) {
            while (!messageQueue.isEmpty()) {
                Message curMessage = messageQueue.poll();
                System.out.println("Processing message " + curMessage.getGuid() + " (" + curMessage.getType() + ")");
                processMessage(curMessage);
            }

        }
    }

    public int getPort() { return port; }

    public void sendAppendEntries(int dest) {
        Gson gson = new Gson();
        String payload = gson.toJson("foo" + id);
        Message message = new Message(append, payload);
        boolean result = client.sendMessage(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), message);
        System.out.println("Append entries message " + message.getGuid() + ": " + result);
    }

    public void sendRequestVote(int dest) {
        Gson gson = new Gson();
        String payload = gson.toJson("bar" + id);
        Message message = new Message(requestVote, payload);
        boolean result = client.sendMessage(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), message);
        System.out.println("Request vote message " + message.getGuid() + ": " + result);
    }

    private boolean receiveAppendEntries(String dummy) {
        System.out.println("append_entries: " + dummy);
        return true;
    }

    private boolean receiveRequestVote(String dummy) {
        System.out.println("request_vote: " + dummy);
        return true;
    }

    public void receiveMessage(Message message) {
        messageQueue.add(message);
    }

    public HashMap<UUID, Boolean> getMessageResponses() {
        return messageResponses;
    }

    private void processMessage(Message message) {
        boolean retVal = false;
        Gson gson = new Gson();

        if (message.getType().equals(append)) {
            String payload = gson.fromJson(message.getPayload(), String.class);
            retVal = receiveAppendEntries(payload);
            messageResponses.put(message.getGuid(), retVal);
        }
        else if (message.getType().equals(requestVote)) {
            String payload = gson.fromJson(message.getPayload(), String.class);
            retVal = receiveRequestVote(payload);
            messageResponses.put(message.getGuid(), retVal);
        }
        else {
            System.out.println(Colors.ANSI_RED + "***ERROR: UNSUPPORTED MESSAGE TYPE " + message.getType() + "***" + Colors.ANSI_RESET);
        }
    }
}
