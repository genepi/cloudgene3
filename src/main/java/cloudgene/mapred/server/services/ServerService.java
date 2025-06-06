package cloudgene.mapred.server.services;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import cloudgene.mapred.plugins.nextflow.NextflowPlugin;
import cloudgene.mapred.util.command.Command;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.core.Template;
import cloudgene.mapred.core.User;
import cloudgene.mapred.plugins.IPlugin;
import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.server.controller.ServerAdminController;
import cloudgene.mapred.util.Settings;
import genepi.io.FileUtil;
import io.micronaut.security.oauth2.configuration.OauthClientConfigurationProperties;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ServerService {

	private static final Logger log = LoggerFactory.getLogger(ServerService.class);

	public static final String IMAGE_DATA = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"96\" height=\"20\">"
			+ "	<linearGradient id=\"b\" x2=\"0\" y2=\"100%\"><stop offset=\"0\" stop-color=\"#bbb\" stop-opacity=\".1\"/><stop offset=\"1\" stop-opacity=\".1\"/></linearGradient>"
			+ "	<mask id=\"a\"><rect width=\"96\" height=\"20\" rx=\"3\" fill=\"#fff\"/></mask>"
			+ "	<g mask=\"url(#a)\"><path fill=\"#555\" d=\"M0 0h55v20H0z\"/><path fill=\"#97CA00\" d=\"M55 0h41v20H55z\"/><path fill=\"url(#b)\" d=\"M0 0h96v20H0z\"/></g>"
			+ "	<g fill=\"#fff\" text-anchor=\"middle\" font-family=\"DejaVu Sans,Verdana,Geneva,sans-serif\" font-size=\"11\">"
			+ "		<text x=\"27.5\" y=\"15\" fill=\"#010101\" fill-opacity=\".3\">version</text>"
			+ "		<text x=\"27.5\" y=\"14\">version</text>"
			+ "		<text x=\"74.5\" y=\"15\" fill=\"#010101\" fill-opacity=\".3\">" + Application.VERSION + "</text>"
			+ "		<text x=\"74.5\" y=\"14\">" + Application.VERSION + "</text>" + "	</g>" + "</svg>";

	@Inject
	protected Application application;

	@Inject
	protected List<OauthClientConfigurationProperties> clients;

	public String getRoot(User user) {

		ObjectMapper mapper = new ObjectMapper();

		ObjectNode data = mapper.createObjectNode();
		data.put("name", application.getSettings().getName());
		data.put("background", application.getSettings().getColors().get("background"));
		data.put("foreground", application.getSettings().getColors().get("foreground"));
		data.put("footer", application.getTemplate(Template.FOOTER));
		data.put("emailRequired", application.getSettings().isEmailRequired());
		data.put("userEmailDescription", application.getTemplate(Template.USER_EMAIL_DESCRIPTION));
		data.put("userWithoutEmailDescription", application.getTemplate(Template.USER_WITHOUT_EMAIL_DESCRIPTION));

		List<String> authClients = new Vector<String>();
		for (OauthClientConfigurationProperties client : clients) {
			authClients.add(client.getName());
		}
		data.putPOJO("oauth", authClients);

		if (user != null) {
			ObjectNode userJson = mapper.createObjectNode();
			userJson.put("username", user.getUsername());
			userJson.put("mail", user.getMail());
			userJson.put("admin", user.isAdmin());
			userJson.put("name", user.getFullName());
			data.set("user", userJson);

			ApplicationRepository repository = application.getSettings().getApplicationRepository();
			List<cloudgene.mapred.apps.Application> apps = repository.getAllByUser(user, ApplicationRepository.APPS);
			data.putPOJO("apps", apps);

			List<ObjectNode> appsJson = new Vector<ObjectNode>();
			List<ObjectNode> deprecatedAppsJson = new Vector<ObjectNode>();
			List<ObjectNode> experimentalAppsJson = new Vector<ObjectNode>();

			for (cloudgene.mapred.apps.Application app : apps) {
				ObjectNode appJson = mapper.createObjectNode();
				appJson.put("id", app.getId());
				appJson.put("name", app.getWdlApp().getName());
				appJson.put("version", app.getWdlApp().getVersion());
				if (app.getWdlApp().getRelease() == null) {
					appsJson.add(appJson);
				} else if (app.getWdlApp().getRelease().equals("deprecated")) {
					deprecatedAppsJson.add(appJson);
				} else if (app.getWdlApp().getRelease().equals("experimental")) {
					experimentalAppsJson.add(appJson);
				} else {
					appsJson.add(appJson);
				}
			}

			data.putPOJO("apps", appsJson);
			data.putPOJO("deprecatedApps", deprecatedAppsJson);
			data.putPOJO("experimentalApps", experimentalAppsJson);
			data.put("loggedIn", true);

		} else {
			data.putPOJO("apps", new Vector<ObjectNode>());
			data.put("loggedIn", false);
		}

		data.putPOJO("navigation", application.getSettings().getNavigation());
		if (application.getSettings().isMaintenance()) {
			data.put("maintenace", true);
			data.put("maintenaceMessage", application.getTemplate(Template.MAINTENANCE_MESSAGE));
		} else {
			data.put("maintenace", false);
		}

		return data.toString();
	}

	public void updateSettings(String name, String adminName, String adminMail, String serverUrl, String baseUrl, String background_color, String foreground_color, String google_analytics,
			String mail, String mail_smtp, String mail_port, String mail_user, String mail_password, String mail_name, String workspaceType, String workspaceLocation) {

		Settings settings = application.getSettings();
		settings.setName(name);
		settings.setAdminName(adminName);
		settings.setAdminMail(adminMail);
		settings.setServerUrl(serverUrl);
		settings.setBaseUrl(baseUrl);
		settings.getColors().put("background", background_color);
		settings.getColors().put("foreground", foreground_color);
		settings.setGoogleAnalytics(google_analytics);
		settings.getExternalWorkspace().put("type", workspaceType);
		settings.getExternalWorkspace().put("location", workspaceLocation);
		
		if (mail != null && mail.equals("true")) {
			Map<String, String> mailConfig = new HashMap<String, String>();
			mailConfig.put("smtp", mail_smtp);
			mailConfig.put("port", mail_port);
			mailConfig.put("user", mail_user);
			mailConfig.put("password", mail_password);
			mailConfig.put("name", mail_name);
			application.getSettings().setMail(mailConfig);
		} else {
			application.getSettings().setMail(null);
		}

		application.getSettings().save();

	}

	public String getClusterDetails() {

		ObjectMapper mapper = new ObjectMapper();

		ObjectNode object = mapper.createObjectNode();

		// general settings
		object.put("maintenance", application.getSettings().isMaintenance());
		object.put("blocked", !application.getWorkflowEngine().isRunning());
		object.put("version", Application.VERSION);
		object.put("maintenance", application.getSettings().isMaintenance());
		object.put("blocked", !application.getWorkflowEngine().isRunning());
		object.put("threads", application.getSettings().getThreadsQueue());
		object.put("max_jobs_user", application.getSettings().getMaxRunningJobsPerUser());
		try {
			URL url = ServerAdminController.class.getClassLoader().getResource("META-INF/MANIFEST.MF");
			Manifest manifest = new Manifest(url.openStream());
			Attributes attr = manifest.getMainAttributes();
			String buildVesion = attr.getValue("Version");
			String buildTime = attr.getValue("Build-Time");
			String builtBy = attr.getValue("Built-By");
			object.put("built_by", builtBy);
			object.put("built_time", buildTime);

		} catch (IOException E) {
			object.put("built_by", "Development");
			object.put("built_time", new Date().toGMTString());
		}

		// workspace and hdd
		File workspace = new File(application.getSettings().getLocalWorkspace());
		object.put("workspace_path", workspace.getAbsolutePath());
		object.put("free_disc_space", workspace.getUsableSpace() / 1024 / 1024 / 1024);
		object.put("total_disc_space", workspace.getTotalSpace() / 1024 / 1024 / 1024);
		object.put("used_disc_space",
				(workspace.getTotalSpace() / 1024 / 1024 / 1024) - (workspace.getUsableSpace() / 1024 / 1024 / 1024));

		// plugins
		PluginManager manager = PluginManager.getInstance();

		ArrayNode plugins = object.putArray("plugins");

		for (IPlugin plugin : manager.getPlugins()) {
			ObjectNode pluginObject = mapper.createObjectNode();
			pluginObject.put("name", plugin.getName());

			if (plugin.isInstalled()) {
				pluginObject.put("enabled", true);
				pluginObject.put("details", plugin.getDetails());
			} else {
				pluginObject.put("enabled", false);
				pluginObject.put("error", plugin.getStatus());
			}
			plugins.add(pluginObject);
		}

		//check user defined resources
		for (Map<String, String> resource: application.getSettings().getResources()){
			String name = resource.get("name");
			ObjectNode pluginObject = mapper.createObjectNode();
			pluginObject.put("name", name);
			if (!resource.containsKey("command")) {
				pluginObject.put("error", "Command defined in resource '" + name + "'");
			}
			String cmd = resource.get("command");
			String[] tiles = cmd.split(" ");
			String[] params = Arrays.copyOfRange(tiles, 1, tiles.length);
			Command command = new Command(tiles[0], params);
			command.setSilent(true);
			StringBuffer output = new StringBuffer();
			StringBuffer error = new StringBuffer();
			command.writeStdout(output);
			command.writeStderr(error);
			int exitCode = command.execute();
			if (exitCode == 0) {
				pluginObject.put("enabled", true);
				pluginObject.put("details", output.toString());
			}else {
				pluginObject.put("enabled", false);
				pluginObject.put("error",  output.toString()+ "\n" + error.toString());
			}
			plugins.add(pluginObject);
		}

		// database
		object.put("db_max_active", application.getDatabase().getDataSource().getMaxActive());
		object.put("db_active", application.getDatabase().getDataSource().getNumActive());
		object.put("db_max_idle", application.getDatabase().getDataSource().getMaxIdle());
		object.put("db_idle", application.getDatabase().getDataSource().getNumIdle());
		object.put("db_max_open_prep_statements",
				application.getDatabase().getDataSource().getMaxOpenPreparedStatements());

		return object.toString();
	}

	public void updateNextflowConfig(String content) {
		NextflowPlugin plugin = (NextflowPlugin) PluginManager.getInstance().getPlugin(NextflowPlugin.ID);
		String filename = plugin.getNextflowConfig();
		FileUtil.writeStringBufferToFile(filename, new StringBuffer(content));
	}

	public void updateNextflowEnv(String content) {
		NextflowPlugin plugin = (NextflowPlugin) PluginManager.getInstance().getPlugin(NextflowPlugin.ID);
		String filename = plugin.getNextflowEnv();
		FileUtil.writeStringBufferToFile(filename, new StringBuffer(content));
	}

}
