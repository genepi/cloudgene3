package cloudgene.mapred.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import cloudgene.mapred.database.dao.*;
import cloudgene.mapred.jobs.engine.handler.IJobErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.util.Database;

public class PersistentWorkflowEngine extends WorkflowEngine {

	private static final Logger log = LoggerFactory.getLogger(PersistentWorkflowEngine.class);

	private Database database;

	private JobDao dao;

	private CounterDao counterDao;

	private Map<String, Long> counters;

	private List<IJobErrorHandler> handlers = new Vector<IJobErrorHandler>();

	public PersistentWorkflowEngine(Database database, int ltqThreads) {
		super(ltqThreads);
		this.database = database;

		log.info("Init Counters....");

		counterDao = new CounterDao(database);
		counters = counterDao.getAll();

		dao = new JobDao(database);

		List<AbstractJob> deadJobs = dao.findAllByState(AbstractJob.STATE_WAITING);
		deadJobs.addAll(dao.findAllByState(AbstractJob.STATE_RUNNING));
		deadJobs.addAll(dao.findAllByState(AbstractJob.STATE_EXPORTING));

		for (AbstractJob job : deadJobs) {
			log.info("lost control over job " + job.getId() + " -> Dead");
			job.setState(AbstractJob.STATE_DEAD);
			dao.update(job);
		}

	}

	@Override
	protected void statusUpdated(AbstractJob job) {
		super.statusUpdated(job);
		dao.update(job);
	}

	@Override
	protected void jobCompleted(AbstractJob job) {
		super.jobCompleted(job);

		DownloadDao downloadDao = new DownloadDao(database);

		for (CloudgeneParameterOutput parameter : job.getOutputParams()) {

			if (parameter.isDownload()) {

				if (parameter.getFiles() != null) {

					for (Download download : parameter.getFiles()) {
						download.setParameter(parameter);
						downloadDao.insert(download);
					}

				}

			}

		}

		if (job.getLogOutput().getFiles() != null) {

			for (Download download : job.getLogOutput().getFiles()) {
				download.setParameter(job.getLogOutput());
				downloadDao.insert(download);
			}

		}

		if (job.getSteps() != null) {
			StepDao dao2 = new StepDao(database);
			for (Step step : job.getSteps()) {
				dao2.insert(step);

				MessageDao messageDao = new MessageDao(database);
				if (step.getLogMessages() != null) {
					for (Message logMessage : step.getLogMessages()) {
						messageDao.insert(logMessage);
					}
				}

			}
		}

		// count all runs when counter was not set by application
		Map<String, Integer> submittedCounters = job.getContext().getSubmittedCounters();
		if (!submittedCounters.containsKey("runs")) {
			if (job.getState() == AbstractJob.STATE_SUCCESS) {
				submittedCounters.put("runs", 1);
			}
		}

		// write all submitted counters into database
		for (String name : submittedCounters.keySet()) {
			Integer value = submittedCounters.get(name);

			if (value != null) {

				Long counterValue = counters.get(name);
				if (counterValue == null) {
					counterValue = 0L + value;
				} else {
					counterValue = counterValue + value;
				}
				counters.put(name, counterValue);

				counterDao.insert(name, value, job);

			}
		}

		// write all submitted values into database
		JobValueDao jobValueDao = new JobValueDao(database);
		Map<String, String> submittedValues = job.getContext().getSubmittedValues();
		for (String name : submittedValues.keySet()) {
			String value = submittedValues.get(name);

			if (value != null) {
				jobValueDao.insert(name, value, job);
			}
		}

		// update job updates (state, endtime, ....)
		dao.update(job);

		if (job.getState() == AbstractJob.STATE_FAILED) {
			for (IJobErrorHandler handler: handlers) {
				handler.handle(this, job);
			}
		}

	}

	@Override
	protected void jobSubmitted(AbstractJob job) {
		super.jobSubmitted(job);
		dao.insert(job);

		ParameterDao dao = new ParameterDao(database);

		for (CloudgeneParameterInput parameter : job.getInputParams()) {
			parameter.setJobId(job.getId());
			dao.insert(parameter);
		}

		for (CloudgeneParameterOutput parameter : job.getOutputParams()) {
			parameter.setJobId(job.getId());
			dao.insert(parameter);
		}

		dao.insert(job.getLogOutput());
	}

	@Override
	public Map<String, Long> getCounters(int state, List<String> names) {
		if (state == AbstractJob.STATE_SUCCESS) {
			List<String> keys = (names == null) ? counters.keySet().stream().toList() : names;
			Map<String, Long> counters = new HashMap<>();
			for (String name: keys) {
				counters.put(name, this.counters.get(name));
			}
			return counters;
		} else {
			return super.getCounters(state, null);
		}
	}

	public void addJobErrorHandler(IJobErrorHandler handler) {
		this.handlers.add(handler);
	}

}
