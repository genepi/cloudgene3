package cloudgene.mapred.jobs.workspace;

import cloudgene.mapred.util.SSHJumper;
import com.jcraft.jsch.*;
import genepi.io.FileUtil;
import cloudgene.mapred.jobs.Download;
import cloudgene.mapred.util.HashUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class SFTPWorkspace implements IWorkspace {

    private static final Logger log = LoggerFactory.getLogger(SFTPWorkspace.class);

    private static final String OUTPUT_DIRECTORY = "outputs";
    private static final String INPUT_DIRECTORY = "input";
    private static final String LOGS_DIRECTORY = "logs";
    private static final String TEMP_DIRECTORY = "temp";

    private String location;
    private String job;

    private String host;
    private int port;
    private String username;
    private String password;

    public SFTPWorkspace(SSHJumper jumper) {
        this(jumper.getHost(), jumper.getPort(), jumper.getUser(), jumper.getPassword(), jumper.getWorkspace());
    }

    public SFTPWorkspace(String host, int port, String username, String password, String location) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
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

    private ChannelSftp connect() throws IOException {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, host, port);
            session.setPassword(password);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            return (ChannelSftp) channel;
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    private void disconnect(ChannelSftp channel) throws IOException {
        if (channel != null) {
            Session session = null;
            try {
                session = channel.getSession();
            } catch (JSchException e) {
                throw new IOException(e);
            }
            channel.exit();
            session.disconnect();
        }
    }

    @Override
    public void setup() throws IOException {
        if (job == null || location == null) {
            throw new IOException("SFTP workspace setup failed. Missing job or location.");
        }
        try {
           // upload("version.txt", new File("version.txt"));
        } catch (Exception e) {
            throw new IOException("Failed to upload version file to SFTP.", e);
        }
    }

    @Override
    public String upload(String id, File file) throws IOException {
        String remotePath = location + "/" + job + "/" + id + "/";
        ChannelSftp sftp = null;
        try {
            sftp = connect();
            createDirectories(sftp, remotePath);
            sftp.put(new FileInputStream(file), remotePath + file.getName());
            return remotePath + file.getName();
        } catch (Exception e) {
            throw new IOException("Upload to SFTP failed.", e);
        } finally {
            disconnect(sftp);
        }
    }

    private void createDirectories(ChannelSftp sftp, String path) throws SftpException {
        String[] folders = path.split("/");
        String currentPath = "";
        for (String folder : folders) {
            if (folder.length() == 0) continue;
            currentPath += "/" + folder;
            try {
                sftp.cd(currentPath);
            } catch (SftpException e) {
                sftp.mkdir(currentPath);
            }
        }
    }

    @Override
    public String uploadInput(String id, File file) throws IOException {
        return upload(FileUtil.path(INPUT_DIRECTORY, id), file);
    }

    @Override
    public String uploadLog(File file) throws IOException {
        return upload(LOGS_DIRECTORY, file);
    }

    public static String stripSftpPrefix(String sftpUrl) {
        // Match and remove protocol and host:port
        if (sftpUrl.startsWith("sftp://")) {
            // Remove "sftp://"
            String withoutProtocol = sftpUrl.substring(7);
            // Find first '/' after host:port
            int firstSlashIndex = withoutProtocol.indexOf('/');
            if (firstSlashIndex != -1) {
                return withoutProtocol.substring(firstSlashIndex); // Return only path
            }
        }
        // Return original if not matching
        return sftpUrl;
    }

    @Override
    public InputStream download(String path) throws IOException {
        final ChannelSftp  sftp = connect();;
        try {
            String relativePath = stripSftpPrefix(path);
            InputStream inputStream = sftp.get(relativePath);

            // Wrap the input stream to ensure connection is closed when the stream is closed
            return new FilterInputStream(inputStream) {
                @Override
                public void close() throws IOException {
                    super.close();
                    disconnect(sftp); // Ensure the SFTP channel is closed
                }
            };
        } catch (Exception e) {
            disconnect(sftp); // Clean up on failure
            throw new IOException("Download from SFTP failed.", e);
        }
    }

    @Override
    public String downloadLog(String name) throws IOException {
        try (InputStream is = download(location + "/" + job + "/" + LOGS_DIRECTORY + "/" + name)) {
            return FileUtil.readFileAsString(is);
        }
    }

    @Override
    public boolean exists(String path) {
        ChannelSftp sftp = null;
        try {
            sftp = connect();
            sftp.lstat(path);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                disconnect(sftp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
        ChannelSftp sftp = null;
        try {
            sftp = connect();
            Vector<ChannelSftp.LsEntry> list = sftp.ls(path);
            for (ChannelSftp.LsEntry entry : list) {
                String filePath = path + "/" + entry.getFilename();
                if (!entry.getAttrs().isDir()) {
                    sftp.rm(filePath);
                } else if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                    deleteFolder(filePath);
                }
            }
            sftp.rmdir(path);
        } catch (Exception e) {
            throw new IOException("Failed to delete folder: " + path, e);
        } finally {
            disconnect(sftp);
        }
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
        List<Download> downloads = new ArrayList<>();
        ChannelSftp sftp = null;
        try {
            sftp = connect();
            traverseSftp(sftp, url, downloads, url); // Pass root url as base for relative path
        } catch (Exception e) {
            throw new IOException("Failed to list downloads from SFTP.", e);
        } finally {
            disconnect(sftp);
        }
        return downloads;
    }

    // Recursive method to traverse directories
    private void traverseSftp(ChannelSftp sftp, String currentPath, List<Download> downloads, String baseUrl) throws SftpException {
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> list = sftp.ls(currentPath);

        for (ChannelSftp.LsEntry entry : list) {
            String filename = entry.getFilename();

            if (".".equals(filename) || "..".equals(filename)) {
                continue;
            }

            String fullPath = currentPath + "/" + filename;

            if (entry.getAttrs().isDir()) {
                traverseSftp(sftp, fullPath, downloads, baseUrl);
            } else {
                String size = FileUtils.byteCountToDisplaySize(entry.getAttrs().getSize());
                String hash = HashUtil.getSha256(filename + size + (Math.random() * 100000));

                // Compute relative name
                String relativeName = fullPath.substring(baseUrl.length());
                if (relativeName.startsWith("/")) {
                    relativeName = relativeName.substring(1);
                }

                Download download = new Download();
                download.setName(relativeName);
                //download and update?
                download.setPath("sftp://" + host + ":" + port + fullPath);
                download.setSize(size);
                download.setHash(hash);

                downloads.add(download);
            }
        }
    }

    @Override
    public List<Download> getLogs() throws IOException {
        String url = location + "/" + job + "/" + LOGS_DIRECTORY;
        return getDownloads(url);
    }
}
