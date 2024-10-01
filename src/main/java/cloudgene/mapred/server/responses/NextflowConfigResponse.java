package cloudgene.mapred.server.responses;

import java.io.File;
import java.util.List;
import java.util.Vector;

import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.plugins.nextflow.NextflowPlugin;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;

import cloudgene.mapred.jobs.Environment.Variable;
import cloudgene.mapred.util.Settings;
import genepi.io.FileUtil;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonClassDescription
public class NextflowConfigResponse {

	private String config = "";

	private String env = "";

	private List<Variable> variables = new Vector<Variable>();

	public static NextflowConfigResponse build(Settings settings) {

		NextflowPlugin plugin = (NextflowPlugin) PluginManager.getInstance().getPlugin(NextflowPlugin.ID);

		NextflowConfigResponse response = new NextflowConfigResponse();
		String configFilename = plugin.getNextflowConfig();
		File configFile = new File(configFilename);
		if (configFile.exists()) {
			response.setConfig(FileUtil.readFileAsString(configFilename));
		}

		String envFilename = plugin.getNextflowEnv();
		File envFile = new File(envFilename);
		if (envFile.exists()) {
			response.setEnv(FileUtil.readFileAsString(envFilename));
		}
		response.setVariables(settings.buildEnvironment().toList());
		return response;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public String getConfig() {
		return config;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public String getEnv() {
		return env;
	}

	public void setVariables(List<Variable> variables) {
		this.variables = variables;
	}

	public List<Variable> getVariables() {
		return variables;
	}

}
