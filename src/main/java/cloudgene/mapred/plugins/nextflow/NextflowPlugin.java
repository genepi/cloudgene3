package cloudgene.mapred.plugins.nextflow;

import cloudgene.mapred.jobs.CloudgeneStepFactory;
import cloudgene.mapred.plugins.IPlugin;
import cloudgene.mapred.util.Configuration;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.wdl.WdlApp;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;
import genepi.io.FileUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NextflowPlugin implements IPlugin {

	public static final String ID = "nextflow";
	public static final String NEXTFLOW_CONFIG = "nextflow.config";
	public static final String NEXTFLOW_YAML = "nextflow.yaml";
	public static final String NEXTFLOW_ENV = "nextflow.env";

	private Settings settings;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return "Nextflow";
	}

	@Override
	public boolean isInstalled() {
		NextflowBinary binary = NextflowBinary.build(settings);
		return binary.isInstalled();
	}

	@Override
	public String getDetails() {
		NextflowBinary binary = NextflowBinary.build(settings);
		return binary.getVersion();
	}

	@Override
	public void configure(Settings settings) {
		this.settings = settings;
		CloudgeneStepFactory factory = CloudgeneStepFactory.getInstance();
		factory.register("nextflow", NextflowStep.class);
	}

	@Override
	public String getStatus() {
		if (isInstalled()) {
			return "Nextflow support enabled.";
		} else {
			return "Nextflow Binary not found. Nextflow support disabled.";
		}
	}

	@Override
	public void updateConfig(WdlApp app, Map<String, String> config) throws IOException {

		String appFolder = settings.getApplicationRepository().getConfigDirectory(app);
		FileUtil.createDirectory(appFolder);

		String nextflowConfig = FileUtil.path(appFolder, NEXTFLOW_CONFIG);
		String content = config.get("nextflow.config");
		StringBuffer contentNextflowConfig = new StringBuffer(content == null ? "" : content);
		FileUtil.writeStringBufferToFile(nextflowConfig, contentNextflowConfig);

		String nextflowProperties = FileUtil.path(appFolder, NEXTFLOW_YAML);
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("profile", config.get("nextflow.profile"));
		properties.put("work", config.get("nextflow.work"));

		YamlWriter writer = new YamlWriter(new FileWriter(nextflowProperties));
		writer.write(properties);
		writer.close();

		String nextflowEnv = FileUtil.path(appFolder, NEXTFLOW_ENV);
		String contentEnv = config.get("nextflow.env");
		StringBuffer contentNextflowEnv = new StringBuffer(contentEnv == null ? "" : contentEnv);
		FileUtil.writeStringBufferToFile(nextflowEnv, contentNextflowEnv);
	}

	@Override
	public Map<String, String> getConfig(WdlApp app) throws IOException {

		String appFolder = settings.getApplicationRepository().getConfigDirectory(app);

		Map<String, String> config = new HashMap<String, String>();

		String nextflowConfig = FileUtil.path(appFolder, NEXTFLOW_CONFIG);
		if (new File(nextflowConfig).exists()) {
			String content = FileUtil.readFileAsString(nextflowConfig);
			config.put("nextflow.config", content);
		}

		String nextflowProperties = FileUtil.path(appFolder, NEXTFLOW_YAML);
		File file = new File(nextflowProperties);
		if (!file.exists()) {
			return config;
		}

		YamlReader reader = new YamlReader(new FileReader(nextflowProperties));
		Map properties = reader.read(Map.class);
		reader.close();

		if (properties.get("profile") != null) {
			config.put("nextflow.profile", properties.get("profile").toString());
		}
		if (properties.get("work") != null) {
			config.put("nextflow.work", properties.get("work").toString());
		}

		String nextflowEnv = FileUtil.path(appFolder, NEXTFLOW_ENV);
		if (new File(nextflowEnv).exists()) {
			String content = FileUtil.readFileAsString(nextflowEnv);
			config.put("nextflow.env", content);
		}

		return config;

	}

	public String getNextflowConfig() {
		return FileUtil.path(Configuration.getConfigDirectory(), "nextflow.config");
	}

}
