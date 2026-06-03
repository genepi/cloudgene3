package cloudgene.mapred.server.services;

import cloudgene.mapred.test.TestApplication;
import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.CloudgeneJob;
import cloudgene.mapred.jobs.workspace.WorkspaceFactory;
import cloudgene.mapred.server.exceptions.JsonHttpStatusException;
import cloudgene.mapred.util.FormUtil;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class JobServiceTest {

	TestApplication application;
	WorkspaceFactory workspaceFactory;
	UserService userService;
	JobService jobService;

	// NOTE(Marc): Micronaut pushes the notion of favoring (single-core) speed over
	// correctness by sharing an application instance across tests. This is al well
	// and good until we try to test things like the limit of concurrent jobs per
	// user, and then suddenly we get different behavior depending on which
	// combination of tests is run together. Here I'm recreating the application for
	// each test. It's significantly slower, but at least it behaves correctly.
	// Perhaps a better solution would be to use mocks.
	@BeforeEach
	public void setup() throws SQLException {
		application = new TestApplication();
		workspaceFactory = new WorkspaceFactory(application);
		userService = new UserService(application);
		jobService = new JobService(application, workspaceFactory);
	}

	@Test
	public void testCreateId() {
		String id = jobService.createId();

		assertNotNull(id);
		assertTrue(id.startsWith("job-"));

		// NOTE(Marc): I tested creating two in a row and comparing, and we usually get
		// 1ms difference (the smallest resolution available), but it seems like the
		// kind of flaky comparison that would cause occasional test failures.
	}

	@Test
	public void testSubmitJob() {
		User user = userService.getByUsername("user");
		User admin = userService.getByUsername("admin");

		// null user -> exception thrown
		assertThrows(
				JsonHttpStatusException.class,
				() -> jobService.submitJob("return-true-step-public\"", List.of(), null, "agent"));

		// Normal user but unrecognized application -> exception thrown.
		assertThrows(JsonHttpStatusException.class,
				() -> jobService.submitJob("this-application-does-not-exist", List.of(), user, "agent"));

		// Normal user and installed application, but missing 'workflow'
		// block in application definition -> exception thrown.
		assertThrows(JsonHttpStatusException.class,
				() -> jobService.submitJob("no-workflow", List.of(), user, "agent"));

		// Normal user and installed application -> works fine.
		AbstractJob job = jobService.submitJob("return-true-step-public", List.of(), user, "agent");

		// On successful submission, we always get a valid AbstractJob back.
		assertNotNull(job);

		// ID automatically set to job-<timestamp>
		assertNotNull(job.getId());
		assertTrue(job.getId().startsWith("job-"));

		// Name defaults to ID
		assertEquals(job.getId(), job.getName());

		// Application ID matches the expected value.
		assertEquals("return-true-step-public", job.getApplicationId());

		// Non-admin user can only submit a fixed number of concurrent jobs.
		int maxJobs = application.getSettings().getMaxRunningJobsPerUser();
		assertTrue(maxJobs >= 1);

		// First we saturate (there is already one job running, hence i = 1):
		for (int i = 1; i < maxJobs; i++) {
			try {
				job = jobService.submitJob("return-true-step-public", List.of(), user, "agent");
				assertNotNull(job);
			} catch (JsonHttpStatusException e) {
				System.out.println(e.getMessage());
				throw e;
			}
		}

		// We have reached the maximum:
		assertThrows(
				JsonHttpStatusException.class,
				() -> jobService.submitJob("return-true-step-public", List.of(), user, "agent"));

		// Admin user can submit an arbitrary number of jobs.
		for (int i = 0; i < 2 * maxJobs; i++) {
			job = jobService.submitJob("return-true-step-public", List.of(), admin, "agent");
			assertNotNull(job);
		}

		// If we pass a "job-name" parameter, it is used to set the job name.

		List<FormUtil.Parameter> params = List.of(new FormUtil.Parameter("job-name", "Hello, world!"));
		job = jobService.submitJob("return-true-step-public", params, admin, "agent");

		assertNotNull(job);
		assertEquals("Hello, world!", job.getName()); // Name is set...
		assertTrue(job.getId().startsWith("job-")); // But ID is still auto-generated.

		// Passing an input that does not match the expected inputs throws an exception.
		assertThrows(JsonHttpStatusException.class,
				() -> jobService.submitJob(
						"return-true-step-public",
						List.of(new FormUtil.Parameter("this-input-does-not-exist", "1-2-3")),
						admin,
						"agent"));
	}

	@Test
	public void testGetById() {
		// On job missing, throw an exception.
		assertThrows(JsonHttpStatusException.class, () -> jobService.getById("fake-id"));

		// If we submit a job and immediately read it from its ID, we should receive an
		// equivalent object.
		User user = userService.getByUsername("user");
		AbstractJob submitted = jobService.submitJob("return-true-step-public", List.of(), user, "agent");

		assertNotNull(submitted);
		assertNotNull(submitted.getId());
		assertFalse(submitted.getId().isBlank());

		AbstractJob read = jobService.getById(submitted.getId());

		assertEquals(submitted, read);
	}

	@Test
	public void testCancel() {
		User user = userService.getByUsername("user");

		// First we submit a job.
		AbstractJob submitted = jobService.submitJob("return-true-step-public", List.of(), user, "agent");
		assertNotNull(submitted);
		assertNotNull(submitted.getId());
		assertFalse(submitted.getId().isBlank());

		assertTrue((submitted.getState() == AbstractJob.STATE_RUNNING)
				|| (submitted.getState() == AbstractJob.STATE_WAITING));

		// Job can be read for now.
		AbstractJob read = jobService.getById(submitted.getId());
		assertNotNull(read);
		assertEquals(submitted, read);

		// We cancel the job. Action is passthrough.
		AbstractJob canceled = jobService.cancel(submitted);
		assertSame(canceled, submitted); // Referential equality.

		// Status was set to CANCELED
		assertEquals(AbstractJob.STATE_CANCELED, canceled.getState());

		// Job can still be found, but it is still CANCELED.
		AbstractJob reRead = jobService.getById(submitted.getId());
		assertEquals(canceled, reRead);

		// Cancelling more than once is a no-op.
		AbstractJob again = jobService.cancel(canceled);
		assertSame(canceled, again);

		// Passing in garbage is also a no-op.
		AbstractJob helloDummy = new CloudgeneJob();
		AbstractJob byeDummy = jobService.cancel(helloDummy);
		assertSame(helloDummy, byeDummy);
	}

	@Test
	public void testGetByIdAndUser() {
		User user = userService.getByUsername("user");
		User other = userService.getByUsername("public");
		User admin = userService.getByUsername("admin");

		// On job missing, throw an exception.
		assertThrows(JsonHttpStatusException.class, () -> jobService.getByIdAndUser("fake-id", user));

		// If we submit a job and immediately read it from its ID,
		// we should receive an equivalent object.
		AbstractJob submitted = jobService.submitJob("return-true-step-public", List.of(), user, "agent");

		assertNotNull(submitted);
		assertNotNull(submitted.getId());
		assertFalse(submitted.getId().isBlank());

		// The submitting user can access the job.
		AbstractJob read = jobService.getByIdAndUser(submitted.getId(), user);
		assertEquals(submitted, read);

		// Admin users can also access the job.
		AbstractJob readAsAdmin = jobService.getByIdAndUser(submitted.getId(), admin);
		assertEquals(submitted, readAsAdmin);

		// User must not be null.
		assertThrows(JsonHttpStatusException.class, () -> jobService.getByIdAndUser(submitted.getId(), null));

		// Non-admin users that are not the submitter are forbidden from accessing the
		// data.
		assertThrows(JsonHttpStatusException.class, () -> jobService.getByIdAndUser(submitted.getId(), other));
	}
}
