package cloudgene.mapred.steps;

import java.io.File;
import java.util.List;
import java.util.Vector;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.jobs.CloudgeneStep;
import cloudgene.mapred.jobs.Message;
import cloudgene.mapred.wdl.WdlStep;

public class BashCommandStep extends CloudgeneStep {

	@Override
	public boolean run(WdlStep step, CloudgeneContext context) {

		context.createStep(step.getName());

		String cmd = step.getString("exec");
		if (cmd == null) {
			cmd = step.getString("cmd");
		}

		if (cmd == null) {
			context.error("No 'exec' or 'cmd' parameter found.");
		}

		if (cmd.isEmpty()) {
			context.error("'exec' or 'cmd' parameter cannot be an empty string.");
		}

		String bash = step.getString("bash", "false");
		String stdout = step.getString("stdout", "false");

		boolean useBash = bash.equals("true");
		boolean streamStdout = stdout.equals("true");

		String[] params = cmd.split(" ");

		File file = new File(params[0]);

		if (!file.exists()) {
			context.error("Command '" + file.getAbsolutePath()
					+ "' was not found. Please set the correct path in the cloudgene.yaml file.");
			return false;
		}

		if (!file.canExecute()) {
			context.error("Command '" + file.getAbsolutePath()
					+ "' was found but can not be executed. Please check the permissions.");
			return false;
		}

		List<String> command = new Vector<String>();

		if (useBash) {
			command.add("/bin/bash");
			command.add("-c");
		}

		String bashCommand = "";
		for (String param : params) {

			if (useBash) {
				bashCommand += param + " ";
			} else {
				command.add(param);
			}

		}
		if (useBash) {
			command.add(bashCommand);
		}

		StringBuilder output = null;
		if (streamStdout) {
			output = new StringBuilder();
		}

		try {
			context.beginTask("Running Command...");
			boolean successful = executeCommand(command, context, output);
			if (successful) {
				if (streamStdout) {
					context.endTask(output.toString(), Message.OK);
				} else {
					context.endTask("Execution successful.", Message.OK);
				}
				return true;
			} else {
				if (streamStdout) {
					context.endTask(output.toString(), Message.ERROR);
				} else {
					context.endTask(
							"Execution failed. Please contact the server administrators for help if you believe this job should have completed successfully.",
							Message.ERROR);
				}
				return false;
			}
		} catch (Exception e) {
			context.log("Execution failed.", e);
			return false;
		}

	}

}
