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

public class RaftNode implements Node{
    private final String APPEND = "APPEND", REQ_VOTE = "REQ_VOTE";
    private final int HEARTBEAT_TIME = 50;
    private int port, id;
    private Client client;
    HashMap<Integer, RemoteNode> remoteNodes;
    volatile Queue<Message> messageQueue;
    volatile HashMap<UUID, Boolean> messageResponses;

    public RaftNode(int id, int port, HashMap<Integer, RemoteNode> remoteNodes) {
        this.id = id;
        this.port = port;
        term = 0;
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

    private void startElection() {
        term++;
        int votes = 1; //start with vote for self
        //reset election timer

        //send a requestVote to all other nodes
        for (Integer remoteNode : remoteNodes.keySet()) {
            if (remoteNode.equals(id)) continue;
            sendRequestVote(remoteNode);
        }

        //TODO: finish election logic
    }

    private void sendAppendEntries(int dest) {
        Gson gson = new Gson();
        String payload = gson.toJson("foo" + id);
        Message message = new Message(id, APPEND, payload);
        Client client = new Client(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), dest, message);

        System.out.println("MAIN THREAD: Starting append entries message to node " + dest + ": " + message.getGuid());
        client.start();
    }

    //TODO: implement sendRequestVote
    private void sendRequestVote(int dest) {
        Gson gson = new Gson();
        String payload = gson.toJson("bar" + id);
        Message message = new Message(id, REQ_VOTE, payload);
        Client client = new Client(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), dest, message);

        System.out.println("MAIN THREAD: Starting request vote message to node " + dest + ": " + message.getGuid());
        client.start();
    }

    private boolean receiveAppendEntries(String dummy) {
        System.out.println("MAIN THREAD: append_entries: " + dummy);
        return true;
    }

    //TODO: implement receiveRequestVote
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

        if (curMessage.getType().equals(APPEND)) {
            String payload = gson.fromJson(curMessage.getPayload(), String.class);
            retVal = receiveAppendEntries(payload);
            messageResponses.put(curMessage.getGuid(), retVal);
        }
        else if (curMessage.getType().equals(REQ_VOTE)) {
            String payload = gson.fromJson(curMessage.getPayload(), String.class);
            retVal = receiveRequestVote(payload);
            messageResponses.put(curMessage.getGuid(), retVal);
        }
        else {
            System.out.println(Colors.ANSI_RED + "***ERROR: UNSUPPORTED MESSAGE TYPE " + curMessage.getType() + "***" + Colors.ANSI_RESET);
        }
    }
}
