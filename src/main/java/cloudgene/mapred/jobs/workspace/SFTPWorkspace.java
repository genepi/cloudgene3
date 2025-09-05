package cloudgene.mapred.jobs.workspace;
import cloudgene.mapred.jobs.Download;
import cloudgene.mapred.util.HashUtil;
import genepi.io.FileUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.*;
import cloudgene.mapred.util.SSHJumper;

public class SFTPWorkspace implements IWorkspace {
    private static final Logger log = LoggerFactory.getLogger(SFTPWorkspace.class);
    private static final String OUTPUT_DIRECTORY = "outputs";
    private static final String INPUT_DIRECTORY = "input";
    private static final String LOGS_DIRECTORY = "logs";
    private static final String TEMP_DIRECTORY = "temp";
    private String location;
    private String job;
    private String userAndHost;
    private int port;


    public SFTPWorkspace(SSHJumper jumper) { this(jumper.getUserAndHost(), jumper.getPort(), jumper.getWorkspace()); }


    public SFTPWorkspace(String userAndHost, int port, String location) {
        this.userAndHost = userAndHost;
        this.port = port;
        this.location = location;
    }


    @Override
    public String getName() {
        return "SFTP";
    }
    @Override
    public void setJob(String job) {
        this.job = job;
    }
    @Override
    public void setup() throws IOException {
        if (job == null || location == null) {
            throw new IOException("SSH workspace setup failed. Missing job or location.");
        }
    }
    @Override
    public String upload(String id, File file) throws IOException {
        String remotePath = location + "/" + job + "/" + id + "/";
        runSshCommand("mkdir -p " + remotePath);
        runScpCommand(file.getAbsolutePath(), userAndHost + ":" +remotePath);
        return remotePath + file.getName();
    }
    @Override
    public String uploadInput(String id, File file) throws IOException {
        return upload(FileUtil.path(INPUT_DIRECTORY, id), file);
    }
    @Override
    public String uploadLog(File file) throws IOException {
        return upload(LOGS_DIRECTORY, file);
    }
    @Override
    public InputStream download(String path) throws IOException {
        String relativePath = stripSftpPrefix(path);
        File tempFile = File.createTempFile("download", ".tmp");
        tempFile.deleteOnExit();
        runScpCommand(userAndHost + ":" + relativePath, tempFile.getAbsolutePath());
        return new FileInputStream(tempFile);
    }
    @Override
    public String downloadLog(String name) throws IOException {
        try (InputStream is = download(location + "/" + job + "/" + LOGS_DIRECTORY + "/" + name)) {
            return FileUtil.readFileAsString(is);
        }
    }
    @Override
    public boolean exists(String path) {
        try {
            runSshCommand("[ -e " + path + " ]");
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    @Override
    public void delete(String job) throws IOException {
        deleteFolder(location + "/" + job);
    }
    @Override
    public void cleanup(String job) throws IOException {
        deleteFolder(location + "/" + job + "/" + TEMP_DIRECTORY);
        deleteFolder(location + "/" + job + "/" + INPUT_DIRECTORY);
    }
    private void deleteFolder(String path) throws IOException {
        runSshCommand("rm -rf " + path);
    }
    @Override
    public String createPublicLink(String url) {
        return null;
    }
    @Override
    public String getParent(String url) {
        int index = url.lastIndexOf('/');
        return (index > 0) ? url.substring(0, index) : null;
    }
    @Override
    public String createFolder(String id) {
        return location + "/" + job + "/" + OUTPUT_DIRECTORY + "/" + id;
    }
    @Override
    public String createFile(String folder, String id) {
        return location + "/" + job + "/" + OUTPUT_DIRECTORY + "/" + folder + "/" + id;
    }
    @Override
    public String createLogFile(String id) {
        return location + "/" + job + "/" + LOGS_DIRECTORY + "/" + id;
    }
    @Override
    public String createTempFolder(String id) {
        return location + "/" + job + "/" + TEMP_DIRECTORY + "/" + id;
    }
    @Override
    public List<Download> getDownloads(String url) throws IOException {

        //TODO: rsync url with local folder.
        //TODO: use logic from lcaolWorkspace to add downloads
        //TODO: I need a link to localWorkspace.

        List<Download> downloads = new ArrayList<>();
        String output = runSshCommandWithOutput("find " + url + " -type f");
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String size = runSshCommandWithOutput("stat -c%s " + line).trim();
            String hash = HashUtil.getSha256(line + size + (Math.random() * 100000));
            String relativeName = line.substring(url.length());
            if (relativeName.startsWith("/")) {
                relativeName = relativeName.substring(1);
            }
            Download download = new Download();
            download.setName(relativeName);
            download.setPath("sftp://" + userAndHost + ":" + port + line);
            download.setSize(FileUtils.byteCountToDisplaySize(Long.parseLong(size)));
            download.setHash(hash);
            downloads.add(download);
        }
        return downloads;
    }
    @Override
    public List<Download> getLogs() throws IOException {
        String url = location + "/" + job + "/" + LOGS_DIRECTORY;
        return getDownloads(url);
    }
    // --- Helper Methods ---
    private String stripSftpPrefix(String sftpUrl) {
        if (sftpUrl.startsWith("sftp://")) {
            String withoutProtocol = sftpUrl.substring(7);
            int firstSlashIndex = withoutProtocol.indexOf('/');
            if (firstSlashIndex != -1) {
                return withoutProtocol.substring(firstSlashIndex);
            }
        }
        return sftpUrl;
    }
    private void runScpCommand(String source, String target) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("scp");
        command.add("-P");
        command.add(String.valueOf(port));
        command.add(source);
        command.add(target);
        runProcess(command);
    }
    private void runSshCommand(String cmd) throws IOException {
        runProcess(Arrays.asList("ssh", "-p", String.valueOf(port), userAndHost, cmd));
    }
    private String runSshCommandWithOutput(String cmd) throws IOException {
        return runProcessWithOutput(Arrays.asList("ssh", "-p", String.valueOf(port), userAndHost, cmd));
    }
    private void runProcess(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try {
            if (process.waitFor() != 0) {
                throw new IOException("Command failed: " + String.join(" ", command));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
    private String runProcessWithOutput(List<String> command) throws IOException {
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
                throw new IOException("Command failed: " + String.join(" ", command));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
        return output.toString();
    }
}