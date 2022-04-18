public class RemoteRobot {
    private String address;
    private int id, port;

    public RemoteRobot(int id, String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    public int getId() { return id; }
    public String getAddress() { return address; }
    public int getPort() { return port; }
}