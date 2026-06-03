package cloudgene.mapred.jobs;

import cloudgene.mapred.test.TestApplication;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.UserDao;
import cloudgene.mapred.jobs.workspace.IWorkspace;
import cloudgene.mapred.jobs.workspace.WorkspaceFactory;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.test.TestMailServer;
import cloudgene.mapred.wdl.WdlApp;
import cloudgene.mapred.wdl.WdlReader;
import com.dumbster.smtp.SmtpMessage;
import genepi.io.FileUtil;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class CloudgeneContextTest {

	@Inject
	TestApplication application;

	@Inject
	WorkspaceFactory workspaceFactory;

	@Test
	public void testConstructor() {
		CloudgeneContext context = makeContext();

		assertEquals("user@example.com", context.getData("cloudgene.user.mail"));
		assertEquals("User User", context.getData("cloudgene.user.name"));
		assertEquals("user", context.getUser().getUsername());
		assertTrue(context.getLocalOutput().endsWith(context.getJob().getLocalWorkspace()));
		assertTrue(context.getLocalTemp().endsWith("temp"));
		assertFalse(context.getInputs().isEmpty());
		assertFalse(context.getOutputs().isEmpty());
		assertEquals(application.getSettings(), context.getSettings());
	}

	@Test
	public void testGetInput() {
		CloudgeneContext context = makeContext();
		String value;

		value = context.getInput("404-not-found");
		assertNull(value);

		value = context.getInput("input"); // ID from return-true workflow
		assertEquals("input-file", value); // We set it in makeJob()
	}

	@Test
	public void testGetOutput() {
		CloudgeneContext context = makeContext();
		String value;

		value = context.getOutput("404-not-found");
		assertNull(value);

		value = context.getOutput("output"); // ID from return-true workflow
		assertEquals("", value); // Apparently it's not set
	}

	@Test
	public void testGet() {
		CloudgeneContext context = makeContext();
		String value;

		value = context.get("404-not-found");
		assertNull(value);

		value = context.get("input"); // Input ID from return-true workflow
		assertEquals("input-file", value); // We set it in makeJob()

		value = context.get("output"); // Output ID from return-true workflow
		assertEquals("", value); // Apparently it's not set
	}

	@Test
	public void testGetInputs() {
		CloudgeneContext context = makeContext();
		Set<String> inputs = context.getInputs();

		assertFalse(inputs.isEmpty());
		assertTrue(inputs.contains("input")); // ID of the only input in return-true.
	}

	@Test
	public void testSendMail() {
		TestMailServer mailServer = TestMailServer.getInstance();
		mailServer.start();

		CloudgeneContext context = makeContext();

		int mailsBefore = mailServer.getReceivedEmailSize();

		try {
			context.sendMail("noreply@cloudgene", "Test", "I am a test!");
		} catch (Exception e) {
			fail();
			return;
		}

		int mailsAfter = mailServer.getReceivedEmailSize();
		assertEquals(mailsBefore + 1, mailsAfter); // Exactly one email received

		List<SmtpMessage> allEmails = mailServer.getReceivedEmailAsList();
		SmtpMessage mail = allEmails.get(allEmails.size() - 1);

		// [ settings.name ] injected by sendMail(). Defaults to "Cloudgene".
		assertEquals("[Cloudgene] Test", mail.getHeaderValue("Subject"));
		assertEquals("I am a test!", mail.getBody());
	}

	@Test
	public void testIncCounter() {
		CloudgeneContext context = makeContext();
		Map<String, Integer> counters;

		counters = context.getCounters();
		assertEquals(0, counters.size());

		context.incCounter("foo", 42);

		counters = context.getCounters();
		assertEquals(1, counters.size());
		assertEquals(42, counters.get("foo"));

		context.incCounter("bar", 13);

		counters = context.getCounters();
		assertEquals(2, counters.size());
		assertEquals(42, counters.get("foo"));
		assertEquals(13, counters.get("bar"));

		context.incCounter("foo", 57);

		counters = context.getCounters();
		assertEquals(2, counters.size());
		assertEquals(99, counters.get("foo"));
		assertEquals(13, counters.get("bar"));
	}

	@Test
	public void testSubmitCounter() {
		// NOTE(Marc): This doesn't seem to be correctly designed. There is no relation
		//             between what goes into counters and submitCounters, except
		//             getSubmittedCounters kind of assumes the submitCounters keys are a
		//             subset of the counters keys (as one would expect, but we don't
		//             enforce).

		CloudgeneContext context = makeContext();
		Map<String, Integer> counters;
		Map<String, Integer> submittedCounters;

		counters = context.getCounters();
		submittedCounters = context.getSubmittedCounters();
		assertEquals(0, counters.size());
		assertEquals(0, submittedCounters.size());

		context.submitCounter("hello");

		counters = context.getCounters();
		submittedCounters = context.getSubmittedCounters();
		assertEquals(0, counters.size());
		assertEquals(1, submittedCounters.size());
		assertTrue(submittedCounters.containsKey("hello"));
		assertNull(submittedCounters.get("hello"));

		context.incCounter("foo", 42);

		counters = context.getCounters();
		submittedCounters = context.getSubmittedCounters();
		assertEquals(1, counters.size());
		assertEquals(1, submittedCounters.size());
		assertEquals(42, counters.get("foo"));
		assertTrue(submittedCounters.containsKey("hello"));
		assertNull(submittedCounters.get("hello"));

		context.submitCounter("foo");

		counters = context.getCounters();
		submittedCounters = context.getSubmittedCounters();
		assertEquals(1, counters.size());
		assertEquals(2, submittedCounters.size());
		assertEquals(42, counters.get("foo"));
		assertTrue(submittedCounters.containsKey("hello"));
		assertNull(submittedCounters.get("hello"));
		assertEquals(42, submittedCounters.get("foo"));
	}

	private CloudgeneJob makeJob() {
		try {
			// User: user; pwd: admin1978
			UserDao userDao = new UserDao(application.getDatabase());
			User user = userDao.findByUsername("user");

			String id = "test_" + System.currentTimeMillis();

			WdlApp app = WdlReader.loadAppFromFile("test-data/return-true.yaml");

			Map<String, String> params = new HashMap<>();
			params.put("input", "input-file");

			CloudgeneJob job = new CloudgeneJob(user, id, app, params);

			IWorkspace workspace = workspaceFactory.getDefault();
			workspace.setJob(id);
			workspace.setup();

			Settings settings = application.getSettings();
			String localWorkspace = FileUtil.path(settings.getLocalWorkspace(), id);

			job.setName(id);
			job.setWorkspace(workspace);
			job.setLocalWorkspace(localWorkspace);
			job.setSettings(settings);
			job.setApplication(app.getName() + " " + app.getVersion());
			job.setApplicationId(app.getId());

			return job;
		} catch (IOException e) {
			fail();
			return new CloudgeneJob();
		}
	}

	private CloudgeneContext makeContext() {
		CloudgeneJob job = makeJob();
		CloudgeneContext context = new CloudgeneContext(job);
		return context;
	}
}
