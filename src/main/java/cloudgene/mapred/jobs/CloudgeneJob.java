package cloudgene.mapred.jobs;

import java.io.IOException;
import java.util.*;

import cloudgene.mapred.util.GlobUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.engine.ExecutableStep;
import cloudgene.mapred.jobs.engine.Executor;
import cloudgene.mapred.jobs.engine.Planner;
import cloudgene.mapred.wdl.WdlApp;
import cloudgene.mapred.wdl.WdlParameterInput;
import cloudgene.mapred.wdl.WdlParameterOutput;
import cloudgene.mapred.wdl.WdlParameterOutputType;
import cloudgene.mapred.wdl.WdlStep;

public class CloudgeneJob extends AbstractJob {

	private static final String CLOUDGENE_LOGS_PARAM = "cloudgene_logs";

	private WdlApp app;

	private String workingDirectory;

	private Executor executor;

	public static final int MAX_DOWNLOAD = 10;

	private static Logger log = LoggerFactory.getLogger(CloudgeneJob.class);

	public CloudgeneJob() {
		super();
	}

	public CloudgeneJob(User user, String id, WdlApp app, Map<String, String> params) {
		this.app = app;
		setId(id);
		log.warn("set_id_cloudgenejob");
		setUser(user);
		log.warn("set_user");
		workingDirectory = app.getPath();

		// init parameters
		inputParams = new Vector<CloudgeneParameterInput>();
		for (WdlParameterInput input : app.getWorkflow().getInputs()) {
			CloudgeneParameterInput newInput = new CloudgeneParameterInput(input);
			newInput.setJob(this);

			if (params.containsKey(input.getId())) {
				newInput.setValue(params.get(input.getId()));
			}

			inputParams.add(newInput);
		}
		log.warn("outputParams_cloudgene_job");
		outputParams = new Vector<CloudgeneParameterOutput>();
		for (WdlParameterOutput output : app.getWorkflow().getOutputs()) {
			CloudgeneParameterOutput newOutput = new CloudgeneParameterOutput(output);
			newOutput.initHash();
			newOutput.setJob(this);
			outputParams.add(newOutput);
			outputParamsIndex.put(output.getId(), newOutput);
		}
		log.warn("initLogOutput");

		initLogOutput();
		log.warn("finish clougene+job");

	}

	public void loadApp(WdlApp app) {

		this.app = app;
		workingDirectory = app.getPath();

		// find logOutput and remove from outputs
		for (CloudgeneParameterOutput param : getOutputParams()) {
			if (!param.getName().equals(CLOUDGENE_LOGS_PARAM)) {
				continue;
			}
			logOutput = param;
		}
		if (logOutput != null) {
			getOutputParams().remove(logOutput);
		}

	}

	protected void initLogOutput() {
		logOutput = new CloudgeneParameterOutput();
		logOutput.setAdminOnly(true); //!getSettings().isShowLogs());
		logOutput.setDownload(true);
		logOutput.setDescription("Logs");
		logOutput.setName(CLOUDGENE_LOGS_PARAM);
		logOutput.setType(WdlParameterOutputType.LOCAL_FOLDER);
		logOutput.setJob(this);
		logOutput.initHash();
	}

	@Override
	public boolean setup() throws Exception {

		context = new CloudgeneContext(this);
		context.resolveAppLinks();

		try {
			log.info("[Job {}] Setup workspace {}'", getId(), workspace.getName());
			context.log("Setup External Workspace on " + workspace.getName());
			workspace.setup();
			context.setWorkspace(workspace);
		} catch (Exception e) {
			writeLog(e.toString());
			log.error("[Job {}] Error setup external workspace failed.", getId(), e);
			setError(e.toString());
			return false;
		}

		// create output directories
		for (CloudgeneParameterOutput param : outputParams) {

			switch (param.getType()) {
			case LOCAL_FILE:
				String filename = workspace.createFile(param.getName(), param.getName());
				param.setValue(filename);
				log.info("[Job {}] Set output file '{}' to '{}'", getId(), param.getName(), param.getValue());
				break;

			case LOCAL_FOLDER:
				String folder = workspace.createFolder(param.getName());
				param.setValue(folder);
				log.info("[Job {}] Set output folder '{}' to '{}'", getId(), param.getName(), param.getValue());
				break;

			case WEBPAGE:
				String webpageFolder = workspace.createFolder(param.getName());
				param.setValue(webpageFolder);
				log.info("[Job {}] Set webpage output folder '{}' to '{}'", getId(), param.getName(), param.getValue());
				break;
			}

		}

		return true;

	}

