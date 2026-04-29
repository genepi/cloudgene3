package cloudgene.mapred.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.jobs.Environment;
import cloudgene.mapred.jobs.workspace.LocalWorkspace;
import genepi.io.FileUtil;

public class Settings {

	private static final Logger log = LoggerFactory.getLogger(Settings.class);

	private String serverUrl = "http://localhost:8082";

	private String baseUrl = "";

	private String tempPath = "tmp";

	private String localWorkspace = "workspace";

	private String name = "Cloudgene";

	private Map<String, String> colors;

	private String secretKey = "";

	private Map<String, String> mail;

	private Map<String, String> database;

	private Map<String, Map<String, String>> plugins;

	private List<Map<String, String>> errorHandlers = new Vector<Map<String, String>>();

	private List<Map<String, String>> resources = new Vector<>();

	private int autoRetireInterval = 5;

	private int retireAfter = 6;

	private int notificationAfter = 4;

	private int threadsQueue = 5;

	private int maxRunningJobsPerUser = 2;

	private boolean autoRetire = false;

	private boolean writeStatistics = true;

	private boolean maintenance = false;

	private boolean emailRequired = true;

	private String adminMail = null;

	private String adminName = null;

	private boolean showLogs = false;

	private List<MenuItem> navigation = new Vector<MenuItem>();

	private Map<String, String> externalWorkspace = null;

	private int uploadLimit = 5000;

	private String googleAnalytics = "";

	private int maxDownloads = 10;

	private String port = "8082";

	private List<String> counters = new Vector<String>();

	public static final String DEFAULT_SECURITY_KEY = "default-key-change-me-immediately";

	// fake!
	private List<Application> apps = new Vector<Application>();

	private ApplicationRepository repository;

	public Settings() {

		repository = new ApplicationRepository();
		repository.setAppsFolder(Configuration.getAppsDirectory());

		// read default settings from env variables when set
		this.name = Configuration.get("CG_SERVICE_NAME", this.name);

		MenuItem helpMenuItem = new MenuItem();
		helpMenuItem.setId("help");
		helpMenuItem.setName("Help");
		String helpLink = Configuration.get("CG_HELP_PAGE", "http://docs.cloudgene.io");
		helpMenuItem.setLink(helpLink);
		navigation.add(helpMenuItem);

		database = new HashMap<String, String>();
		initDefaultDatabase(database, "data/cloudgene");

		colors = getDefaultColors();

	}

	public static Settings load() throws IOException {

		String filename = Configuration.getSettingsFilename();

		if (!new File(filename).exists()){
			log.info("Loading default settings. File '" + filename + "' not found.");
			return new Settings();
		}

		log.info("Loading settings from " + filename + "...");

		YamlConfig yamlConfig = new YamlConfig();
		yamlConfig.setPropertyElementType(Settings.class, "apps", Application.class);
		yamlConfig.setClassTag("cloudgene.mapred.util.Application", Application.class);
		YamlReader reader = new YamlReader(new FileReader(filename), yamlConfig);
		Settings settings = reader.read(Settings.class);
		log.info("Settings loaded.");

		log.info("Auto retire: " + settings.isAutoRetire());
		log.info("Retire jobs after " + settings.retireAfter + " days.");
		log.info("Notify user after " + settings.notificationAfter + " days.");
		log.info("Write statistics: " + settings.writeStatistics);

		if (settings.getServerUrl() == null || settings.getServerUrl().trim().isEmpty()) {
			throw new IOException("Error: serverUrl not set. Please set serverUrl in file '" + filename + "'");
		}

		return settings;

	}

	public List<Application> getApps() {
		return repository.getAll();
	}

	public void setApps(List<Application> apps) {
		this.apps = apps;
		repository.setApps(apps);
	}

	public static void initDefaultDatabase(Map<String, String> database, String defaultSchema) {
		database.put("driver", "h2");
		database.put("database", defaultSchema);
		database.put("user", "cloudgene");
		database.put("password", "cloudgene");
	}

	public static Map<String, String> getDefaultColors() {
		Map<String, String> colors = new HashMap<String, String>();
		colors.put("background", "#343a40");
		colors.put("foreground", "navbar-dark");
		return colors;
	}

	public void save() {
		String filename = Configuration.getSettingsFilename();
		try {

			File file = new File(filename);
			if (!file.exists()) {
				file.getParentFile().mkdirs();
			}

			log.info("Storing settings to file " + filename + " (" + getApps().size() + " apps installed)");
			apps = repository.getAll();

			YamlConfig yamlConfig = new YamlConfig();
			yamlConfig.setPropertyElementType(Settings.class, "apps", Application.class);
			yamlConfig.setClassTag("cloudgene.mapred.util.Application", Application.class);

			YamlWriter writer = new YamlWriter(new FileWriter(filename), yamlConfig);
			writer.write(this);
			writer.close();

		} catch (Exception e) {
			log.error("Storing settings failed.", e);
		}

	}

	public String getTempPath() {
		return tempPath;
	}

	public void setTempPath(String tempPath) {
		this.tempPath = tempPath;
	}

	public String getLocalWorkspace() {
		return localWorkspace;
	}

	public void setLocalWorkspace(String localWorkspace) {
		this.localWorkspace = localWorkspace;
	}

	public Map<String, String> getMail() {
		return mail;
	}

