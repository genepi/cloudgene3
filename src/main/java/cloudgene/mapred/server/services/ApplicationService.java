package cloudgene.mapred.server.services;

import java.io.IOException;
import java.util.*;

import cloudgene.mapred.plugins.IPlugin;
import cloudgene.mapred.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.core.User;
import cloudgene.mapred.server.exceptions.JsonHttpStatusException;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.wdl.WdlApp;
import io.micronaut.http.HttpStatus;
import jakarta.inject.Inject;

public class ApplicationService {

	private static final String UNDER_MAINTENANCE = "This functionality is currently under maintenance.";
	private static final String HADOOP_CLUSTER_UNREACHABLE = "Hadoop cluster seems unreachable or misconfigured. Hadoop support is disabled, but this application requires it.";
	private static final String APPLICATION_IS_DATA_PACKAGE = "Application %s is a data package.";
	private static final String APPLICATION_NOT_REMOVED = "Application not removed: %s";
	private static final String APPLICATION_NOT_FOUND = "Application %s not found or the request requires user authentication.";
	private static final String APPLICATION_NOT_UPDATED = "Application not updated: %s";
	private static final String APPLICATION_NOT_INSTALLED = "Application not installed. %s";
	private static final String NO_URL = "No url or file location set.";
	private static final String APPLICATION_NOT_INSTALLED_NO_WORKFLOW = "Application not installed: No workflow file found.";

	private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

	@Inject
	protected cloudgene.mapred.server.Application application;

	public Application getById(String appId) {

		ApplicationRepository repository = application.getSettings().getApplicationRepository();
		Application app = repository.getById(appId);

		if (app == null) {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, String.format(APPLICATION_NOT_FOUND, appId));
		}

		return app;

	}

	public Application getByIdAndUser(User user, String appId) {

		Settings settings = application.getSettings();

		if (settings.isMaintenance() && (user == null || !user.isAdmin())) {
			throw new JsonHttpStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNDER_MAINTENANCE);
		}

		ApplicationRepository repository = settings.getApplicationRepository();
		Application app = repository.getByIdAndUser(appId, user);

		if (app == null) {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, String.format(APPLICATION_NOT_FOUND, appId));
		}

		return app;

	}

	public void checkRequirements(Application app) {

		WdlApp wdlApp = app.getWdlApp();

		if (wdlApp.getWorkflow() == null) {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND,
					String.format(APPLICATION_IS_DATA_PACKAGE, app.getId()));
		}

	}

	public Application removeApp(String appId) {

		ApplicationRepository repository = this.application.getSettings().getApplicationRepository();
		Application app = repository.getById(appId);

		if (app != null) {
			try {
				repository.remove(app);
				this.application.getSettings().save();
				return app;

			} catch (Exception e) {
				throw new JsonHttpStatusException(HttpStatus.BAD_REQUEST,
						String.format(APPLICATION_NOT_REMOVED, e.getMessage()));
			}
		} else {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, String.format(APPLICATION_NOT_FOUND, appId));
		}

	}

	public void enableApp(Application app, boolean enabled) {

		ApplicationRepository repository = this.application.getSettings().getApplicationRepository();
		app.setEnabled(enabled);
		repository.reload();
		this.application.getSettings().save();

	}

	public void updatePermissions(Application app, String permission) {

		ApplicationRepository repository = this.application.getSettings().getApplicationRepository();
		if (app == null || permission == null) {
			return;
		}
		if (!app.getPermission().equals(permission)) {
			app.setPermission(permission);
			repository.reload();
			this.application.getSettings().save();
		}

	}

	public void updateConfig(Application app, Map<String, String> config) throws IOException {

		ApplicationRepository repository = this.application.getSettings().getApplicationRepository();
		WdlApp wdlApp = app.getWdlApp();

		if (config == null) {
			return;
		}

		for (IPlugin plugin: PluginManager.getInstance().getPlugins()) {
			Map<String, String> updatedConfig = plugin.getConfig(wdlApp);
			if (updatedConfig == null) {
				continue;
			}
			for (String key: config.keySet()){
				updatedConfig.put(key, config.get(key));
			}
			plugin.updateConfig(wdlApp, updatedConfig);
		}

	}

	public List<Application> listApps(boolean reload) {

		ApplicationRepository repository = application.getSettings().getApplicationRepository();

		if (reload) {
			repository.reload();
		}

		List<Application> applications = new Vector<Application>(repository.getAll());
		Collections.sort(applications, new Comparator<Application>() {
			@Override
			public int compare(Application o1, Application o2) {
					return o1.getWdlApp().getName().compareTo(o2.getWdlApp().getName());
			}
		});
		return applications;
	}

	public Application installApp(String url) {

		if (url == null) {
			throw new JsonHttpStatusException(HttpStatus.BAD_REQUEST, NO_URL);
		}

		ApplicationRepository repository = application.getSettings().getApplicationRepository();

		try {

			Application app = repository.install(url);
			application.getSettings().save();

			if (app != null) {
				return app;
			} else {
				throw new JsonHttpStatusException(HttpStatus.BAD_REQUEST, APPLICATION_NOT_INSTALLED_NO_WORKFLOW);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(APPLICATION_NOT_INSTALLED, e);
			throw new JsonHttpStatusException(HttpStatus.BAD_REQUEST,
					String.format(APPLICATION_NOT_INSTALLED, e.getMessage()));

		} catch (Error e) {
			e.printStackTrace();
			log.error(APPLICATION_NOT_INSTALLED, e);
			throw new JsonHttpStatusException(HttpStatus.BAD_REQUEST,
					String.format(APPLICATION_NOT_INSTALLED, e.getMessage()));
		}

	}

	public ApplicationRepository getRepository() {
		return this.application.getSettings().getApplicationRepository();
	}
}
