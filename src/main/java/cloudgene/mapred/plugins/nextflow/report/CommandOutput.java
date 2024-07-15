package cloudgene.mapred.plugins.nextflow.report;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.plugins.nextflow.report.GitHubActionsParser.Command;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class CommandOutput {

	public static final String DEFAULT_FILENAME = ".command.out";

	private List<ReportEvent> events = new Vector<ReportEvent>();

	public CommandOutput() {

	}

	public CommandOutput(String filename) throws IOException {
		loadFromFile(filename);
	}

	public CommandOutput(InputStream in) throws IOException {
		loadFromInputStream(in);
	}

	public void loadFromFile(String filename) throws IOException {
		loadFromInputStream(new FileInputStream(filename));
	}

	public void loadFromInputStream(InputStream in) throws IOException {
		GitHubActionsParser parser = new GitHubActionsParser();
		List<Command> commands = parser.parseOutput(in);
		for (Command command : commands) {
			switch(command.getName()){
				case "error":
					addEvent(ReportEvent.WebCommand.MESSAGE, command.getParameters().get("value"), (double)CloudgeneContext.ERROR);
					break;
				case "warning":
					addEvent(ReportEvent.WebCommand.MESSAGE, command.getParameters().get("value"),  (double)CloudgeneContext.WARNING);
					break;
				case "message":
				case "notice":
					addEvent(ReportEvent.WebCommand.MESSAGE, command.getParameters().get("value"),  (double)CloudgeneContext.OK);
					break;
				case "log":
					addEvent(ReportEvent.WebCommand.LOG, command.getParameters().get("value"));
					break;
				case "debug":
					addEvent(ReportEvent.WebCommand.PRINTLN, command.getParameters().get("value"));
					break;
				case "set-counter":
					addEvent(ReportEvent.WebCommand.INC_COUNTER, command.getParameters().get("name"), Double.valueOf(Integer.parseInt(command.getParameters().get("value"))));
					break;
				case "submit-counter":
					addEvent(ReportEvent.WebCommand.SUBMIT_COUNTER, command.getParameters().get("name"));
					break;
				default:
					throw new IOException("Unknown command: " + command.getName());
			}
			System.out.println("Command: " + command.getName());
			for (Map.Entry<String, String> param : command.getParameters().entrySet()) {
				System.out.println("  " + param.getKey() + ": " + param.getValue());
			}
		}
	}

	public List<ReportEvent> getEvents() {
		return events;
	}

	public void addEvent(ReportEvent.WebCommand command, Object... params) {
		ReportEvent event = new ReportEvent(command, params);
		events.add(event);
		// TODO: autosave?
	}

}
