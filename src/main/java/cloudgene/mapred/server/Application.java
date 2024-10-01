package cloudgene.mapred.server;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.engine.handler.IJobErrorHandler;
import cloudgene.mapred.jobs.engine.handler.JobErrorHandlerFactory;
import cloudgene.mapred.util.Configuration;
import io.micronaut.runtime.event.ApplicationShutdownEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.TemplateDao;
import cloudgene.mapred.database.updates.BcryptHashUpdate;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.DatabaseConnector;
import cloudgene.mapred.database.util.DatabaseConnectorFactory;
import cloudgene.mapred.database.util.DatabaseUpdater;
import cloudgene.mapred.database.util.Fixtures;
import cloudgene.mapred.jobs.PersistentWorkflowEngine;
import cloudgene.mapred.jobs.WorkflowEngine;
import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.util.Settings;
import genepi.io.FileUtil;
import io.micronaut.context.annotation.Context;

@Context
public class Application {

	public static final String VERSION = "3.0.3";

	private Database database;

	public static Settings settings;

	private WorkflowEngine engine;

	private Map<String, String> cacheTemplates;

	protected Logger log = LoggerFactory.getLogger(Application.class);

	public Application() throws Exception {

		PluginManager pluginManager = PluginManager.getInstance();
		pluginManager.initPlugins(settings);

		database = new Database();

		// create h2 or mysql connector
		DatabaseConnector connector = DatabaseConnectorFactory.createConnector(settings.getDatabase());

		if (connector == null) {

			log.error("Unknown database driver");
			System.exit(1);

		}

		// connect do database
		try {

			database.connect(connector);

			log.info("Establish connection to database successful");

		} catch (SQLException e) {

			log.error("Establish connection to database failed", e);
			System.exit(1);

		}

		// update database schema if needed
		log.info("Setup Database...");
		InputStream is = Application.class.getResourceAsStream("/updates.sql");

		DatabaseUpdater updater = new DatabaseUpdater(database, Configuration.getVersionFilename(), is, VERSION);
		updater.addUpdate("2.3.0", new BcryptHashUpdate());

		if (!updater.updateDB()) {
			System.exit(-1);
		}

		// create directories
		FileUtil.createDirectory(settings.getTempPath());
		FileUtil.createDirectory(settings.getLocalWorkspace());

		// insert fixtures
		Fixtures.insert(database);

		reloadTemplates();
		
		afterDatabaseConnection(database);

		// start workflow engine
		try {

			PersistentWorkflowEngine persistentWorkflowEngine = new PersistentWorkflowEngine(database, settings.getThreadsQueue());
			for (Map<String, String> map: settings.getErrorHandlers()) {
				IJobErrorHandler handler = JobErrorHandlerFactory.createByMap(map);
				persistentWorkflowEngine.addJobErrorHandler(handler);
				log.info("Created Job Error handler `" + handler.getName() + "`.");
			}
			engine = persistentWorkflowEngine;
			new Thread(engine).start();

		} catch (Exception e) {

			log.error("Can't launch the web server.\nAn unexpected " + "exception occured:", e);

			database.disconnect();

			System.exit(1);

		}

	}

	@EventListener
	public void stop(final ApplicationShutdownEvent event) throws SQLException {
		System.out.println("Shutting down Cloudgene...");
		log.info("Shutting down Cloudgene...");
		engine.block();
		database.disconnect();
	}

	public WorkflowEngine getWorkflowEngine() {
		return engine;
	}

	public Settings getSettings() {
		return settings;
	}

	public Database getDatabase() {
		return database;
	}

	public void reloadTemplates() {
		TemplateDao dao = new TemplateDao(database);
		List<cloudgene.mapred.core.Template> templates = dao.findAll();

		cacheTemplates = new HashMap<String, String>();
		for (cloudgene.mapred.core.Template snippet : templates) {
			cacheTemplates.put(snippet.getKey(), snippet.getText());
		}
	}

	public String getTemplate(String key) {

		String template = cacheTemplates.get(key);

		if (template != null) {
			return template;
		} else {
			return "!" + key;
		}

	}

	public String getTemplate(String key, Object... strings) {

		String template = cacheTemplates.get(key);

		if (template != null) {
			return String.format(template, strings);
		} else {
			return "!" + key;
		}

	}

	protected void afterDatabaseConnection(Database database) {

	}

}
