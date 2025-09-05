package cloudgene.mapred.util;

public class SSHJumper {

    private String host = "";

    private String user = "";

    private int port = 22;

    private String workspace = null;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getUserAndHost() {
        if (user.isEmpty()) {
            return host;
        } else {
            return user + "@" + host;
        }
    }
}
