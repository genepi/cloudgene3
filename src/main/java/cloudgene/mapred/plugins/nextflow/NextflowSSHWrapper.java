package cloudgene.mapred.plugins.nextflow;

import cloudgene.mapred.jobs.workspace.IWorkspace;
import cloudgene.mapred.util.SSHJumper;
import cloudgene.mapred.util.Settings;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

public class NextflowSSHWrapper {


	private final NextflowBinary binary;

	private final Settings settings;

	private final IWorkspace workspace;

	public NextflowSSHWrapper(Settings settings, IWorkspace workspace, NextflowBinary binary) {
		this.binary = binary;
		this.settings = settings;
		this.workspace = workspace;
	}

	public List<String> buildCommand() throws IOException {

		//Stage params file
		File paramsFile = binary.getParamsFile();
		String remoteParamsFile = workspace.uploadInput("params",  paramsFile);
		binary.setParamsFile(new File(remoteParamsFile));


		//Stage config files
		List<File> configFiles = new Vector<>();
		int i = 0;
		for (File file: binary.getConfigFiles()) {
			String remoteConfigFile = workspace.uploadInput("config-" + (i++),  file);
			configFiles.add(new File(remoteConfigFile));
		}
		binary.setConfigFiles(configFiles);

		//Stage env files
		List<File> envScripts = new Vector<>();
		i = 0;
		for (File file: binary.getEnvScripts()) {
			String remoteEnvScript = workspace.uploadInput("env-" + (i++),  file);
			envScripts.add(new File(remoteEnvScript));
		}
		binary.setEnvScripts(envScripts);

		List<String> nextflowCommand = binary.buildCommand(false);

		// Final joined command to run
		String joinedCommand = join(nextflowCommand);
		String workDir = workspace.createTempFolder("nextflow-run");
		String fullCommand = "mkdir -p " + workDir + " && cd " + workDir + " && " + joinedCommand;

		List<String> command = new Vector<>();
		SSHJumper jumper = settings.getJumper();

		// use sshpass to provide password, and ssh to run remote command
		command.add("sshpass"); // TODO: optional
		command.add("ssh");
		command.add("-o");
		command.add("StrictHostKeyChecking=no"); // TODO: optional
		command.add(jumper.getUser() + "@" + jumper.getHost());
		command.add("bash -c \"" + fullCommand.replace("\"", "\\\"") + "\"");

		return command;

	}

	private String join(List<String> array) {
		String result = "";
		for (int i = 0; i < array.size(); i++) {
			if (i > 0) {
				result += " ";
			}
			result += array.get(i);
		}
		return result;
	}

}
