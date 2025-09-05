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
    private String job;
    private SSHJumper jumper;

    public SFTPWorkspace(SSHJumper jumper) {
        this.jumper = jumper;
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
        if (job == null || jumper.getWorkspace() == null) {
            throw new IOException("SSH workspace setup failed. Missing job or location.");
        }
    }

    @Override
    public String upload(String id, File file) throws IOException {
        String remotePath = jumper.getWorkspace() + "/" + job + "/" + id + "/";
        jumper.runSsh("mkdir -p " + remotePath);
        jumper.runScp(file.getAbsolutePath(), jumper.getUserAndHost() + ":" + remotePath);
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
        jumper.runScp(jumper.getUserAndHost() + ":" + relativePath, tempFile.getAbsolutePath());
        return new FileInputStream(tempFile);
    }

    @Override
    public String downloadLog(String name) throws IOException {
        try (InputStream is = download(jumper.getWorkspace() + "/" + job + "/" + LOGS_DIRECTORY + "/" + name)) {
            return FileUtil.readFileAsString(is);
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            jumper.runSsh("[ -e " + path + " ]");
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void delete(String job) throws IOException {
        deleteFolder(jumper.getWorkspace() + "/" + job);
    }

    @Override
    public void cleanup(String job) throws IOException {
        deleteFolder(jumper.getWorkspace() + "/" + job + "/" + TEMP_DIRECTORY);
        deleteFolder(jumper.getWorkspace() + "/" + job + "/" + INPUT_DIRECTORY);
    }

    private void deleteFolder(String path) throws IOException {
        jumper.runSsh("rm -rf " + path);
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
        return jumper.getWorkspace() + "/" + job + "/" + OUTPUT_DIRECTORY + "/" + id;
    }

    @Override
    public String createFile(String folder, String id) {
        return jumper.getWorkspace() + "/" + job + "/" + OUTPUT_DIRECTORY + "/" + folder + "/" + id;
    }

    @Override
    public String createLogFile(String id) {
        return jumper.getWorkspace() + "/" + job + "/" + LOGS_DIRECTORY + "/" + id;
    }

    @Override
    public String createTempFolder(String id) {
        return jumper.getWorkspace() + "/" + job + "/" + TEMP_DIRECTORY + "/" + id;
    }

    @Override
    public List<Download> getDownloads(String url) throws IOException {

        //TODO: rsync url with local folder.
        //TODO: use logic from lcaolWorkspace to add downloads
        //TODO: I need a link to localWorkspace.

        List<Download> downloads = new ArrayList<>();
        String output = jumper.runSsh("find " + url + " -type f");
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String size = jumper.runSsh("stat -c%s " + line).trim();
            String hash = HashUtil.getSha256(line + size + (Math.random() * 100000));
            String relativeName = line.substring(url.length());
            if (relativeName.startsWith("/")) {
                relativeName = relativeName.substring(1);
            }
            Download download = new Download();
            download.setName(relativeName);
            download.setPath("sftp://" + jumper.getHost() + ":" + jumper.getPort() + line);
            download.setSize(FileUtils.byteCountToDisplaySize(Long.parseLong(size)));
            download.setHash(hash);
            downloads.add(download);
        }
        return downloads;
    }

    @Override
    public List<Download> getLogs() throws IOException {
        String url = jumper.getWorkspace() + "/" + job + "/" + LOGS_DIRECTORY;
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

}