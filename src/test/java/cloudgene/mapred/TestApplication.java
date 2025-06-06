package cloudgene.mapred;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.esotericsoftware.yamlbeans.YamlException;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.util.TestMailServer;
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

	protected static List<Application> registerApplications(Settings settings) {

		List<Application> applications = new Vector<Application>();

		Application app = new Application();
		app.setFilename("test-data/return-true.yaml");
		app.setPermission("public");
		applications.add(app);

		Application app2 = new Application();
		app2.setFilename("test-data/return-false.yaml");
		app2.setPermission("public");
		applications.add(app2);

		Application app3 = new Application();
		app3.setFilename("test-data/return-exception.yaml");
		app3.setPermission("public");
		applications.add(app3);

		Application app4 = new Application();
		app4.setFilename("test-data/write-text-to-file.yaml");
		app4.setPermission("public");
		applications.add(app4);

		Application app5 = new Application();
		app5.setFilename("test-data/return-true-in-setup.yaml");
		app5.setPermission("public");
		applications.add(app5);

		Application app6 = new Application();
		app6.setFilename("test-data/return-false-in-setup.yaml");
		app6.setPermission("public");
		applications.add(app6);

		Application app7 = new Application();
		app7.setFilename("test-data/all-possible-inputs.yaml");
		app7.setPermission("public");
		applications.add(app7);

		Application app71 = new Application();
		app71.setFilename("test-data/all-possible-inputs-private.yaml");
		app71.setPermission("private");
		applications.add(app71);

		Application app8 = new Application();
		app8.setFilename("test-data/long-sleep.yaml");
		app8.setPermission("public");
		applications.add(app8);

		Application app9 = new Application();
		app9.setFilename("test-data/write-files-to-folder.yaml");
		app9.setPermission("public");
		applications.add(app9);

		Application app13 = new Application();
		app13.setFilename("test-data/three-tasks.yaml");
		app13.setPermission("public");
		applications.add(app13);

		Application app14 = new Application();
		app14.setFilename("test-data/write-text-to-std-out.yaml");
		app14.setPermission("public");
		applications.add(app14);

		//app links
		
		Application app17 = new Application();
		app17.setFilename("test-data/app-links.yaml");
		app17.setPermission("public");
		applications.add(app17);

		Application app18 = new Application();
		app18.setFilename("test-data/app-links-child.yaml");
		app18.setPermission("public");
		applications.add(app18);

		Application app19 = new Application();
		app19.setFilename("test-data/app-links-child-protected.yaml");
		app19.setPermission("protected");
		applications.add(app19);

		Application app22 = new Application();
		app22.setFilename("test-data/print-hidden-inputs.yaml");
		app22.setPermission("public");
		applications.add(app22);

		Application app23 = new Application();
		app23.setFilename("test-data/app-version-test.yaml");
		app23.setPermission("private");
		applications.add(app23);

		Application app24 = new Application();
		app24.setFilename("test-data/app-version-test2.yaml");
		app24.setPermission("private");
		applications.add(app24);

		settings.setApps(applications);

		return applications;

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
