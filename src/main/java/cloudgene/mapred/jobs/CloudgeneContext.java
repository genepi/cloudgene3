package cloudgene.mapred.jobs;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.sdk.WorkflowContext;
import cloudgene.mapred.util.MailUtil;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.wdl.WdlApp;
import cloudgene.mapred.wdl.WdlParameterInputType;
import genepi.io.FileUtil;

public class CloudgeneContext extends WorkflowContext {

	private String localTemp;

	private String localOutput;

	private String workingDirectory;

	private Settings settings;

	private User user;

	private Map<String, CloudgeneParameterInput> inputParameters;

	private Map<String, CloudgeneParameterOutput> outputParameters;

	private Map<String, Integer> counters = new HashMap<String, Integer>();

	private Map<String, String> values = new HashMap<String, String>();

	private Map<String, Boolean> submitCounters = new HashMap<String, Boolean>();

	private Map<String, Boolean> submitValues = new HashMap<String, Boolean>();

	private CloudgeneJob job;

	private Map<String, Object> data = new HashMap<String, Object>();

	private Map<String, Object> config;

	private List<WdlApp> dependencies = new Vector<WdlApp>();

	private int stepCounter = 0;

	public CloudgeneContext(CloudgeneJob job) {

		this.workingDirectory = job.getWorkingDirectory();
		this.job = job;

		this.user = job.getUser();

		setData("cloudgene.user.mail", user.getMail());
		setData("cloudgene.user.name", user.getFullName());

		localOutput = new File(job.getLocalWorkspace()).getAbsolutePath();
		localTemp = new File(FileUtil.path(job.getLocalWorkspace(), "temp")).getAbsolutePath();

		inputParameters = new HashMap<String, CloudgeneParameterInput>();
		for (CloudgeneParameterInput param : job.getInputParams()) {
			inputParameters.put(param.getName(), param);
		}

		outputParameters = new HashMap<String, CloudgeneParameterOutput>();
		for (CloudgeneParameterOutput param : job.getOutputParams()) {
			outputParameters.put(param.getName(), param);
		}

		settings = job.getSettings();

	}


	public boolean resolveAppLinks() throws IOException {
		
		this.addDependency(job.getApp());

		Settings settings = getSettings();
		ApplicationRepository repository = settings.getApplicationRepository();

		// resolve application links
		for (CloudgeneParameterInput input : inputParameters.values()) {
			if (input.getType() != WdlParameterInputType.APP_LIST) {
				continue;
			}
			String value = input.getValue();
			String linkedAppId = value;
			if (value.startsWith("apps@")) {
				linkedAppId = value.replaceAll("apps@", "");
			}

			if (value.isEmpty()) {
				continue;
			}
			Application linkedApp = repository.getByIdAndUser(linkedAppId, getUser());
			if (linkedApp == null) {
				throw new IOException("Application " + linkedAppId + " is not installed or wrong permissions.");
			}

			this.addDependency(linkedApp.getWdlApp());

			// update environment variables
			Environment environment = settings.buildEnvironment().addApplication(linkedApp.getWdlApp())
					.addContext(this);
			Map<String, Object> properties = linkedApp.getWdlApp().getProperties();
			for (String property : properties.keySet()) {
				Object propertyValue = properties.get(property);
				if (propertyValue instanceof String) {
					propertyValue = environment.resolve(propertyValue.toString());
				}
				properties.put(property, propertyValue);
			}

			setData(input.getName(), properties);
			
		}

		return true;

	}

	private void addDependency(WdlApp app) {
		dependencies.add(app);
	}

	public List<WdlApp> getDependencies() {
		return dependencies;
	}

	public String getInput(String param) {

		if (inputParameters.get(param) != null) {

			return inputParameters.get(param).getValue();

		} else {
			return null;
		}
	}

	public String getJobId() {
		return job.getId();
	}

	public String getPublicJobId() {
		return job.getPublicJobId();
	}

	public String getOutput(String param) {

		if (outputParameters.get(param) != null) {

			return outputParameters.get(param).getValue();

		} else {

			return null;

		}

	}

	public String get(String param) {
		String result = getInput(param);
		if (result == null) {
			return getOutput(param);
		} else {
			return result;
		}
	}

	public Settings getSettings() {
		return settings;
	}

	public String getLocalTemp() {
		return localTemp;
	}

	public String getLocalOutput() {
		return localOutput;
	}

	public void println(String line) {
		job.writeOutputln(line);
	}

	public void log(String line, Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString(); // stack trace as a string
		job.writeLog(line + "\n" + sStackTrace);
	}

	public void log(String line) {
		job.writeLog(line);
	}

	public CloudgeneJob getJob() {
		return job;
	}

