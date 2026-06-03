package cloudgene.mapred.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import cloudgene.mapred.jobs.workspace.IWorkspace;
import org.junit.jupiter.api.Test;

import cloudgene.mapred.test.TestApplication;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.UserDao;
import cloudgene.mapred.jobs.workspace.WorkspaceFactory;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.wdl.WdlApp;
import cloudgene.mapred.wdl.WdlReader;
import genepi.io.FileUtil;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;

// TODO(Marc): This seems to be a misnomer. This class seems to ignore
//             PriorityThreadPoolExecutor and instead tests WorkflowEngine. We should
//             rename the existing WorkflowEngineTest to whatever it's actually
//             testing, and rename this class to WorkflowEngineTest.

@MicronautTest
public class PriorityThreadPoolExecutorTest {

	private static final int WAIT_FOR_CANCEL = 1_100;

	@Inject
	WorkspaceFactory workspaceFactory;

	@Test
	public void testCancelRunningJob() throws Exception {
		TestApplication application = new TestApplication();
		WorkflowEngine engine = application.getWorkflowEngine();

		WdlApp wdlApp = WdlReader.loadAppFromFile("test-data/long-sleep.yaml");

		Map<String, String> inputs = Map.of("input", "input-file");

		List<AbstractJob> jobsBeforeSubmit = engine.getAllJobsInLongTimeQueue();
		assertEquals(0, jobsBeforeSubmit.size());

		AbstractJob job1 = createJobFromWdl(application, wdlApp, "job_running", inputs);
		engine.submit(job1);

		while (job1.getState() == AbstractJob.STATE_WAITING) {
			Thread.sleep(100);
		}

		assertEquals(AbstractJob.STATE_RUNNING, job1.getState());

		List<AbstractJob> jobsAfterSubmit = engine.getAllJobsInLongTimeQueue();
		assertEquals(1, jobsAfterSubmit.size());

		engine.cancel(job1);

		while (job1.getState() == AbstractJob.STATE_RUNNING) {
			Thread.sleep(100);
		}

		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());

