import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;

/* TODOS:
 * Track node state (follower, candidate, or leader)
 * Have an election timeout
 * Keep a log
 * Track state values: currentTerm, votedFor, commitIndex, lastApplied
 * For leader: track state values: nextIndex[], matchIndex[]
 */

public class RaftNode {
    public static final String APPEND = "APPEND", REQ_VOTE = "REQ_VOTE", CANDIDATE_ID = "candidateId", CANDIDATE_TERM = "candidateTerm" ;
    private final String FOLLOW = "FOLLOWER", CANDID = "CANDIDATE", LEADER = "LEADER";

    private final int HEARTBEAT_TIME = 50 * RaftRunner.SLOW_FACTOR, MAJORITY;
    private int port, id;

    private HashMap<Integer, RemoteNode> remoteNodes;

    private ArrayList<ReplicatedLog> logs;
    private int[] nextIndex;
    private int[] matchIndex;
    private int commitIndex;
    private int lastApplied;

    volatile private ElectionTimer timer;
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
        timer = new ElectionTimer();
        logs = new ArrayList<>();
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
        timer.start();
        long lastHeartbeat = System.nanoTime();

        while (true) {
            if (state.equals(CANDID) && voteCount >= MAJORITY) {
                becomeLeader();
            }

            if (state.equals(LEADER) && ((System.nanoTime() - lastHeartbeat) / 1000000) >= HEARTBEAT_TIME) {
                sendHeartbeat();
                lastHeartbeat = System.nanoTime();
            }

            while (!messageQueue.isEmpty()) {
                processMessage();
            }

            if (timer.isExpired() && !state.equals(LEADER)) startElection();
        }
    }

    public int getPort() { return port; }

    public synchronized void addVote(int voteTerm) {
        if (voteTerm == this.term) voteCount++;
    }

    private void startElection() {
        // switch to candidate state
        state = CANDID;
        // increment its term
        term++;
        //start with vote for self
        voteCount = 1;
        // set voted for to the candidate id
        votedFor = id;
        // reset the term timer
        timer.reset();

        //send a requestVote to all other nodes
        for (Integer remoteNode : remoteNodes.keySet()) {
            if (remoteNode.equals(id)) continue;
            sendRequestVote(remoteNode);
        }
        // If the RPC succeeds, some time has passed, we have to check the state to see what our options are.
        // If our state is no longer candidate, bail out.
        // If we're still a candidate when the reply is back, we check the term of the reply and compare it
        // to the original term we were on when we sent the request.
        // If the reply's term is higher, we revert to a follower state.
        // This can happen if another candidate won an election while we were collecting votes
    }

    private void becomeLeader() {
        if (state.equals(CANDID)) {
            state = LEADER;
            currentLeader = id;
            System.out.println("MAIN THREAD: became the leader!!");
            sendHeartbeat();
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

    // if the RPC returns a term higher than our own,
    // this peer switches to become a follower
    private void becomeFollower(int savedCurrentTerm) {
        System.out.println("Become follower with term: " + term);
        state = FOLLOW;
        term = savedCurrentTerm;
        votedFor = -1;

        // something with the timer...

    }
    private void sendHeartbeat() {
        for (Integer remoteNode : remoteNodes.keySet()) {
            if (remoteNode.equals(id)) continue;
            sendAppendEntries(remoteNode);
        }
    }

    // TODO: finish this method
    private void sendAppendEntries(int dest) {
        // send the log and leaderId, prelogindex
        //
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
        HashMap<String, String> voteInfo = new HashMap<>();

        // candidate requesting vote
        voteInfo.put(CANDIDATE_ID, String.valueOf(id));
        // candidate’s term
        voteInfo.put(CANDIDATE_TERM, String.valueOf(term));

        //TODO: lastLogIndex
        //TODO: lastLogTerm

        String payload = gson.toJson(voteInfo);
        Message message = new Message(id, dest, term, REQ_VOTE, payload);
        Client client = new Client(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), message, this);

        System.out.println("MAIN THREAD: Starting request vote message to node " + dest + ": " + message.getGuid());
        client.start();
    }

    //TODO: implement receiveRequestVote
    private boolean receiveRequestVote(String payload) {
        boolean grantedVote = false;
        System.out.println("Request Vote: " + "term: " + term + "votedFor: " + votedFor);
        JsonObject jsonObject = new JsonParser().parse(payload).getAsJsonObject();
        // if the sending node's term is higher than my term
        if (jsonObject.get(CANDIDATE_TERM).getAsInt() >= this.term
                && (votedFor == null || votedFor == jsonObject.get(CANDIDATE_ID).getAsInt() )) {
            grantedVote = true;
            votedFor = jsonObject.get(CANDIDATE_ID).getAsInt();
        }

        return grantedVote;
    }

    private boolean receiveAppendEntries(String dummy) {
        System.out.println("MAIN THREAD: append_entries: " + dummy);

        return true;
    }

    public synchronized void receiveMessage(Message message) {
        if (message == null) {
            System.out.println(Colors.ANSI_RED + "WARNING (" + Thread.currentThread().getName() + "): putting NULL message on queue" + Colors.ANSI_RESET);
        }

        if (message.getType().equals(REQ_VOTE) && (message.getTerm() > term || (message.getTerm() >= term && votedFor.equals(message.getSender())))) {
            timer.reset();
        }

        if (message.getTerm() >= term && message.getType().equals(APPEND)) {
            timer.reset();
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
        boolean retVal;
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
