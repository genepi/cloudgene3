package cloudgene.mapred.plugins.nextflow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import cloudgene.mapred.jobs.*;
import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.plugins.nextflow.report.CommandOutput;
import cloudgene.mapred.util.MapValueParser;
import cloudgene.mapred.wdl.WdlParameterInput;
import cloudgene.mapred.wdl.WdlParameterInputType;
import cloudgene.mapred.wdl.WdlParameterOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.workspace.IWorkspace;
import cloudgene.mapred.plugins.nextflow.report.Report;
import cloudgene.mapred.plugins.nextflow.report.ReportEvent;
import cloudgene.mapred.plugins.nextflow.report.ReportEventExecutor;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.wdl.WdlStep;
import genepi.io.FileUtil;
import groovy.json.JsonOutput;

public class NextflowStep extends CloudgeneStep {

	private static final String PROPERTY_PROCESS_CONFIG = "processes";

	private static final String PROPERTY_GROUPS_CONFIG = "groups";

	private CloudgeneContext context;

	private Map<String, Message> messages = new HashMap<String, Message>();

	private Map<String, NextflowProcessConfig> configs = new HashMap<String, NextflowProcessConfig>();

	private NextflowCollector collector = NextflowCollector.getInstance();

	private static final Logger log = LoggerFactory.getLogger(NextflowStep.class);

	@Override
	public boolean run(WdlStep step, CloudgeneContext context) {

		this.context = context;

		Settings settings = context.getSettings();

		String script = step.getString("script");

		if (script == null) {
			// no script set. try to find a main.nf. IS the default convention of nf-core.
			script = "main.nf";
		}

		String scriptPath = script;
		if (!script.startsWith("/")) {
			scriptPath = FileUtil.path(context.getWorkingDirectory(), script);
		}
		if (!new File(scriptPath).exists()) {
			context.log(
					"Warning: Nextflow script '" + scriptPath + "' not found. Try to resolve it on github as '" + script + "'");
			scriptPath = script;
		}

		String revision = step.getString("revision");

		// load process styling
		loadProcessConfigs(step.get(PROPERTY_PROCESS_CONFIG));
		Map<String, Step> groups = loadGroups(step.get(PROPERTY_GROUPS_CONFIG));
		if (groups.isEmpty()) {
			context.createStep(step.getName());
		}
		for (NextflowProcessConfig config: configs.values()) {
			if (config.getGroup() != null) {
				config.setStep(groups.get(config.getGroup()));
			}
		}

		NextflowBinary nextflow = NextflowBinary.build(settings);
		nextflow.setScript(scriptPath);
		nextflow.setRevision(revision);

		AbstractJob job = context.getJob();
		String appFolder = settings.getApplicationRepository().getConfigDirectory(job.getApplicationId());

		NextflowPlugin plugin = (NextflowPlugin) PluginManager.getInstance().getPlugin(NextflowPlugin.ID);

		Map<String, String> nextflowSettings = null;
		try {
			nextflowSettings = plugin.getConfig(context.getJob().getApp());
		} catch (Exception e) {
			e.printStackTrace();
		}
		// set profile
		String profile = nextflowSettings.get("nextflow.profile");
		nextflow.setProfile(profile);

		// set global configuration
		String globalConfig = plugin.getNextflowConfig();
		nextflow.addConfig(globalConfig);

		// set application specific configuration
		String appConfig = FileUtil.path(appFolder, "nextflow.config");
		nextflow.addConfig(appConfig);

		// set work directory
		IWorkspace workspace = job.getWorkspace();

		// use workdir if set in settings
		String work = nextflowSettings.get("nextflow.work");
		if (work != null && !work.trim().isEmpty()) {
			nextflow.setWork(work);
		} else {
			String workDir = workspace.createTempFolder("nextflow");
			nextflow.setWork(workDir);
		}


		String prefix = "step" + context.getStepCounter() + "-";

		// params json file
		String paramsJsonFilename = FileUtil.path(context.getLocalOutput(),prefix + "params.json");
		File paramsFile = new File(paramsJsonFilename);
		try {
			Map<String, Object> params = createParamsMap(step);
			writeParamsJson(params, paramsFile);
			context.log("Wrote params to file '" + paramsJsonFilename + "'");

		} catch (IOException e) {
			log.error("[Job {}] Writing params.json file failed.", context.getJobId(), e);
			return false;
		}
		nextflow.setParamsFile(paramsFile);

		// register job in webcollector and set created url
		String collectorUrl = collector.addContext(context, configs);
		nextflow.setWeblog(collectorUrl);

		// log files and reports
		nextflow.setTrace(workspace.createLogFile(prefix + "trace.csv"));
		nextflow.setReport(workspace.createLogFile(prefix + "report.html"));
		nextflow.setTimeline(workspace.createLogFile(prefix + "timeline.html"));
		nextflow.setLog(workspace.createLogFile(prefix + "nextflow.log"));
		nextflow.setName(job.getId() +  "-step-" + context.getStepCounter());

		try {

			File executionDir = new File(context.getLocalOutput());

			StringBuilder output = new StringBuilder();
			boolean successful = executeCommand(nextflow.buildCommand(), context, output, executionDir);

			if (!successful) {

				// set all running processes to failed
				List<NextflowProcess> processes = collector.getProcesses(context);
				for (NextflowProcess process : processes) {
					for (NextflowTask task : process.getTasks()) {
						String status = (String) task.getTrace().get("status");

						if (status.equals("RUNNING") || status.equals("SUBMITTED")) {
							task.getTrace().put("status", "KILLED");
						}
					}
				}
				updateProgress();

				if (killed) {
					context.error("Pipeline execution canceled.");
				} else {
					context.error("Pipeline execution failed.");
					Object stdout = step.get("stdout");
					if (stdout != null && stdout.toString().equalsIgnoreCase("true")) {
						context.error(output.toString());
					}
				}

			}

			updateProgress();

			collector.cleanProcesses(context);

			File report = new File(executionDir, Report.DEFAULT_FILENAME);
			if (report.exists()) {
				context.log("Load report file from '" + report.getCanonicalPath() + "'");
				try {
					parseReport(report);
				} catch (Exception e) {
					log.error("[Job {}] Invalid report file.", context.getJobId(), e);
				}
			}

			File outputFile = new File(executionDir, "cloudgene.out");
			if (!outputFile.exists()) {
				return successful;
			}

			context.log("Load output file from '" + outputFile.getCanonicalPath() + "'");
			try {
				parseOutput(outputFile);
			} catch (Exception e) {
				log.error("[Job {}] Invalid output file.", context.getJobId(), e);
			}

			return successful;

		} catch (Exception e) {
			log.error("[Job {}] Running nextflow script failed.", context.getJobId(), e);
			return false;
		}

	}

