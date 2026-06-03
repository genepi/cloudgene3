package cloudgene.mapred.database.dao;

import cloudgene.mapred.test.TestApplication;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.jobs.CloudgeneJob;
import cloudgene.mapred.jobs.JobValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JobValueDaoTest {
	private JobValueDao valueDao;

	@BeforeEach
	public void setup() throws Exception {
		TestApplication application = new TestApplication();
		Database db = application.getDatabase();
		valueDao = new JobValueDao(db);
	}

	@Test
	public void testInsertAndGetAll() {
		List<JobValue> observed;
		boolean success;

		CloudgeneJob dummyJob = new CloudgeneJob();
		dummyJob.setId("DummyJob-JobValueDaoTest-testInsertAndGetAll");

		// Value DAO starts empty.
		observed = valueDao.getAll();
		assertNotNull(observed);
		assertEquals(List.of(), observed);

		// On first insert, we only see the same key-value pair with count 1.
		success = valueDao.insert("hello", "there", dummyJob);
		assertTrue(success);

		observed = valueDao.getAll();
		assertNotNull(observed);
		assertEquals(
				List.of(new JobValue("hello", "there", 1)),
				observed);

		// Repeating the same key-value pair just increases the count.
		success = valueDao.insert("hello", "there", dummyJob);
		assertTrue(success);

		observed = valueDao.getAll();
		assertNotNull(observed);
		assertEquals(
				List.of(new JobValue("hello", "there", 2)),
				observed);

		// Using the same key but using a different value creates a new entry.
		success = valueDao.insert("hello", "friend", dummyJob);
		assertTrue(success);

		observed = valueDao.getAll();
		assertNotNull(observed);
		assertEquals(
				List.of(
						// Sorted alphabetically key -> value
						new JobValue("hello", "friend", 1),
						new JobValue("hello", "there", 2)),
				observed);

		// Using a different key also creates a new entry.
		success = valueDao.insert("stop", "there", dummyJob);
		assertTrue(success);

		observed = valueDao.getAll();
		assertNotNull(observed);
		assertEquals(
				List.of(
						// Sorted alphabetically key -> value
						new JobValue("hello", "friend", 1),
						new JobValue("hello", "there", 2),
						new JobValue("stop", "there", 1)),
				observed);
	}
}