	@Override
	public boolean execute() {

		try {

			// evaluate WDL and replace all variables (e.g. ${job_id})
			Planner planner = new Planner();
			WdlApp app = planner.evaluateWDL(this.app, context, getSettings());

			// merge setup steps and normal steps
			List<WdlStep> steps = new Vector<WdlStep>(app.getWorkflow().getSetups());
			steps.addAll(app.getWorkflow().getSteps());
			log.info("[Job {}] execute {} steps", getId(), steps.size());

			// execute steps
			executor = new Executor();
			ExecutionResult result = executor.execute(steps, context);

			if (result != ExecutionResult.SUCCESS) {
				setError("Job Execution failed.");
				executeFailureStep();
				return false;
			}

			context.setValue("application", getApp().getId());
			context.submitValue("application");

			setError(null);
			return true;

		} catch (Exception e) {
			writeOutput(e.getMessage());
			setError(e.getMessage());
			log.error("[Job {}] execution failed.", getId(), e);
			return false;
		}

	}

	@Override
	public boolean onFailure() {

		after();

		cleanUp();

		return true;
	}

	public boolean executeFailureStep() {

		WdlStep step = app.getWorkflow().getOnFailure();
		cleanUp();

		if (step == null) {
			return true;
		}

		try {
			writeLog("Executing onFailure... ");
			ExecutableStep node = new ExecutableStep(step, context);
			ExecutionResult result = node.run();
			if (result == ExecutionResult.SUCCESS) {
				writeLog("onFailure execution successful.");
				return true;
			} else {
				writeLog("onFailure execution failed.");
				return false;
			}

		} catch (Exception e) {
			writeLog("onFailure execution failed.");
			writeLog(e.getMessage());
			setError(e.getMessage());
			return false;
		}

	}

	@Override
	public boolean cleanUp() {

		log.info("[Job {}] Cleaning up...", getId());

		try {
			workspace.cleanup(getId());
		} catch (IOException e) {
			writeLog("Cleanup failed.");
			writeLog(e.getMessage());
			setError(e.getMessage());
			log.error("[Job {}] Clean up failed.", getId(), e);
			return false;
		}

		return true;
	}

	@Override
	public boolean after() {

		log.info("[Job {}] Export parameters...", getId());
		try {
			for (WdlParameterOutput output : getApp().getWorkflow().getOutputs()) {
				if (output.isDownload()) {
					CloudgeneParameterOutput out = outputParamsIndex.get(output.getId());
					exportParameter(out, output.getIncludes(), output.getExcludes());
				}
			}

			log.info("[Job {}] Export logs...", getId());
			List<Download> logs = workspace.getLogs();
			for (Download log : logs) {
				log.setCount(-1);
			}
			logOutput.setFiles(logs);
		} catch (IOException e) {
			log.error("[Job {}] Export parameters failed.", getId(), e);
			return false;
		}
		return true;
	}

	public void exportParameter(CloudgeneParameterOutput out,List<String> includes, List<String> excludes) throws IOException {

		writeLog("  Exporting parameter " + out.getName() + "...");
		out.setJobId(getId());
		List<Download> downloads = workspace.getDownloads(out.getValue());
		for (Download download : downloads) {
			download.setParameter(out);
			download.setCount(MAX_DOWNLOAD);

			// check if it is on excludes or not in includes
			if (!GlobUtil.isFileIncluded(download.getName(), includes, excludes)){
				writeLog("  Ignore download " + download.getName() + ".");
				continue;
			}

			if (!out.getFiles().contains(download)) {
				out.getFiles().add(download);
				writeLog("  Added new download " + download.getName() + ".");
			} else {
				writeLog("  Download " + download.getName() + " already added.");
			}
		}
		Collections.sort(out.getFiles());
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public WdlApp getApp() {
		return app;
	}

	@Override
	public void kill() {
		if (executor != null) {
			executor.kill();
		}
	}

	public void updateProgress() {

		if (executor != null) {
			executor.updateProgress();
		}

	}

}