		application.stop(null);
	}

	@Test
	public void testCancelWaitingJob() throws Exception {
		TestApplication application = new TestApplication();
		WorkflowEngine engine = application.getWorkflowEngine();

		WdlApp wdlApp = WdlReader.loadAppFromFile("test-data/long-sleep.yaml");

		Map<String, String> inputs = Map.of("input", "input-file");

		List<AbstractJob> jobsBeforeSubmit = engine.getAllJobsInLongTimeQueue();
		assertEquals(0, jobsBeforeSubmit.size());

		AbstractJob job1 = createJobFromWdl(application, wdlApp, "job_running_a", inputs);
		engine.submit(job1);

		while (job1.getState() == AbstractJob.STATE_WAITING) {
			Thread.sleep(100);
		}

		AbstractJob job2 = createJobFromWdl(application, wdlApp, "job_waiting_b", inputs);
		engine.submit(job2);

		Thread.sleep(1_000);

		assertEquals(AbstractJob.STATE_RUNNING, job1.getState());
		assertEquals(AbstractJob.STATE_WAITING, job2.getState());

		List<AbstractJob> jobsAfterSubmit = engine.getAllJobsInLongTimeQueue();
		assertEquals(2, jobsAfterSubmit.size());

		engine.cancel(job2);
		while (job2.getState() == AbstractJob.STATE_RUNNING) {
			Thread.sleep(100);
		}

		assertEquals(AbstractJob.STATE_CANCELED, job2.getState());

		List<AbstractJob> jobsAfterCancel = engine.getAllJobsInLongTimeQueue();
		assertEquals(1, jobsAfterCancel.size());

		// clear queue
		engine.cancel(job1);
		application.stop(null);
	}

	/**
	 * Submits 4 jobs, cancels job by jobs, checks states, priority and queue
	 * position
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultipleJobs() throws Exception {
		TestApplication application = new TestApplication();
		WorkflowEngine engine = application.getWorkflowEngine();

		WdlApp wdlApp = WdlReader.loadAppFromFile("test-data/long-sleep.yaml");

		Map<String, String> inputs = Map.of("input", "input-file");

		AbstractJob job1 = createJobFromWdl(application, wdlApp, "job1", inputs);
		engine.submit(job1);

		Thread.sleep(500);

		AbstractJob job2 = createJobFromWdl(application, wdlApp, "job2", inputs);
		engine.submit(job2);

		Thread.sleep(500);

		AbstractJob job3 = createJobFromWdl(application, wdlApp, "job3", inputs);
		engine.submit(job3);

		Thread.sleep(500);

		AbstractJob job4 = createJobFromWdl(application, wdlApp, "job4", inputs);
		engine.submit(job4);

		Thread.sleep(500);

		assertEquals(AbstractJob.STATE_RUNNING, job1.getState());
		assertEquals(AbstractJob.STATE_WAITING, job2.getState());
		assertEquals(0, job2.getPositionInQueue());
		assertEquals(AbstractJob.STATE_WAITING, job3.getState());
		assertEquals(1, job3.getPositionInQueue());
		assertEquals(AbstractJob.STATE_WAITING, job4.getState());
		assertEquals(2, job4.getPositionInQueue());

		assertTrue(job1.getPriority() < job2.getPriority() && job2.getPriority() < job3.getPriority()
				&& job3.getPriority() < job4.getPriority());

		assertEquals(4, engine.getAllJobsInLongTimeQueue().size());

		// check if all jobs are sorted by priority
		List<AbstractJob> jobs = engine.getAllJobsInLongTimeQueue();
		assertEquals(0, jobs.indexOf(job1));
		assertEquals(1, jobs.indexOf(job2));
		assertEquals(2, jobs.indexOf(job3));
		assertEquals(3, jobs.indexOf(job4));

		engine.cancel(job1);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(3, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_RUNNING, job2.getState());
		assertEquals(AbstractJob.STATE_WAITING, job3.getState());
		assertEquals(0, job3.getPositionInQueue());
		assertEquals(AbstractJob.STATE_WAITING, job4.getState());
		assertEquals(1, job4.getPositionInQueue());

		engine.cancel(job2);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(2, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job2.getState());
		assertEquals(AbstractJob.STATE_RUNNING, job3.getState());
		assertEquals(AbstractJob.STATE_WAITING, job4.getState());
		assertEquals(0, job4.getPositionInQueue());

		engine.cancel(job3);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(1, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job2.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job3.getState());
		assertEquals(AbstractJob.STATE_RUNNING, job4.getState());

		engine.cancel(job4);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(0, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job2.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job3.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job4.getState());

		application.stop(null);
	}

	/**
	 * Submits 4 jobs, job3 has small priority cancels job by jobs, checks
	 * states, priority and queue position
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultipleJobsWithPriority() throws Exception {
		TestApplication application = new TestApplication();
		WorkflowEngine engine = application.getWorkflowEngine();

		WdlApp wdlApp = WdlReader.loadAppFromFile("test-data/long-sleep.yaml");

		Map<String, String> inputs = Map.of("input", "input-file");

		AbstractJob job1 = createJobFromWdl(application, wdlApp, "job1_a", inputs);
		engine.submit(job1);

		Thread.sleep(500);

		AbstractJob job2 = createJobFromWdl(application, wdlApp, "job2_a", inputs);
		engine.submit(job2);

		Thread.sleep(500);

		AbstractJob job3 = createJobFromWdl(application, wdlApp, "job3_a", inputs);
		engine.submit(job3);

		Thread.sleep(500);

		AbstractJob job4 = createJobFromWdl(application, wdlApp, "job4_a", inputs);
		engine.submit(job4, 0);

		Thread.sleep(500);

		assertEquals(AbstractJob.STATE_RUNNING, job1.getState());
		assertEquals(AbstractJob.STATE_WAITING, job2.getState());
		assertEquals(1, job2.getPositionInQueue());
		assertEquals(AbstractJob.STATE_WAITING, job3.getState());
		assertEquals(2, job3.getPositionInQueue());
		assertEquals(AbstractJob.STATE_WAITING, job4.getState());
		assertEquals(0, job4.getPositionInQueue());

		assertTrue(job1.getPriority() < job2.getPriority() && job2.getPriority() < job3.getPriority()
				&& job4.getPriority() < job2.getPriority() && job4.getPriority() < job3.getPriority());

		assertEquals(4, engine.getAllJobsInLongTimeQueue().size());

		// check if all jobs are sorted by priority
		List<AbstractJob> jobs = engine.getAllJobsInLongTimeQueue();
		assertEquals(0, jobs.indexOf(job1));
		assertEquals(2, jobs.indexOf(job2));
		assertEquals(3, jobs.indexOf(job3));
		assertEquals(1, jobs.indexOf(job4));

		engine.cancel(job1);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(3, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_WAITING, job2.getState());
		assertEquals(0, job2.getPositionInQueue());
		assertEquals(AbstractJob.STATE_WAITING, job3.getState());
		assertEquals(1, job3.getPositionInQueue());
		assertEquals(AbstractJob.STATE_RUNNING, job4.getState());

		engine.cancel(job4);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(2, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_RUNNING, job2.getState());
		assertEquals(AbstractJob.STATE_WAITING, job3.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job4.getState());
		assertEquals(0, job3.getPositionInQueue());

		engine.cancel(job2);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(1, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job2.getState());
		assertEquals(AbstractJob.STATE_RUNNING, job3.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job4.getState());

		engine.cancel(job3);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(0, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job2.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job3.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job4.getState());

		application.stop(null);
	}

	/**
	 * Submits 4 jobs, job3 has small priority cancels job by jobs, checks
	 * states, priority and queue position
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultipleJobsAndUpdatePriority() throws Exception {
		TestApplication application = new TestApplication();
		WorkflowEngine engine = application.getWorkflowEngine();

		WdlApp wdlApp = WdlReader.loadAppFromFile("test-data/long-sleep.yaml");

		Map<String, String> inputs = Map.of("input", "input-file");

		AbstractJob job1 = createJobFromWdl(application, wdlApp, "job1_ab", inputs);
		engine.submit(job1);

		Thread.sleep(500);

		AbstractJob job2 = createJobFromWdl(application, wdlApp, "job2_ab", inputs);
		engine.submit(job2);

		Thread.sleep(500);

		AbstractJob job3 = createJobFromWdl(application, wdlApp, "job3_ab", inputs);
		engine.submit(job3);

		Thread.sleep(500);

		// submit with lowest priority
		AbstractJob job4 = createJobFromWdl(application, wdlApp, "job4_ab", inputs);
		engine.submit(job4);

		Thread.sleep(500);

		assertEquals(AbstractJob.STATE_RUNNING, job1.getState());
		assertEquals(AbstractJob.STATE_WAITING, job2.getState());
		assertEquals(0, job2.getPositionInQueue());
		assertEquals(AbstractJob.STATE_WAITING, job3.getState());
		assertEquals(1, job3.getPositionInQueue());
		assertEquals(AbstractJob.STATE_WAITING, job4.getState());
		assertEquals(2, job4.getPositionInQueue());

		assertTrue(job1.getPriority() < job2.getPriority()
				&& job2.getPriority() < job3.getPriority()
				&& job3.getPriority() < job4.getPriority());

		assertEquals(4, engine.getAllJobsInLongTimeQueue().size());

		// check if all jobs are sorted by priority
		List<AbstractJob> jobs = engine.getAllJobsInLongTimeQueue();
		assertEquals(0, jobs.indexOf(job1));
		assertEquals(1, jobs.indexOf(job2));
		assertEquals(2, jobs.indexOf(job3));
		assertEquals(3, jobs.indexOf(job4));

		// update priority to highest priority
		engine.updatePriority(job4, 0);

		Thread.sleep(5000);

		assertEquals(AbstractJob.STATE_RUNNING, job1.getState());
		assertEquals(AbstractJob.STATE_WAITING, job2.getState());
		assertEquals(1, job2.getPositionInQueue());
		assertEquals(AbstractJob.STATE_WAITING, job3.getState());
		assertEquals(2, job3.getPositionInQueue());
		assertEquals(AbstractJob.STATE_WAITING, job4.getState());
		assertEquals(0, job4.getPositionInQueue());

		assertTrue(job1.getPriority() < job2.getPriority() && job2.getPriority() < job3.getPriority()
				&& job4.getPriority() < job2.getPriority() && job4.getPriority() < job3.getPriority());

		assertEquals(4, engine.getAllJobsInLongTimeQueue().size());

		// check if all jobs are sorted by priority
		jobs = engine.getAllJobsInLongTimeQueue();
		assertEquals(0, jobs.indexOf(job1));
		assertEquals(2, jobs.indexOf(job2));
		assertEquals(3, jobs.indexOf(job3));
		assertEquals(1, jobs.indexOf(job4));

		engine.cancel(job1);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(3, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_WAITING, job2.getState());
		assertEquals(0, job2.getPositionInQueue());
		assertEquals(AbstractJob.STATE_WAITING, job3.getState());
		assertEquals(1, job3.getPositionInQueue());
		assertEquals(AbstractJob.STATE_RUNNING, job4.getState());

		engine.cancel(job4);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(2, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_RUNNING, job2.getState());
		assertEquals(AbstractJob.STATE_WAITING, job3.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job4.getState());
		assertEquals(0, job3.getPositionInQueue());

		engine.cancel(job2);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(1, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job2.getState());
		assertEquals(AbstractJob.STATE_RUNNING, job3.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job4.getState());

		engine.cancel(job3);
		Thread.sleep(WAIT_FOR_CANCEL);

		assertEquals(0, engine.getAllJobsInLongTimeQueue().size());
		assertEquals(AbstractJob.STATE_CANCELED, job1.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job2.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job3.getState());
		assertEquals(AbstractJob.STATE_CANCELED, job4.getState());

		application.stop(null);
	}

	private CloudgeneJob createJobFromWdl(
			TestApplication application,
			WdlApp wdlApp,
			String id,
			Map<String, String> inputs) throws Exception {

		UserDao userDao = new UserDao(application.getDatabase());
		User user = userDao.findByUsername("user");

		Settings settings = application.getSettings();

		String localWorkspace = FileUtil.path(settings.getLocalWorkspace(), id);
		FileUtil.createDirectory(localWorkspace);

		// setup workspace
		IWorkspace workspace = workspaceFactory.getDefault();
		workspace.setJob(id);
		workspace.setup();

		CloudgeneJob job = new CloudgeneJob(user, id, wdlApp, inputs);
		job.setId(id);
		job.setName(id);
		job.setWorkspace(workspace);
		job.setLocalWorkspace(localWorkspace);
		job.setSettings(settings);
		job.setApplication(wdlApp.getName() + " " + wdlApp.getVersion());
		job.setApplicationId(wdlApp.getId());

		return job;
	}
}