	@Override
	public String getJobName() {
		return job.getName();
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public User getUser() {
		return user;
	}

	public boolean sendMail(String subject, String body) throws Exception {
		Settings settings = getSettings();

		if (settings.getMail() != null) {

			MailUtil.send(settings.getMail().get("smtp"), settings.getMail().get("port"),
					settings.getMail().get("user"), settings.getMail().get("password"), settings.getMail().get("name"),
					user.getMail(), "[" + settings.getName() + "] " + subject, body);

		}

		return true;

	}

	public boolean sendMail(String to, String subject, String body) throws Exception {
		Settings settings = getSettings();

		if (settings.getMail() != null) {

			MailUtil.send(settings.getMail().get("smtp"), settings.getMail().get("port"),
					settings.getMail().get("user"), settings.getMail().get("password"), settings.getMail().get("name"),
					to, "[" + settings.getName() + "] " + subject, body);

		}
		return true;

	}

	@Override
	public boolean sendNotification(String text) throws Exception {
		return true;
	}

	public Set<String> getInputs() {
		return inputParameters.keySet();
	}

	public Set<String> getOutputs() {
		return outputParameters.keySet();
	}

	public void setInput(String input, String value) {

		CloudgeneParameterInput parameter = inputParameters.get(input);
		parameter.setValue(value);
	}

	public void setOutput2(String input, String value) {

		CloudgeneParameterOutput parameter = outputParameters.get(input);
		parameter.setValue(value);

	}

	public void incCounter(String name, int value) {

		Integer oldvalue = counters.get(name);
		if (oldvalue == null) {
			oldvalue = 0;
		}
		log("Increment counter " + name + " by " + value);
		counters.put(name, oldvalue + value);

	}

	public void submitCounter(String name) {
		log("Submit counter " + name);
		submitCounters.put(name, true);
	}

	public Map<String, Integer> getSubmittedCounters() {
		Map<String, Integer> result = new HashMap<String, Integer>();
		for (String counter : submitCounters.keySet()) {
			result.put(counter, counters.get(counter));
		}
		return result;
	}

	public Map<String, Integer> getCounters() {
		return counters;
	}

	public void setValue(String name, String value) {
		log("Set value " + name + " to " + value);
		values.put(name,  value);
	}

	public void submitValue(String name) {
		log("Submit value " + name);
		submitValues.put(name, true);
	}

	public Map<String, String> getSubmittedValues() {
		Map<String, String> result = new HashMap<String, String>();
		for (String name : submitValues.keySet()) {
			result.put(name, values.get(name));
		}
		return result;
	}

	public Map<String, String> getValues() {
		return values;
	}

	public String createLinkToFile(String id) {

		CloudgeneParameterOutput out = outputParameters.get(id);

		if (out != null) {

			return "<a href=\"/results/" + job.getId() + "/" + out.getName() + "/" + out.getName() + ".txt" + "\">"
					+ out.getName() + ".txt" + "</a>";

		} else {
			return "[PARAMETER UNKOWN!]";
		}

	}

	public String createLinkToFile(String id, String filename) {

		CloudgeneParameterOutput out = outputParameters.get(id);

		if (out != null) {

			return "<a href=\"/results/" + job.getId() + "/" + out.getName() + "/" + filename + "\">" + filename
					+ "</a>";

		} else {
			return "[PARAMETER UNKOWN!]";
		}

	}

	public Step getCurrentStep() {
		if (job.getSteps().isEmpty()) {
			return createStep("Steps");
		}
		return job.getSteps().get(job.getSteps().size() -1);
	}

	public void message(String message, int type) {
		message(getCurrentStep(), message, type);
	}

	public void message(Step step, String message, int type) {
		Message status = new Message(step, type, message);

		List<Message> logs = step.getLogMessages();
		if (logs == null) {
			logs = new Vector<Message>();
			step.setLogMessages(logs);
		}
		logs.add(status);

	}

	public void beginTask(String name) {
		beginTask(getCurrentStep(), name);
	}

	public void beginTask(Step step, String name) {
		Message status = new Message(step, Message.RUNNING, name);

		List<Message> logs = step.getLogMessages();
		if (logs == null) {
			logs = new Vector<Message>();
			step.setLogMessages(logs);
		}
		logs.add(status);
	}

	public Message createTask(String name) {
		return createTask(getCurrentStep(), name);
	}

	public Message createTask(Step step, String name) {
		Message status = new Message(step, Message.RUNNING, name);

		List<Message> logs = step.getLogMessages();
		if (logs == null) {
			logs = new Vector<Message>();
			step.setLogMessages(logs);
		}
		logs.add(status);
		return status;
	}

	public void beginTask(String name, int totalWork) {
		beginTask(getCurrentStep(), name);
	}

	public void beginTask(Step step, String name, int totalWork) {
		beginTask(step, name);
	}

	public void endTask(String message, int type) {
		endTask(getCurrentStep(), message, type);
	}

	public void endTask(Step step, String message, int type) {
		Message status = step.getLogMessages().get(step.getLogMessages().size() - 1);
		status.setType(type);
		status.setMessage(message);
	}

	public void updateTask(String message, int type) {
		updateTask(getCurrentStep(), message, type);
	}

	public void updateTask(Step step, String message, int type) {
		Message status = step.getLogMessages().get(step.getLogMessages().size() - 1);
		status.setType(type);
		status.setMessage(message);
	}

	public void updateTask(String message) {
		updateTask(getCurrentStep(), message);
	}

	public void updateTask(Step step, String message) {
		Message status = step.getLogMessages().get(step.getLogMessages().size() - 1);
		status.setMessage(message);
	}

	public void endTask(int type) {
		endTask(getCurrentStep(), type);
	}

	public void endTask(Step step, int type) {
		Message status = step.getLogMessages().get(step.getLogMessages().size() - 1);
		status.setType(type);
	}

	public Step createStep(String name) {
		Step outputStep = new Step();
		outputStep.setJob(job);
		outputStep.setName(name);
		job.getSteps().add(outputStep);
		return outputStep;
	}


	public Object getData(String key) {
		return data.get(key);
	}

	public void setData(String key, Object object) {
		data.put(key, object);
	}

	@Override
	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

	@Override
	public String getConfig(String param) {
		if (config != null) {
			Object value = config.get(param);
			return value != null ? value.toString() : null;
		} else {
			return null;
		}
	}

	@Override
	public String getHdfsTemp() {
		throw new RuntimeException("Not support in cg3");
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void incStepCounter(){
		stepCounter++;
	}

	public int getStepCounter() {
		return stepCounter;
	}
}
