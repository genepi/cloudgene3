package cloudgene.mapred.cli;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.util.GitHubException;

import java.util.List;

public class InstallApplication extends BaseTool {

	private String cmd = "cloudgene";

	public InstallApplication(String[] args) {
		super(args);
	}

	@Override
	public void createParameters() {

	}

	@Override
	public int run() {

		if (args.length != 1) {
			System.out.println("Usage: " + cmd + " install <filename|url|github> ");
			System.out.println();
			System.exit(1);
		}

		String url = args[0];

		try {

			PluginManager pluginManager = PluginManager.getInstance();
			pluginManager.initPlugins(settings);

			System.out.println("Installing application " + url + "...");

			List<Application> applications = repository.install(url);

			if (!applications.isEmpty()) {
				settings.save();
				for (Application application: applications) {
					printlnInGreen("[OK] Application '" + application.getWdlApp().getName() + "' installed.");
				}
				System.out.println();
				return 0;
			} else {
				printlnInRed("[ERROR] No valid application found.\n");
				return 1;
			}
		} catch (GitHubException e) {
			printlnInRed("[ERROR] " + e.getMessage() + ".\n");
			return 1;
		} catch (Exception e) {
			printlnInRed("[ERROR] Application not installed:" + e.toString() + "\n");
			return 1;

		}
	}
}