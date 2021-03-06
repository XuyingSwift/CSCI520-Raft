import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.*;

public class RaftNode {
    public static final String APPEND = "APPEND", REQ_VOTE = "REQ_VOTE", COMMAND = "COMMAND",
            CANDIDATE_ID = "candidateId", CANDIDATE_TERM = "candidateTerm",
            LEADER_TERM = "leaderTerm", LEADER_ID = "leaderId",
            PREV_LOG_INDEX = "prevLogIndex", PREV_LOG_TERM = "prevLogTerm", ENTRIES = "entries", LEADER_COMMIT = "leaderCommit",
            LAST_LOG_INDEX = "lastLogIndex", LAST_LOG_TERM = "lastLogTerm", CURRENT_LEADER = "current leader";
    private final String FOLLOW = "FOLLOWER", CANDID = "CANDIDATE", LEADER = "LEADER";
    public static final String REDIRECT = "redirect", REACTION = "reaction", TYPE = "type", ROBOT_ID = "robotId";

    private final int HEARTBEAT_TIME = 50 * RaftRunner.SLOW_FACTOR, MAJORITY;
    private int port, id;

    private HashMap<Integer, RemoteNode> remoteNodes;
    private HashMap<Integer, StateMachine> robotStates;
    private HashMap<Integer, String[]> robotAddresses;

    volatile private ArrayList<ReplicatedLog> logs;
    private int lastApplied;
    private ForceTimeout forceTimeout;

    volatile private int[] nextIndex, matchIndex;
    volatile private ElectionTimer timer;
    volatile private Integer voteCount, votedFor, term, commitIndex, currentLeader;
    volatile private String state;

