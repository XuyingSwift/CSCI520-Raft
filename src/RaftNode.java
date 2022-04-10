import com.google.gson.Gson;

import java.util.*;

public class RaftNode {
    /* TODOS:
     * Track node state (follower, candidate, or leader)
     * Have an election timeout
     * Keep a log
     * Track state values: currentTerm, votedFor, commitIndex, lastApplied
     * For leader: track state values: nextIndex[], matchIndex[]
     */

    private final String append = "APPEND", requestVote = "REQ_VOTE";
    private int port, id;
    HashMap<Integer, RemoteNode> remoteNodes;
    volatile Queue<Message> messageQueue;
    volatile HashMap<UUID, Boolean> messageResponses;

    public RaftNode(int id, int port, HashMap<Integer, RemoteNode> remoteNodes) {
        this.id = id;
        this.port = port;
        messageQueue = new LinkedList<>();
        messageResponses = new HashMap<>();
        this.remoteNodes = remoteNodes;
    }

    public void run() {
        int curIndex = 0;

        /*
         * WHILE TRUE:
         * If leader then send heartbeat (empty append entries)
         * Process any messages (while message queue is not empty...)
         *      Reset election timer
         * If timeout is expired, convert to candidate & start election
         *      increment current term
         *      vote for self
         *      reset election timer
         *      send requestVote to everyone else
         */

        while (true) {
            //------TESTING CODE-------
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            curIndex++;
            int destNode = curIndex % 3;
            if (curIndex != id) {
                Random rand = new Random();
                if (rand.nextBoolean()) {
                    sendAppendEntries(destNode);
                }
                else {
                    sendRequestVote(destNode);
                }
            }
            //------END TESTING CODE-------

            while (!messageQueue.isEmpty()) {
                processMessage();
            }
        }
    }

    public int getPort() { return port; }

    private void sendAppendEntries(int dest) {
        Gson gson = new Gson();
        String payload = gson.toJson("foo" + id);
        Message message = new Message(id, append, payload);
        Client client = new Client(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), dest, message);

        System.out.println("MAIN THREAD: Starting append entries message to node " + dest + ": " + message.getGuid());
        client.start();
    }

    private void sendRequestVote(int dest) {
        Gson gson = new Gson();
        String payload = gson.toJson("bar" + id);
        Message message = new Message(id, requestVote, payload);
        Client client = new Client(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), dest, message);

        System.out.println("MAIN THREAD: Starting request vote message to node " + dest + ": " + message.getGuid());
        client.start();
    }

    private boolean receiveAppendEntries(String dummy) {
        System.out.println("MAIN THREAD: append_entries: " + dummy);
        return true;
    }

    private boolean receiveRequestVote(String dummy) {
        System.out.println("MAIN THREAD: request_vote: " + dummy);
        return true;
    }

    public synchronized void receiveMessage(Message message) {
        if (message == null) {
            System.out.println(Colors.ANSI_RED + "WARNING (" + Thread.currentThread().getName() + "): putting NULL message on queue" + Colors.ANSI_RESET);
        }
        messageQueue.add(message);
    }

    public HashMap<UUID, Boolean> getMessageResponses() {
        return messageResponses;
    }

    private synchronized void processMessage() {
        boolean retVal = false;
        Gson gson = new Gson();

        Message curMessage = messageQueue.poll();
        if (curMessage == null) {
            System.out.println("MAIN THREAD: Pulled NULL message off of queue");
        }
        System.out.println("MAIN THREAD: Processing message " + curMessage.getGuid() + " from node " + curMessage.getSender() + " (" + curMessage.getType() + ")");

        if (curMessage.getType().equals(append)) {
            String payload = gson.fromJson(curMessage.getPayload(), String.class);
            retVal = receiveAppendEntries(payload);
            messageResponses.put(curMessage.getGuid(), retVal);
        }
        else if (curMessage.getType().equals(requestVote)) {
            String payload = gson.fromJson(curMessage.getPayload(), String.class);
            retVal = receiveRequestVote(payload);
            messageResponses.put(curMessage.getGuid(), retVal);
        }
        else {
            System.out.println(Colors.ANSI_RED + "***ERROR: UNSUPPORTED MESSAGE TYPE " + curMessage.getType() + "***" + Colors.ANSI_RESET);
        }
    }
}
