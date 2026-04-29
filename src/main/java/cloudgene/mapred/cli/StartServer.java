package cloudgene.mapred.cli;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cloudgene.mapred.util.Configuration;
import genepi.io.FileUtil;
import org.apache.commons.lang.RandomStringUtils;

import ch.qos.logback.classic.util.ContextInitializer;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.util.Settings;
import genepi.base.Tool;
import io.micronaut.context.env.Environment;
import io.micronaut.runtime.Micronaut;

public class StartServer extends Tool {

	public static final String CONFIG_DIRECTORY = Configuration.getConfigDirectory();

	public static final String SECURITY_FILENAME = FileUtil.path(CONFIG_DIRECTORY, "security.yaml");

	public static final String SERVER_FILENAME = FileUtil.path(CONFIG_DIRECTORY, "server.yaml");

	private String[] args;

	public StartServer(String[] args) {
		super(args);
		this.args = args;
	}

	@Override
	public void createParameters() {
		addFlag("verbose", "running in verbose mode");
	}

	@Override
	public int run() {

		if (isFlagSet("verbose")) {
			System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback-verbose.xml");
		} else if (new File("webapp").exists()) {
			System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.xml");
		} else {
			System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback-dev.xml");
		}


		try {

			// load setting.yaml. contains applications, server configuration, ...
			Application.settings = Settings.load();

			String port = Application.settings.getPort();
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put("micronaut.server.port", port);
			if (Application.settings.getUploadLimit() != -1) {
				properties.put("micronaut.server.maxRequestSize", Application.settings.getUploadLimit() + "MB");
				properties.put("micronaut.server.multipart.maxFileSize", Application.settings.getUploadLimit() + "MB");
			}

			String secretKey = Application.settings.getSecretKey();
			if (secretKey == null || secretKey.isEmpty() || secretKey.equals(Settings.DEFAULT_SECURITY_KEY)) {
				secretKey = RandomStringUtils.randomAlphabetic(64);
				Application.settings.setSecretKey(secretKey);
				Application.settings.save();
			}

			properties.put("micronaut.security.token.jwt.signatures.secret.generator.secret",
					Application.settings.getSecretKey());
			properties.put("micronaut.autoRetireInterval", Application.settings.getAutoRetireInterval() + "h");

			String customConfigurationFiles = null;

			if (new File(SECURITY_FILENAME).exists()) {
				customConfigurationFiles = SECURITY_FILENAME;
			}

			if (new File(SERVER_FILENAME).exists()) {
				if (customConfigurationFiles != null) {
					customConfigurationFiles += ",";
				} else {
					customConfigurationFiles = "";
				}
				customConfigurationFiles += SERVER_FILENAME;
			}

			if (customConfigurationFiles != null) {
				System.out.println("Use config file(s): " + customConfigurationFiles);
				System.setProperty("micronaut.config.files", customConfigurationFiles);
			}


			String baseUrl = Application.settings.getBaseUrl();
			if (!baseUrl.trim().isEmpty()) {
				if (!baseUrl.startsWith("/") || baseUrl.endsWith("/")){
					System.out.println("Error: baseUrl has wrong format. Example: \"/path\" or \"/path/subpath\".");
					System.exit(1);
				}
				properties.put("micronaut.server.context-path", baseUrl);
			} else {

			}

			if (new File("webapp").exists()) {

				Micronaut.build(args).mainClass(Application.class).properties(properties).start();

			} else {

				System.out.println("Start in DEVELOPMENT mode");

				Micronaut.build(args).mainClass(Application.class).properties(properties)
						.defaultEnvironments(Environment.DEVELOPMENT).start();

			}

			System.out.println();
			System.out.println("Server is running on port " + port);
			System.out.println();
			System.out.println("Please press ctrl-c to stop.");
			while (true) {
				Thread.sleep(5000000);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
	}

	@Override
	public void init() {

	}

}