	public void setMail(Map<String, String> mail) {
		this.mail = mail;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getTempFilename(String filename) {
		String path = getTempPath();
		String name = FileUtil.getFilename(filename);
		return FileUtil.path(path, name);
	}

	public File getTempFolder(String name) throws IOException {
		return Files.createTempDirectory(Path.of(getTempPath()), name).toFile();
	}

	public void setNotificationAfter(int notificationAfter) {
		this.notificationAfter = notificationAfter;
	}

	public int getNotificationAfter() {
		return notificationAfter;
	}

	public int getNotificationAfterInSec() {
		return notificationAfter * 24 * 60 * 60;
	}

	public void setRetireAfter(int retireAfter) {
		this.retireAfter = retireAfter;
	}

	public int getRetireAfter() {
		return retireAfter;
	}

	public int getRetireAfterInSec() {
		return retireAfter * 24 * 60 * 60;
	}

	public void setAutoRetire(boolean autoRetire) {
		this.autoRetire = autoRetire;
	}

	public boolean isAutoRetire() {
		return autoRetire;
	}

	public void setWriteStatistics(boolean writeStatistics) {
		this.writeStatistics = writeStatistics;
	}

	public boolean isWriteStatistics() {
		return writeStatistics;
	}

	public void setMaintenance(boolean maintenance) {
		this.maintenance = maintenance;
	}

	public boolean isMaintenance() {
		return maintenance;
	}

	public void setAdminMail(String adminMail) {
		this.adminMail = adminMail;
	}

	public String getAdminMail() {
		return adminMail;
	}

	public void setAdminName(String adminName) {
		this.adminName = adminName;
	}

	public String getAdminName() {
		return adminName;
	}

	public void setThreadsQueue(int threadsQueue) {
		this.threadsQueue = threadsQueue;
	}

	public int getThreadsQueue() {
		return threadsQueue;
	}

	public int getMaxRunningJobsPerUser() {
		return maxRunningJobsPerUser;
	}

	public void setMaxRunningJobsPerUser(int maxRunningJobsPerUser) {
		this.maxRunningJobsPerUser = maxRunningJobsPerUser;
	}

	public void setDatabase(Map<String, String> database) {
		this.database = database;
	}

	public Map<String, String> getDatabase() {
		return database;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public void setNavigation(List<MenuItem> navigation) {
		this.navigation = navigation;
	}

	public List<MenuItem> getNavigation() {
		return navigation;
	}

	public int getUploadLimit() {
		return uploadLimit;
	}

	public void setUploadLimit(int uploadLimit) {
		this.uploadLimit = uploadLimit;
	}

	public int getAutoRetireInterval() {
		return autoRetireInterval;
	}

	public void setAutoRetireInterval(int autoRetireInterval) {
		this.autoRetireInterval = autoRetireInterval;
	}

	public Map<String, String> getColors() {
		return colors;
	}

	public void setColors(Map<String, String> colors) {
		this.colors = colors;
	}

	public void setPlugins(Map<String, Map<String, String>> plugins) {
		this.plugins = plugins;
	}

	public Map<String, Map<String, String>> getPlugins() {
		return plugins;
	}

	public Map<String, String> getPlugin(String plugin) {
		if (plugins != null) {
			return plugins.get(plugin);
		} else {
			return null;
		}
	}

	public void setErrorHandlers(List<Map<String, String>> errorHandlers) {
		this.errorHandlers = errorHandlers;
	}

	public List<Map<String, String>> getErrorHandlers() {
		return errorHandlers;
	}

	public void setResources(List<Map<String, String>> resources) {
		this.resources = resources;
	}

	public List<Map<String, String>> getResources() {
		return resources;
	}

	public void setRepository(ApplicationRepository repository) {
		this.repository = repository;
	}

	public void setGoogleAnalytics(String googleAnalytics) {
		this.googleAnalytics = googleAnalytics;
	}

	public String getGoogleAnalytics() {
		return googleAnalytics;
	}

	public void setMaxDownloads(int maxDownloads) {
		this.maxDownloads = maxDownloads;
	}

	public int getMaxDownloads() {
		return maxDownloads;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getPort() {
		return port;
	}

	public void setShowLogs(boolean showLogs) {
		this.showLogs = showLogs;
	}

	public boolean isShowLogs() {
		return showLogs;
	}

	public ApplicationRepository getApplicationRepository() {
		return repository;
	}

	public Map<String, String> getExternalWorkspace() {
		return externalWorkspace;
	}

	public void setExternalWorkspace(Map<String, String> externalWorkspace) {
		this.externalWorkspace = externalWorkspace;
	}

	public String getExternalWorkspaceLocation() {
		if (externalWorkspace == null) {
			externalWorkspace = new HashMap<>();
			externalWorkspace.put("type", "local");
			externalWorkspace.put("location", getLocalWorkspace());
		}

		if (externalWorkspace.get("location") == null) {
			return "";
		}

		return externalWorkspace.get("location");

	}

	public String getExternalWorkspaceType() {
		if (externalWorkspace == null) {
			externalWorkspace = new HashMap<>();
			externalWorkspace.put("type", "local");
			externalWorkspace.put("location", getLocalWorkspace());
		}

		if (externalWorkspace.get("type") == null) {
			return "";
		}

		return externalWorkspace.get("type");

	}

	public List<String> getCounters() {
		return counters;
	}

	public void setCounters(List<String> counters) {
		this.counters = counters;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public Environment buildEnvironment() {
		return new Environment(this);
	}

	public boolean isEmailRequired() {
		return emailRequired;
	}

	public void setEmailRequired(boolean emailRequired) {
		this.emailRequired = emailRequired;
	}

}