	private void parseReport(File file) throws IOException {
		Report report = new Report(file.getAbsolutePath());
		context.log("Execute " + report.getEvents().size() + " events.");
		for (ReportEvent event : report.getEvents()) {
			context.log("Event: " + event);
			ReportEventExecutor.execute(event, context, context.getCurrentStep());
		}
	}

	private void parseOutput(File file) throws IOException {
		CommandOutput report = new CommandOutput(file.getAbsolutePath());
		context.log("Execute " + report.getEvents().size() + " events.");
		for (ReportEvent event : report.getEvents()) {
			context.log("Event: " + event);
			ReportEventExecutor.execute(event, context, context.getCurrentStep());
		}
	}

	@Override
	public void updateProgress() {

		List<NextflowProcess> processes = collector.getProcesses(context);

		for (NextflowProcess process : processes) {

			NextflowProcessConfig config = getNextflowProcessConfig(process);

			Message message = messages.get(process.getName());
			if (message == null) {
				Step step = config.getStep();
				if (step == null) {
					message = context.createTask("<b>" + process.getName() + "</b>");
				} else {
					message = context.createTask(step, "<b>" + process.getName() + "</b>");
				}
				messages.put(process.getName(), message);
			}

			NextflowProcessRenderer.render(config, process, message);

		}

	}

	private void loadProcessConfigs(Object map) {
		if (map != null) {
			List<Map<String, Object>> processConfigs = (List<Map<String, Object>>) map;
			for (Map<String, Object> processConfig : processConfigs) {
				String process = processConfig.get("process").toString();
				NextflowProcessConfig config = new NextflowProcessConfig();
				if (processConfig.get("view") != null) {
					config.setView(processConfig.get("view").toString());
				}
				if (processConfig.get("label") != null) {
					config.setLabel(processConfig.get("label").toString());
				}
				if (processConfig.get("group") != null) {
					config.setGroup(processConfig.get("group").toString());
				}
				configs.put(process, config);
			}
		}
	}

	private Map<String, Step> loadGroups(Object map) {
		Map<String, Step> groups = new HashMap<String, Step>();
		if (map != null) {
			List<Map<String, Object>> groupConfigs = (List<Map<String, Object>>) map;
			for (Map<String, Object> groupConfig : groupConfigs) {
				String id = groupConfig.get("id").toString();
				String label = groupConfig.get("label").toString();
				Step step = context.createStep(label);
				groups.put(id, step);
			}
		}
		return groups;
	}

	private NextflowProcessConfig getNextflowProcessConfig(NextflowProcess process) {
		NextflowProcessConfig config = configs.get(process.getName());
		return config != null ? config : new NextflowProcessConfig();
	}

	@Override
	public String[] getRequirements() {
		return new String[] { NextflowPlugin.ID };
	}

	private Map<String, Object> createParamsMap(WdlStep step) {
		Map<String, Object> params = new HashMap<String, Object>();

		// used to defined hard coded params
		if (step.get("params") != null) {
			Map<String, Object> paramsMap = (Map<String, Object>) step.get("params");
			params.putAll(MapValueParser.parseMap(paramsMap));
		}

		// add all inputs
		for (WdlParameterInput param: context.getJob().getApp().getWorkflow().getInputs()) {
			String name = param.getId();
			String value = context.getInput(name);
			if (!param.isSerialize()) {
				continue;
			}
			// resolve app links: use all properties as input parameters
			if (param.getTypeAsEnum() == WdlParameterInputType.APP_LIST) {
				Map<String, Object> linkedApp = (Map<String, Object>) context.getData(name);
				if (linkedApp != null) {
					params.put(name, MapValueParser.parseMap(linkedApp));
				}
			} else {
				params.put(name, MapValueParser.guessType(value));
			}

		}

		// add all outputs
		for (WdlParameterOutput param: context.getJob().getApp().getWorkflow().getOutputs()) {
			String name = param.getId();
			String value = context.getOutput(name);
			if (!param.isSerialize()) {
				continue;
			}
			params.put(name, value);
		}

		return params;

	}

	protected void writeParamsJson(Map<String, Object> params, File paramsFile) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(paramsFile));
		writer.write(JsonOutput.prettyPrint(JsonOutput.toJson(params)));
		writer.close();
	}

}