    volatile Queue<Message> messageQueue; //queue of incoming messages to process
    volatile HashMap<UUID, String> messageReplies; //dictionary of this node's replies to incoming messages

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
        nextIndex = new int[remoteNodes.size()];
        matchIndex = new int[remoteNodes.size()];
        commitIndex = -1;
        votedFor = -1;
        lastApplied = -1;
        robotStates = new HashMap<>();
        robotAddresses = new HashMap<>();
        forceTimeout = new ForceTimeout();
    }

    public void run() throws IOException {
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

        forceTimeout.start();
        timer.start();
        long lastHeartbeat = System.nanoTime();

        while (true) {
            if (state.equals(CANDID) && voteCount >= MAJORITY) {
                becomeLeader();
            }

            if (forceTimeout.isTimeoutForced()) {
                forceTimeout.reset();
                startElection();
            }

            if (state.equals(LEADER) && ((System.nanoTime() - lastHeartbeat) / 1000000) >= HEARTBEAT_TIME) {
                sendHeartbeat();
                lastHeartbeat = System.nanoTime();
            }

            while (!messageQueue.isEmpty()) {
                processMessage();
            }

            if (state.equals(LEADER)) {
                int newCommitIndex = findNewCommitIndex();
                if (newCommitIndex > commitIndex) {
                    commitIndex = newCommitIndex;
                    System.out.println(Colors.ANSI_YELLOW + "RaftNode (" + Thread.currentThread().getName() + "): Updated commit index to " + newCommitIndex + Colors.ANSI_RESET);
                }
                else if (newCommitIndex < commitIndex) {
                    System.out.println(Colors.ANSI_RED + "WARNING RaftNode (" + Thread.currentThread().getName() + "): found a smaller commit index of " + newCommitIndex + Colors.ANSI_RESET);
                }
            }

            if (commitIndex > lastApplied) updateStateMachines();

            if (timer.isExpired() && !state.equals(LEADER)) startElection();
        }
    }

    public int getPort() { return port; }

    synchronized public void addVote(int voteTerm) {
        if (voteTerm == this.term) voteCount++;
    }

    synchronized public void addRobotAddress(Integer id, String[] address) {
        if (!robotAddresses.containsKey(id)) robotAddresses.put(id, address);
    }

    private void startElection() {
        // switch to candidate state
        state = CANDID;
        // increment its term
        term++;
        System.out.println(Colors.ANSI_YELLOW + "RaftNode (" + Thread.currentThread().getName() + "): became candidate in term " + term + Colors.ANSI_RESET);
        //start with vote for self
        voteCount = 1;
        // set voted for to the candidate id
        votedFor = id;
        // reset the term timer
        timer.reset();

        try {
            writeToDisk("startElection");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //send a requestVote to all other nodes
        for (Integer remoteNode : remoteNodes.keySet()) {
            if (remoteNode.equals(id)) continue;
            sendRequestVote(remoteNode);
        }
    }

    private void becomeLeader() {
        if (state.equals(CANDID)) {
            state = LEADER;
            currentLeader = id;

            System.out.println(Colors.ANSI_YELLOW + "RaftNode (" + Thread.currentThread().getName() + "): became the leader in term " + term + "!!" + Colors.ANSI_RESET);
            Arrays.fill(nextIndex, logs.size()); //re-initialize next index array
            Arrays.fill(matchIndex, -1); //re-initialize matchIndex array
            //TODO: we could send an empty AppendEntries instead of a heartbeat
            sendHeartbeat();
        }
        else {
            System.out.println(Colors.ANSI_CYAN + "RaftNode (" + Thread.currentThread().getName() + "): was trying to become leader but found a new leader" + Colors.ANSI_RESET);
        }
    }

    private void sendHeartbeat() {
        for (Integer remoteNode : remoteNodes.keySet()) {
            if (remoteNode.equals(id)) continue;
            sendAppendEntries(remoteNode);
        }
    }

    private int findNewCommitIndex() {
        HashMap<Integer, Integer> commitStates = new HashMap<>();

        //for each node's matchIndex value, track how many other nodes have a value
        //at least that large
        for (int i = 0; i < matchIndex.length; i++) {
            if (!commitStates.containsKey(matchIndex[i])) commitStates.put(matchIndex[i], 0);

            for (Integer curState : commitStates.keySet()) {
                if (matchIndex[i] >= curState) {
                    commitStates.put(curState, commitStates.get(curState) + 1);
                }
            }
        }

        //find the maximum commitState (same as a matchIndex value) where a majority of nodes
        //have a commitState that big
        int max = commitIndex;
        for (Integer curState : commitStates.keySet()) {
            if (curState > max && commitStates.get(curState) >= MAJORITY) max = curState;
        }

        return max;
    }

    private void sendAppendEntries(int dest) {
        // send the log and leaderId, prelogindex
        Gson gson = new GsonBuilder().serializeNulls().create();
        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put(LEADER_TERM, term);
        logInfo.put(LEADER_ID, id);
        logInfo.put(PREV_LOG_INDEX, nextIndex[dest] - 1);

        if (nextIndex[dest] > 0) {
            logInfo.put(PREV_LOG_TERM, logs.get(nextIndex[dest] - 1).getTerm());
        }
        else {
            logInfo.put(PREV_LOG_TERM, null);
        }

        logInfo.put(LEADER_COMMIT, commitIndex);

        //if logs last index is greater than or equal to nextIndex for the destination node
        if (logs.size() > nextIndex[dest]) {//send everything from nextIndex[dest] to the end of the log
            List<ReplicatedLog> entriesToSend = logs.subList(nextIndex[dest], logs.size());
            logInfo.put(ENTRIES, gson.toJson(entriesToSend));
        }
        else {
            logInfo.put(ENTRIES, null);
        }

        String payload = gson.toJson(logInfo);

        Message message = new Message(id, dest, term, APPEND, payload);
        Client client = new Client(remoteNodes.get(dest).getAddress(),
                remoteNodes.get(dest).getPort(), message, this);
        
        System.out.println(Colors.ANSI_CYAN + "RaftNode (" + Thread.currentThread().getName() + "): Sending append entries message [" + message.getGuid() + "] to node " + dest + Colors.ANSI_RESET);
        System.out.println(Colors.ANSI_CYAN + "     " + message.getPayload() + Colors.ANSI_RESET);
        client.start();
    }

    private String receiveAppendEntries(String payload) {
        JsonObject jsonObject = new JsonParser().parse(payload).getAsJsonObject();
        JsonObject response = new JsonObject();

        if (jsonObject.get(LEADER_TERM).getAsInt() < term) {            
            response.addProperty("result", false);
            response.addProperty("reason", "old_term");
        }
        //checks if the log is still empty
        else if (jsonObject.get(ENTRIES).isJsonNull()) {
            response.addProperty("result", false);
            response.addProperty("reason", "empty_log");
        }
        //checks if we have a log entry at prevLogIndex (from Leader)
        else if (jsonObject.get(PREV_LOG_INDEX).getAsInt() > logs.size() - 1) {
            response.addProperty("result", false);
            response.addProperty("reason", "log_inconsistency");
        }
        //checks if the log entry at prevLogIndex (from Leader) has a matching term
        else if (jsonObject.get(PREV_LOG_INDEX).getAsInt() >= 0 &&
                logs.get(jsonObject.get(PREV_LOG_INDEX).getAsInt()).getTerm() != jsonObject.get(PREV_LOG_TERM).getAsInt()
        ) {
            response.addProperty("result", false);
            response.addProperty("reason", "log_inconsistency");
        }
        else {
            TypeToken<List<ReplicatedLog>> token = new TypeToken<>() {};
            Gson gson = new Gson();
            ArrayList<ReplicatedLog> newEntries = gson.fromJson(jsonObject.get(ENTRIES).getAsString(), token.getType());

            System.out.println(Colors.ANSI_CYAN + "RaftNode (" + Thread.currentThread().getName() + "): adding log entries " + jsonObject.get(ENTRIES).getAsString() + Colors.ANSI_RESET);
            int index = jsonObject.get(PREV_LOG_INDEX).getAsInt() + 1;

            //remove all old entries
            if (logs.size() > index) {
                logs.subList(index, logs.size()).clear();
            }

            for (ReplicatedLog curEntry : newEntries) {
                logs.add(index, curEntry);
                index++;
            }

            if (jsonObject.get(LEADER_COMMIT).getAsInt() > commitIndex) {
                commitIndex = Math.min(jsonObject.get(LEADER_COMMIT).getAsInt(), logs.size() - 1);
            }

            response.addProperty("result", true);
            response.addProperty("newNextIndex", logs.size());
        }

        try {
            writeToDisk("receiveAppendEntries");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    synchronized public void decrementNextIndex(int destination) {
        synchronized (nextIndex) {
            if (nextIndex[destination] > 0) nextIndex[destination]--;
        }
    }

    synchronized public void increaseNextIndex(int destination, int newNextIndex) {
        synchronized (nextIndex) {
            if (newNextIndex > nextIndex[destination]) {
                nextIndex[destination] = newNextIndex;
                matchIndex[destination] = newNextIndex - 1;
            }
        }
    }

    private void sendRequestVote(int dest) {
        HashMap<String, Object> voteInfo = new HashMap<>();

        // candidate requesting vote
        voteInfo.put(CANDIDATE_ID, String.valueOf(id));
        // candidate???s term
        voteInfo.put(CANDIDATE_TERM, String.valueOf(term));
        //last index of candidate's log
        voteInfo.put(LAST_LOG_INDEX, logs.size() - 1);
        //term of candidates last log entry
        if (logs.size() > 0) {
            voteInfo.put(LAST_LOG_TERM, logs.get(logs.size() - 1).getTerm());
        }
        else {
            voteInfo.put(LAST_LOG_TERM, null);
        }

        Gson gson = new GsonBuilder().serializeNulls().create();
        String payload = gson.toJson(voteInfo);
        Message message = new Message(id, dest, term, REQ_VOTE, payload);
        Client client = new Client(remoteNodes.get(dest).getAddress(), remoteNodes.get(dest).getPort(), message, this);

        System.out.println(Colors.ANSI_CYAN + "RaftNode (" + Thread.currentThread().getName() + "): Sending request vote message [" + message.getGuid() + "] to node " + dest + Colors.ANSI_RESET);
        System.out.println(Colors.ANSI_CYAN + "     " + message.getPayload() + Colors.ANSI_RESET);
        client.start();
    }

    private String receiveRequestVote(String payload) {
        JsonObject response = new JsonObject();
        JsonObject jsonObject = new JsonParser().parse(payload).getAsJsonObject();

        //if the sending node's term is at least as high as my term
        //and either I haven't voted yet, or I already voted for this node,
        //maybe grant vote
        if (jsonObject.get(CANDIDATE_TERM).getAsInt() >= this.term
                && (votedFor.equals(-1) || votedFor == jsonObject.get(CANDIDATE_ID).getAsInt() )) {

            boolean logIsUpToDate;
            //check if candidate's log is as up to date as mine
            if (logs.size() == 0) {
                logIsUpToDate = true; //I have no logs yet
            }
            else if (jsonObject.get(LAST_LOG_INDEX).getAsInt() == -1) {
                logIsUpToDate = false; //Candidate has no logs, but I do
            }
            else { //me and the candidate both have logs
                if (jsonObject.get(LAST_LOG_TERM).getAsInt() > logs.get(logs.size() - 1).getTerm()) {
                    logIsUpToDate = true;
                }
                else if (jsonObject.get(LAST_LOG_TERM).getAsInt() < logs.get(logs.size() - 1).getTerm()) {
                    logIsUpToDate = false;
                }
                else { //terms are equal - whose log is longer?
                    logIsUpToDate = jsonObject.get(LAST_LOG_INDEX).getAsInt() >= logs.size() - 1;
                }
            }

            if (logIsUpToDate) {
                response.addProperty("result", true);
                votedFor = jsonObject.get(CANDIDATE_ID).getAsInt();
            }
            else {
                response.addProperty("result", false);
            }
        }
        else {
            response.addProperty("result", false);
        }

        try {
            writeToDisk("receiveRequestVote");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    private String receiveCommand(String payload, int sender, UUID guid) {
        JsonObject response = new JsonObject();

        boolean commandAdded = false;
        if (!state.equals(LEADER)) {
            System.out.println(Colors.ANSI_RED + "WARNING RaftNode (" + Thread.currentThread().getName() + "): not the leader but tried to process a command" + Colors.ANSI_RESET);
            response.addProperty(TYPE, REDIRECT);
            response.addProperty(CURRENT_LEADER, currentLeader);
        }
        else {
            //inject message UUID into command for log
            JsonObject commandJson = new JsonParser().parse(payload).getAsJsonObject();
            commandJson.addProperty("message_guid", guid.toString());

            ReplicatedLog newLog = new ReplicatedLog(term, commandJson.toString());
            logs.add(newLog);
            commandAdded = true;
            System.out.println(Colors.ANSI_YELLOW + "RaftNode (" + Thread.currentThread().getName() + "): Added " + payload + " from " + sender + " to log" + Colors.ANSI_RESET);
            try {
                writeToDisk("receiveCommand");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return !(commandAdded) ? response.toString() : "wait";
    }

    synchronized public void receiveMessage(Message message) {
        if (message.getType().equals(COMMAND)) {
            if (this.state.equals(LEADER)) {
                messageQueue.add(message);
            }
            else {
                // make a response for the robot and send it back
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty(TYPE, REDIRECT);

                if (currentLeader == null) {
                    jsonObject.addProperty(CURRENT_LEADER, "none");
                }
                else {
                    jsonObject.addProperty(CURRENT_LEADER, currentLeader);
                }

                this.messageReplies.put(message.getGuid(), jsonObject.toString());
            }
        }
        else {
            if (message == null) {
                System.out.println(Colors.ANSI_RED + ">>>WARNING RaftNode (" + Thread.currentThread().getName() + "): putting NULL message on queue" + Colors.ANSI_RESET);
            }

            if (message.getType().equals(REQ_VOTE) && (message.getTerm() > term || (message.getTerm() >= term && votedFor.equals(message.getSender())))) {
                timer.reset();
            }

            if (message.getTerm() >= term && message.getType().equals(APPEND)) {
                timer.reset();
                currentLeader = message.getSender();
            }

            if (message.getTerm() > term) {
                if (!state.equals(FOLLOW)) {
                    System.out.println(Colors.ANSI_YELLOW + "RaftNode (" + Thread.currentThread().getName() + "): switching to follower, new term " + message.getTerm() + " from node " + message.getSender() + " greater than my term " + term + Colors.ANSI_RESET);
                }
                state = FOLLOW;
                term = message.getTerm();
                votedFor = -1;
            }

            messageQueue.add(message);
        }
    }

    public synchronized HashMap<UUID, String> getMessageReplies() {
        return messageReplies;
    }

    synchronized private void processMessage() {
        String retVal;

        Message curMessage = messageQueue.poll();
        if (curMessage == null) {
            System.out.println(Colors.ANSI_RED + "WARNING RaftNode (" + Thread.currentThread().getName() + "): Pulled NULL message off of queue" + Colors.ANSI_RESET);
        }
        System.out.println(Colors.ANSI_CYAN + "RaftNode (" + Thread.currentThread().getName() + "): Processing " + curMessage.getType() + " message [" + curMessage.getGuid() + "] from node " + curMessage.getSender() + Colors.ANSI_RESET);

        if (curMessage.getType().equals(APPEND)) {
            retVal = receiveAppendEntries(curMessage.getPayload());
            System.out.println(Colors.ANSI_CYAN + "RaftNode (" + Thread.currentThread().getName() + "): message [" + curMessage.getGuid() + "] response: " + retVal + Colors.ANSI_RESET);
            messageReplies.put(curMessage.getGuid(), retVal);
        }
        else if (curMessage.getType().equals(REQ_VOTE)) {
            retVal = receiveRequestVote(curMessage.getPayload());
            System.out.println(Colors.ANSI_CYAN + "RaftNode (" + Thread.currentThread().getName() + "): message [" + curMessage.getGuid() + "] response: " + retVal + Colors.ANSI_RESET);
            messageReplies.put(curMessage.getGuid(), retVal);
        }
        else if (curMessage.getType().equals(COMMAND)) {
            retVal = receiveCommand(curMessage.getPayload(), curMessage.getSender(), curMessage.getGuid());
            System.out.println(Colors.ANSI_CYAN + "RaftNode (" + Thread.currentThread().getName() + "): message [" + curMessage.getGuid() + "] response: " + retVal + Colors.ANSI_RESET);

            if (!retVal.equals("wait")) {
                messageReplies.put(curMessage.getGuid(), retVal);
            }
        }
        else {
            System.out.println(Colors.ANSI_RED + "ERROR RaftNode (" + Thread.currentThread().getName() + "): UNSUPPORTED MESSAGE TYPE " + curMessage.getType() + Colors.ANSI_RESET);
        }
    }

    private void updateStateMachines() {
        // see the action is commited or not
        for (int i = lastApplied + 1; i <= commitIndex; i++) {
            JsonObject currentCommand = new JsonParser().parse(logs.get(i).getCommand()).getAsJsonObject();
            int robotActor = currentCommand.get(ROBOT_ID).getAsInt();
            JsonObject response = new JsonObject();
            response.addProperty(TYPE, REACTION);

            if (!robotStates.containsKey(robotActor)) { robotStates.put(robotActor, new StateMachine()); }

            if (!robotStates.containsKey(robotActor)) {
                System.out.println(Colors.ANSI_RED + "WARNING RaftNode (" + Thread.currentThread().getName() + "): Attempting to update non-existent state machine for robot " + currentCommand.get(ROBOT_ID).getAsInt() + Colors.ANSI_RESET);
            }
            else {
                String action = currentCommand.get(COMMAND).getAsString();

                if (action.equals(StateMachine.START)) {
                    JsonObject addressInfo = currentCommand.get("addressInfo").getAsJsonObject();
                    String[] addressFields = new String[]{addressInfo.get("address").getAsString(), String.valueOf(addressInfo.get("port").getAsInt()) };
                    robotAddresses.put(robotActor, addressFields);
                    System.out.println("Added address for robot " + robotActor + " - " + addressInfo.get("address").getAsString() + ":" + String.valueOf(addressInfo.get("port").getAsInt()));

                    response.addProperty(TYPE, REACTION);
                    response.addProperty(REACTION, true);
                }
                else if (action.equals(StateMachine.PUNCH_LEFT) || action.equals(StateMachine.PUNCH_RIGHT)) {
                    //get the id for the other state machine
                    int otherRobot = 0;
                    for (Integer robot : robotStates.keySet()) {
                        if (!robot.equals(robotActor)) {
                            otherRobot = robot;
                            break;
                        }
                    }

                    robotStates.get(robotActor).checkStates(action, robotStates.get(otherRobot).getState());
                    if (robotStates.get(robotActor).getState().equals(StateMachine.WIN)) {
                        robotStates.get(otherRobot).checkStates(StateMachine.LOST);
                        //TODO: if leader, put "win" event for winning robot and "KO" event for losing robot in log
                        //TODO: figure out how to sen
                        // d "KO" message to losing robot
                        if (this.state.equals(LEADER)) {
                            // robot actor send the leader punch left
                            // the leader will put the punch to the log and replicate the log
                            // the leader will find the punch is commited or not
                            // after the punch is committed, then the leader will apply the state machine

                            // log size 5, commit index 4, leader knows of majority of the nodes have up to index 4 in their log,
                            // at leader my last applied number is 2, from last applied to 4, apply these actions to the state machine
                            JsonObject object = new JsonObject();
                            object.addProperty(ROBOT_ID, robotActor);
                            object.addProperty(COMMAND, StateMachine.WIN);
                            ReplicatedLog  winCommand = new ReplicatedLog(this.term, object.toString());
                            this.logs.add(winCommand);

                            JsonObject lostObject = new JsonObject();
                            lostObject.addProperty(ROBOT_ID, otherRobot);
                            lostObject.addProperty(COMMAND, StateMachine.LOST);
                            ReplicatedLog lostCommand  = new ReplicatedLog(this.term, lostObject.toString());
                            this.logs.add(lostCommand);

                            try {
                                writeToDisk("updateStateMachines");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Message lostMessage = new Message(id, otherRobot, term, REACTION, lostObject.toString());
                            RobotClient robotClient = new RobotClient(robotAddresses.get(otherRobot)[0], Integer.parseInt(robotAddresses.get(otherRobot)[1]), lostMessage);
                            robotClient.sendMessage();
                        }
                    }
                }
                else {
                    robotStates.get(robotActor).checkStates(action);
                    System.out.println("New robot state is " + robotStates.get(robotActor).getState());
                }

                response.addProperty(REACTION, robotStates.get(robotActor).getState());
                System.out.println("Response: " + response.toString());

                if (currentCommand.has("message_guid")) {
                    messageReplies.put(UUID.fromString(currentCommand.get("message_guid").getAsString()), response.toString());
                }
            }
        }

        lastApplied = commitIndex;
    }

    private void writeToDisk(String callingMethod) throws IOException {
        JsonObject diskInfo = new JsonObject();
        Gson gson = new Gson();

        diskInfo.addProperty("callingMethod", callingMethod);
        diskInfo.addProperty("term", term);
        diskInfo.addProperty("votedFor", votedFor);
        diskInfo.addProperty("logs", gson.toJson(logs));
        String fileName = "Node_" + id + "_log.json";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(diskInfo.toString());
        writer.close();
    }

    public void restoreStateFromFile() {
        String fileName = "Node_" + id + "_log.json";
        Reader fileReader;

        try {
            fileReader = new FileReader(fileName);
            JsonObject jsonObject = new JsonParser().parse(fileReader).getAsJsonObject();

            term = jsonObject.get("term").getAsInt();
            votedFor = jsonObject.get("votedFor").getAsInt();

            TypeToken<List<ReplicatedLog>> token = new TypeToken<>() {};
            Gson gson = new Gson();
            logs = gson.fromJson(jsonObject.get("logs").getAsString(), token.getType());

            System.out.println(Colors.ANSI_WHITE + "-----Restored State From Disk-----" + Colors.ANSI_RESET);
            System.out.println(Colors.ANSI_WHITE + "Term: " + term + Colors.ANSI_RESET);
            System.out.println(Colors.ANSI_WHITE + "Voted For: " + votedFor + Colors.ANSI_RESET);
            System.out.println(Colors.ANSI_WHITE + "Log Entries: " + logs.size() + Colors.ANSI_RESET);
        } catch (FileNotFoundException e) {
            System.out.println("State file not found, starting from empty state...");
        }
    }
}
