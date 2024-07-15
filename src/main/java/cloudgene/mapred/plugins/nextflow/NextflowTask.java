package cloudgene.mapred.plugins.nextflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import cloudgene.mapred.jobs.Step;
import cloudgene.mapred.plugins.nextflow.report.CommandOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.jobs.workspace.IWorkspace;
import cloudgene.mapred.plugins.nextflow.report.Report;
import cloudgene.mapred.plugins.nextflow.report.ReportEvent;
import cloudgene.mapred.plugins.nextflow.report.ReportEventExecutor;
import genepi.io.FileUtil;

public class NextflowTask {

	private int id;

	private Map<String, Object> trace;

	private String logText = null;

	private CloudgeneContext context;

	private Step step;

	private static final Logger log = LoggerFactory.getLogger(NextflowTask.class);
	
	public NextflowTask(CloudgeneContext context, Map<String, Object> trace, Step step) {
		id = (Integer) trace.get("task_id");
		this.trace = trace;
		this.context = context;
		this.step = step;
	}

	public void update(Map<String, Object> trace) throws IOException {
		this.trace = trace;

		// if task is completed or failed check if a cloudgene.log is in workdir and
		// load its content

		// TODO: check if CHACHED os also needed!
		String status = (String) trace.get("status");
		if (!status.equals("COMPLETED") && !status.equals("FAILED")) {
			return;
		}

		String workDir = (String) trace.get("workdir");

		String reportFilename = FileUtil.path(workDir, Report.DEFAULT_FILENAME);
		if (parseReport(reportFilename)) {
			return;
		}

		String logFilename = FileUtil.path(workDir, "cloudgene.log");
		if (parseCommandOutput(logFilename)){
			return;
		}

		String outputFilename = FileUtil.path(workDir, CommandOutput.DEFAULT_FILENAME);
		parseCommandOutput(outputFilename);

	}

	private boolean parseCommandOutput(String reportFilename) throws IOException {
		IWorkspace workspace = context.getJob().getWorkspace();
		if (!workspace.exists(reportFilename)) {
			return false;
		}
		context.log("Load process output file from '" + reportFilename + "'");
		InputStream stream = context.getWorkspace().download(reportFilename);
		try {
			CommandOutput report = new CommandOutput(stream);
			for (ReportEvent event : report.getEvents()) {
				ReportEventExecutor.execute(event, context, step);
			}
		} catch (Exception e) {
			log.error("[Job {}] Invalid report file.", context.getJobId(), e);
			logText = "Invalid report file: \n" + FileUtil.readFileAsString(stream);
		}
		return true;
	}

	private boolean parseReport(String reportFilename) throws IOException {
		IWorkspace workspace = context.getJob().getWorkspace();
		if (!workspace.exists(reportFilename)) {
			return false;
		}

		context.log("Load report file from '" + reportFilename + "'");
		InputStream stream = workspace.download(reportFilename);
		try {
			Report report = new Report(stream);
			for (ReportEvent event : report.getEvents()) {
				ReportEventExecutor.execute(event, context, step);
			}
		} catch (Exception e) {
			log.error("[Job {}] Invalid report file.", context.getJobId(), e);
			logText = "Invalid report file: \n" + FileUtil.readFileAsString(stream);
		}
		return true;
	}

	public int getId() {
		return id;
	}

	public Map<String, Object> getTrace() {
		return trace;
	}

	public String getLogText() {
		return logText;
	}

}
