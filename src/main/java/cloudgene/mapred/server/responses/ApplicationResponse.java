package cloudgene.mapred.server.responses;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.jobs.Environment;
import cloudgene.mapred.plugins.IPlugin;
import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.wdl.WdlApp;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import genepi.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonClassDescription
public class ApplicationResponse {

	private String id = "";

	private boolean enabled = false;

	private String filename = "";

	private String name;

	private String version;

	private boolean loaded = false;

	private String errorMessage = "";

	private boolean changed = false;

	private String permission = "";

	private String source = "";

	private String category = "";

	private boolean workflow = true;

	private List<Environment.Variable> environment;

	private Map<String, String> config;

	private static final Logger log = LoggerFactory.getLogger(ApplicationResponse.class);

	public static ApplicationResponse build(Application app) {

		ApplicationResponse appResponse = new ApplicationResponse();
		appResponse.setId(app.getId());
		appResponse.setName(app.getWdlApp().getName());
		appResponse.setVersion(app.getWdlApp().getVersion());
		appResponse.setEnabled(app.isEnabled());
		appResponse.setFilename(app.getFilename());
		appResponse.setLoaded(app.isLoaded());
		appResponse.setErrorMessage(app.getErrorMessage());
		appResponse.setPermission(app.getPermission());
		appResponse.setWorkflow(app.getWdlApp().getWorkflow() != null);
		appResponse.setCategory(app.getWdlApp().getCategory());

		if (new File(app.getFilename()).exists()) {
			appResponse.setSource(FileUtil.readFileAsString(app.getFilename()));
		}

		return appResponse;

	}

	public static ApplicationResponse buildWithDetails(Application app, Settings settings,
			ApplicationRepository repository) {

		ApplicationResponse appResponse = build(app);

		List<Environment.Variable> environment = settings.buildEnvironment().addApplication(app.getWdlApp()).toList();
		appResponse.setEnvironment(environment);

		Map<String, String> config = new HashMap<String, String>();
		for (IPlugin plugin: PluginManager.getInstance().getPlugins()) {
			try {
				Map<String, String> pluginConfig = plugin.getConfig(app.getWdlApp());
				if (pluginConfig == null) {
					continue;
				}
				config.putAll(pluginConfig);
			}catch (IOException e) {
				log.warn("Loading configuration for plugin '" + plugin.getName() +"' and application '" + app.getWdlApp().getId() + "' failed.",  e);
			}
		}
		appResponse.setConfig(config);

		return appResponse;

	}

	public static List<ApplicationResponse> buildWithDetails(List<Application> applications, Settings settings,
			ApplicationRepository repository) {
		List<ApplicationResponse> response = new Vector<ApplicationResponse>();
		for (Application app : applications) {
			response.add(ApplicationResponse.build(app));
		}
		return response;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	public void setWorkflow(boolean workflow) {
		this.workflow = workflow;
	}

	public boolean isWorkflow() {
		return workflow;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public List<Environment.Variable> getEnvironment() {
		return environment;
	}

	public void setEnvironment(List<Environment.Variable> environment) {
		this.environment = environment;
	}

	public Map<String, String> getConfig() {
		return config;
	}

	public void setConfig(Map<String, String> config) {
		this.config = config;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCategory() {
		return category;
	}
}
