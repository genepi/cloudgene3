package cloudgene.mapred.test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.UserDao;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.Settings;
import genepi.io.FileUtil;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;

@Context
@Replaces(cloudgene.mapred.server.Application.class)
@Requires(env = Environment.TEST)
public class TestApplication extends cloudgene.mapred.server.Application {

	@Inject // Use this constructor for dependency injection.
	public TestApplication() throws SQLException {
		super(loadSettings("primary"));
	}

	public TestApplication(Settings settings) throws SQLException {
		super(settings);
	}

	public static Settings loadSettings(String prefix) {
		Settings settings = new Settings();

		HashMap<String, String> mail = new HashMap<>();
		mail.put("smtp", "localhost");
		mail.put("port", TestMailServer.PORT + "");
		mail.put("user", "");
		mail.put("password", "");
		mail.put("name", "noreply@cloudgene");
		settings.setMail(mail);

		// delete old database
		FileUtil.deleteDirectory("test-database");

		HashMap<String, String> database = new HashMap<>();
		database.put("driver", "h2");
		database.put("database", "./test-database/mapred-" + prefix + "-" + UUID.randomUUID());
		database.put("user", "mapred");
		database.put("password", "mapred");
		settings.setDatabase(database);

		settings.setSecretKey(Settings.DEFAULT_SECURITY_KEY);

		// Set threads for workflow engine to 1
		settings.setThreadsQueue(1);
		settings.setMaintenance(false);

		registerApplications(settings);

		return settings;
	}

	protected static void registerApplications(Settings settings) {
		List<Application> applications = new Vector<>();

		// -------- Applications -------- //

		applications.add(new Application(
				"test-data/return-true.yaml",
				"public"));

		applications.add(new Application(
				"test-data/return-false.yaml",
				"public"));

		applications.add(new Application(
				"test-data/return-exception.yaml",
				"public"));

		applications.add(new Application(
				"test-data/write-text-to-file.yaml",
				"public"));

		applications.add(new Application(
				"test-data/return-true-in-setup.yaml",
				"public"));

		applications.add(new Application(
				"test-data/return-false-in-setup.yaml",
				"public"));

		applications.add(new Application(
				"test-data/all-possible-inputs.yaml",
				"public"));

		applications.add(new Application(
				"test-data/all-possible-inputs-private.yaml",
				"private"));

		applications.add(new Application(
				"test-data/long-sleep.yaml",
				"public"));

		applications.add(new Application(
				"test-data/write-files-to-folder.yaml",
				"public"));

		applications.add(new Application(
				"test-data/three-tasks.yaml",
				"public"));

		applications.add(new Application(
				"test-data/write-text-to-std-out.yaml",
				"public"));

		applications.add(new Application(
				"test-data/no-workflow.yaml",
				"public"));

		// -------- Application Links -------- //

		applications.add(new Application(
				"test-data/app-links.yaml",
				"public"));

		applications.add(new Application(
				"test-data/app-links-child.yaml",
				"public"));

		applications.add(new Application(
				"test-data/app-links-child-protected.yaml",
				"protected"));

		applications.add(new Application(
				"test-data/print-hidden-inputs.yaml",
				"public"));

		applications.add(new Application(
				"test-data/app-version-test.yaml",
				"private"));

		applications.add(new Application(
				"test-data/app-version-test2.yaml",
				"private"));

		settings.setApps(applications);
	}

	@Override
	protected void afterDatabaseConnection(Database database) {
		UserDao dao = new UserDao(database);

		addUser(
				dao,
				"admin",
				"admin1978",
				null,
				null,
				true,
				null);

		addUser(
				dao,
				"user",
				"admin1978",
				"User User",
				"user@example.com",
				false,
				new String[] { "public" });

		addUser(
				dao,
				"public",
				"public-password",
				null,
				null,
				false,
				new String[] { "public" });
	}

	private void addUser(
			UserDao dao,
			String username,
			String password,
			String fullName,
			String mail,
			boolean isAdmin,
			String[] roles) {

		User user = dao.findByUsername(username);

		if (user == null) {
			user = new User();

			// Mandatory
			user.setUsername(username);
			user.setPassword(HashUtil.hashPassword(password));

			// Optional
			user.setFullName(fullName);
			user.setMail(mail);

			if (roles != null) {
				user.setRoles(roles);
			}

			if (isAdmin) {
				user.makeAdmin();
			}

			dao.insert(user);
		}
	}
}
