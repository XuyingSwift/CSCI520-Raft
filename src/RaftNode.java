import com.google.gson.Gson;

import java.util.*;

/* TODOS:
 * Track node state (follower, candidate, or leader)
 * Have an election timeout
 * Keep a log
 * Track state values: currentTerm, votedFor, commitIndex, lastApplied
 * For leader: track state values: nextIndex[], matchIndex[]
 */

public class RaftNode {
    public static final String APPEND = "APPEND", REQ_VOTE = "REQ_VOTE";
    private final String FOLLOW = "FOLLOWER", CANDID = "CANDIDATE", LEADER = "LEADER";
    private final int HEARTBEAT_TIME = 50, MAJORITY;
    private int port, id;
    HashMap<Integer, RemoteNode> remoteNodes;

    volatile private Integer voteCount, votedFor, term, currentLeader;
    volatile private String state;

    volatile Queue<Message> messageQueue; //queue of incoming messages to process
    volatile HashMap<UUID, Boolean> messageReplies; //dictionary of this node's replies to incoming messages

    public RaftNode(int id, int port, HashMap<Integer, RemoteNode> remoteNodes) {
        this.id = id;
        this.port = port;
        term = 0;
        voteCount = 0;
        messageQueue = new LinkedList<>();
        messageReplies = new HashMap<>();
        this.remoteNodes = remoteNodes;
        state = FOLLOW;
        MAJORITY = (int) Math.ceil(remoteNodes.size() / 2.0);
        currentLeader = null;
    }

    public void run() {
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

        boolean once = true;
        while (true) {
            if (state.equals(CANDID) && voteCount >= MAJORITY) {
                becomeLeader();
            }
            //send heartbeat

            while (!messageQueue.isEmpty()) {
                processMessage();
            }

            if (id == 0 && once) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                startElection();
                once = false;
            }
            //if timer is expired, start election
        }
    }

    public int getPort() { return port; }

    public synchronized void addVote(int voteTerm) {
        if (voteTerm == this.term) voteCount++;
    }

    private void startElection() {
        state = CANDID;
        term++;
        voteCount = 1; //start with vote for self
        votedFor = id;
        //reset election timer

        //send a requestVote to all other nodes
        for (Integer remoteNode : remoteNodes.keySet()) {
            if (remoteNode.equals(id)) continue;
            sendRequestVote(remoteNode);
        }

        //TODO: finish election logic
    }

    private void becomeLeader() {
        if (state.equals(CANDID)) {
            state = LEADER;
            currentLeader = id;
            System.out.println("MAIN THREAD: became the leader!!");

            //TODO: send empty AppendEntries messages to all other nodes
            for (Integer remoteNode : remoteNodes.keySet()) {
                if (remoteNode.equals(id)) continue;
                sendAppendEntries(remoteNode);
            }
        }
        else {
            System.out.println("MAIN THREAD: was trying to become leader but found a new leader");
        }
    }

    private void sendAppendEntries(int dest) {
        Gson gson = new Gson();
        String payload = gson.toJson("foo" + id);
        Message message = new Message(id, dest, term, APPEND, payload);
        Client client = new Client(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), message, this);

        System.out.println("MAIN THREAD: Starting append entries message to node " + dest + ": " + message.getGuid());
        client.start();
    }

    //TODO: implement sendRequestVote
    private void sendRequestVote(int dest) {
        Gson gson = new Gson();
        String payload = gson.toJson("bar" + id);
        Message message = new Message(id, dest, term, REQ_VOTE, payload);
        Client client = new Client(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), message, this);

        System.out.println("MAIN THREAD: Starting request vote message to node " + dest + ": " + message.getGuid());
        client.start();
    }

    private boolean receiveAppendEntries(String dummy) {
        //reset election timer
        System.out.println("MAIN THREAD: append_entries: " + dummy);
        return true;
    }

    //TODO: implement receiveRequestVote
    private boolean receiveRequestVote(String dummy) {
        //reset election timer
        System.out.println("MAIN THREAD: request_vote: " + dummy);
        return true;
    }

    public synchronized void receiveMessage(Message message) {
        if (message == null) {
            System.out.println(Colors.ANSI_RED + "WARNING (" + Thread.currentThread().getName() + "): putting NULL message on queue" + Colors.ANSI_RESET);
        }

        if (message.getTerm() > term && message.getType().equals(APPEND)) {
            currentLeader = message.getSender();
        }

        if (message.getTerm() > term) {
            state = FOLLOW;
            term = message.getTerm();
            votedFor = null;
        }

        messageQueue.add(message);
    }

    public HashMap<UUID, Boolean> getMessageReplies() {
        return messageReplies;
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
            messageReplies.put(curMessage.getGuid(), retVal);
        }
        else if (curMessage.getType().equals(REQ_VOTE)) {
            String payload = gson.fromJson(curMessage.getPayload(), String.class);
            retVal = receiveRequestVote(payload);
            messageReplies.put(curMessage.getGuid(), retVal);
        }
        else {
            System.out.println(Colors.ANSI_RED + "***ERROR: UNSUPPORTED MESSAGE TYPE " + curMessage.getType() + "***" + Colors.ANSI_RESET);
        }
    }
}
