package cloudgene.mapred.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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

    public boolean isEnabled() {
        return host != null && !host.isEmpty();
    }

    public List<String> scp(String source, String target) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("scp");
        command.add("-P");
        command.add(String.valueOf(port));
        command.add(source);
        command.add(target);
        return command;
    }

    public String runScp(String source, String target) throws IOException {
        return run(scp(source, target));
    }

    public List<String> ssh(String cmd) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("ssh");
        command.add("-p");
        command.add(String.valueOf(port));
        command.add(getUserAndHost());
        command.add(cmd);
        return command;
    }

    public String runSsh(String cmd) throws IOException {
        return run(ssh(cmd));
    }

    public List<String> rsync(String directory, String target) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("rsync");
        command.add("-avz");
        command.add("--exclude=.git/");
        command.add("-e");
        command.add("ssh -p " + getPort());

        // Ensure remote dirs exist before rsync
        command.add("--rsync-path=mkdir -p " + target + " && rsync");

        //command.add("--delete");
        command.add(directory.endsWith("/") ? directory : directory + "/");
        String completeTarget = getUserAndHost() + ":" + target;
        command.add(completeTarget);
        return command;
    }

    private String run(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        try {
            if (process.waitFor() != 0) {
                throw new IOException("Command failed: " + String.join(" ", command) + ":"+ output);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
        return output.toString();
    }
}
