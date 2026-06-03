package cloudgene.mapred.cli;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.ClassicConstants;
import cloudgene.mapred.util.Configuration;
import cloudgene.mapred.util.Settings;
import genepi.io.FileUtil;
import io.micronaut.context.env.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import cloudgene.mapred.server.Application;
import genepi.base.Tool;
import io.micronaut.runtime.Micronaut;

public class StartServer extends Tool {

	private static final String CONFIG_DIRECTORY = Configuration.getConfigDirectory();
	private static final String SECURITY_FILENAME = FileUtil.path(CONFIG_DIRECTORY, "security.yaml");
	private static final String SERVER_FILENAME = FileUtil.path(CONFIG_DIRECTORY, "server.yaml");

	private final String[] args;

	public StartServer(String[] args) {
		super(args);
		this.args = args;
	}

	@Override
	public void createParameters() {
		addFlag("verbose", "running in verbose mode");
	}

	@Override
	public void init() {}

	@Override
	public int run() {
		if (isFlagSet("verbose")) {
			System.setProperty(ClassicConstants.CONFIG_FILE_PROPERTY, "logback-verbose.xml");
		} else if (new File("webapp").exists()) {
			System.setProperty(ClassicConstants.CONFIG_FILE_PROPERTY, "logback.xml");
		} else {
			System.setProperty(ClassicConstants.CONFIG_FILE_PROPERTY, "logback.xml");
		}

		try {
			// load setting.yaml. contains applications, server configuration, ...
			Settings settings = Settings.load();

			String port = settings.getPort();
			Map<String, Object> properties = new HashMap<>();
			properties.put("micronaut.server.port", port);
			if (settings.getUploadLimit() != -1) {
				properties.put("micronaut.server.maxRequestSize", settings.getUploadLimit() + "MB");
				properties.put("micronaut.server.multipart.maxFileSize", settings.getUploadLimit() + "MB");
			}

			String secretKey = settings.getSecretKey();
			if (secretKey == null || secretKey.isEmpty() || secretKey.equals(Settings.DEFAULT_SECURITY_KEY)) {
				secretKey = RandomStringUtils.secureStrong().nextAlphabetic(64);
				settings.setSecretKey(secretKey);
				settings.save();
			}

			properties.put("micronaut.security.token.jwt.signatures.secret.generator.secret",
					settings.getSecretKey());
			properties.put("micronaut.autoRetireInterval", settings.getAutoRetireInterval() + "h");

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

			String baseUrl = settings.getBaseUrl().trim();
			if (!baseUrl.isEmpty()) {
				if (!baseUrl.startsWith("/") || baseUrl.endsWith("/")) {
					System.out.println("Error: baseUrl has wrong format. Example: \"/path\" or \"/path/subpath\".");
					System.exit(1);
				}
				properties.put("micronaut.server.context-path", baseUrl);
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
				Thread.sleep(5_000_000);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
	}
}
