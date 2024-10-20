package cloudgene.mapred.plugins.docker;

import java.io.File;

import cloudgene.mapred.util.BinaryFinder;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.util.command.Command;
import genepi.io.FileUtil;

public class DockerBinary {

	private String binary = "";

	public static DockerBinary build(Settings settings) {
		String binary = new BinaryFinder("docker").settings(settings, "docker", "home").env("DOCKER_HOME")
				.envPath().find();
		return new DockerBinary(binary);
	}

	private DockerBinary(String binary) {
		this.binary = binary;
	}

	public String getBinary() {
		return binary;
	}

	public boolean isInstalled() {
		if (binary != null) {
			String binary = getBinary();
			return (new File(binary)).exists();
		} else {
			return false;
		}
	}

	public String getVersion() {
		if (isInstalled()) {
			String binary = getBinary();
			Command command = new Command(binary, "version");
			StringBuffer stdout = new StringBuffer();
			command.writeStdout(stdout);
			command.setSilent(true);
			command.execute();
			return stdout.toString();
		} else {
			return "Docker not installed.";
		}
	}
}
