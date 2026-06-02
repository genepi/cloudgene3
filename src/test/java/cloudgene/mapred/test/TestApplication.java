package cloudgene.mapred.test;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.esotericsoftware.yamlbeans.YamlException;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.UserDao;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.Settings;
import genepi.io.FileUtil;
import io.micronaut.context.annotation.Context;

@Context
public class TestApplication extends cloudgene.mapred.server.Application {

	static {
		try {
			TestApplication.settings = loadSettings();
		} catch (FileNotFoundException | YamlException e) {
			e.printStackTrace();
		}
	}
	
	public TestApplication() throws Exception {
		super();
	}

	protected static Settings loadSettings() throws FileNotFoundException, YamlException {
		
		Settings settings = new Settings();

		HashMap<String, String> mail = new HashMap<String, String>();
		mail.put("smtp", "localhost");
		mail.put("port", TestMailServer.PORT + "");
		mail.put("user", "");
		mail.put("password", "");
		mail.put("name", "noreply@cloudgene");
		settings.setMail(mail);

		// delete old database
		FileUtil.deleteDirectory("test-database");

		HashMap<String, String> database = new HashMap<String, String>();
		database.put("driver", "h2");
		database.put("database", "./test-database/mapred");
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

		String username = "admin";
		String password = "admin1978";

		// insert user admin
		UserDao dao = new UserDao(database);
		User adminUser = dao.findByUsername(username);
		if (adminUser == null) {
			adminUser = new User();
			adminUser.setUsername(username);
			password = HashUtil.hashPassword(password);
			adminUser.setPassword(password);
			adminUser.makeAdmin();
			dao.insert(adminUser);
		}

		String usernameUser = "user";
		String passwordUser = "admin1978";

		// insert user admin
		User user = dao.findByUsername(usernameUser);
		if (user == null) {
			user = new User();
			user.setUsername(usernameUser);
			password = HashUtil.hashPassword(passwordUser);
			user.setPassword(passwordUser);
			user.setRoles(new String[] { "public" });
			dao.insert(user);
		}

		User userPublic = dao.findByUsername("public");
		if (userPublic == null) {
			userPublic = new User();
			userPublic.setUsername("public");
			password = HashUtil.hashPassword("public");
			userPublic.setPassword(password);
			userPublic.setRoles(new String[] { "public" });
			dao.insert(userPublic);
		}

	}

}
