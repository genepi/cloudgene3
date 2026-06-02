package cloudgene.mapred.server;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

import cloudgene.mapred.database.updates.DatabaseUpdater;
import cloudgene.mapred.jobs.engine.handler.IJobErrorHandler;
import cloudgene.mapred.jobs.engine.handler.JobErrorHandlerFactory;
import cloudgene.mapred.util.Configuration;
import cloudgene.mapred.util.Settings;
import io.micronaut.runtime.event.ApplicationShutdownEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.dao.TemplateDao;
import cloudgene.mapred.database.updates.BcryptHashUpdate;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.connector.DatabaseConnector;
import cloudgene.mapred.database.connector.DatabaseConnectorFactory;
import cloudgene.mapred.database.util.Fixtures;
import cloudgene.mapred.jobs.PersistentWorkflowEngine;
import cloudgene.mapred.jobs.WorkflowEngine;
import cloudgene.mapred.plugins.PluginManager;
import genepi.io.FileUtil;
import io.micronaut.context.annotation.Context;

@Context
public class Application {

	private static final Logger log = LoggerFactory.getLogger(Application.class);

	public static final String VERSION = "3.1.3";

	private final Settings settings;
	private final Database database;
	private final WorkflowEngine engine;
	private final Map<String, String> templates;

	public Application(Settings settings) throws SQLException {
		// ================ SETTINGS ================ //

		this.settings = settings;

		// Init plugins
		PluginManager pluginManager = PluginManager.getInstance();
		pluginManager.initPlugins(settings);

		// Create directories
		FileUtil.createDirectory(settings.getTempPath());
		FileUtil.createDirectory(settings.getLocalWorkspace());

		// ================ DATABASE ================ //

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

		DatabaseUpdater updater;
		try (InputStream updatesFile = Application.class.getResourceAsStream("/updates.sql")) {
			updater = new DatabaseUpdater(database, Configuration.getVersionFilename(), updatesFile, VERSION);

			updater.addUpdate("2.3.0", new BcryptHashUpdate());

			if (!updater.updateDB()) {
				System.exit(-1);
			}
		} catch (IOException e) {
			log.error("Failed to update database version", e);
			System.exit(1);
		}

		Fixtures.insert(database);

		// Template stuff done halfway through DB stuff (because they touch DB I guess?)
		templates = new HashMap<>();
		reloadTemplates();

		afterDatabaseConnection(database);

		// ================ WORKFLOW ENGINE ================ //

		PersistentWorkflowEngine engine = null; // Dummy so we can try-catch and still make engine final.

		try {
			engine = new PersistentWorkflowEngine(database, settings.getThreadsQueue());

			for (Map<String, String> map : settings.getErrorHandlers()) {
				IJobErrorHandler handler = JobErrorHandlerFactory.createByMap(map);
				engine.addJobErrorHandler(handler);

				log.info("Created Job Error handler `{}`.", handler.getName());
			}
			new Thread(engine).start();
		} catch (Exception e) {
			log.error("Can't launch the web server.\nAn unexpected exception occurred:", e);

			database.disconnect();
			System.exit(1);
		}

		this.engine = engine;
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

	/**
	 * Forces a full reload of all cached templates (accesses DB).
	 */
	public void reloadTemplates() {
		TemplateDao dao = new TemplateDao(database);
		List<cloudgene.mapred.core.Template> dbTemplates = dao.findAll();

		templates.clear();
		for (cloudgene.mapred.core.Template snippet : dbTemplates) {
			templates.put(snippet.getKey(), snippet.getText());
		}
	}

	/**
	 * If a template is cached under the given {@code key}, it is returned.
	 * Otherwise, {@code "!<key>"} is returned.
	 * <p>
	 * This version does not take any parameters and returns the raw template.
	 */
	public String getTemplate(String key) {
		String template = templates.get(key);

		if (template != null) {
			return template;
		} else {
			return "!" + key;
		}
	}

	/**
	 * If a template is cached under the given {@code key}, returns a rendered
	 * version of the template, using the provided {@code strings} as interpolation
	 * parameters. Otherwise, {@code "!<key>"} is returned.
	 * <p>
	 * The number of provided parameters must match the number of {@code %s} in the
	 * template, otherwise an error is thrown.
	 */
	public String getTemplate(String key, Object... strings) {
		String template = templates.get(key);

		if (template != null) {
			try {
				return String.format(template, strings);
			} catch (IllegalFormatException e) {
				String msg = String.format(
						"Failed to format template '%s' with arguments: %s",
						key, Arrays.toString(strings));

				throw new IllegalArgumentException(msg, e);
			}
		} else {
			return "!" + key;
		}
	}

	/**
	 * Callback invoked in the constructor, after the database is fully initialized.
	 * Subclasses can overwrite it to insert custom behavior.
	 */
	protected void afterDatabaseConnection(Database database) {}
}
