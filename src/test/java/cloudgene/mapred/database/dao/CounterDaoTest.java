package cloudgene.mapred.database.dao;

import cloudgene.mapred.test.TestApplication;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.jobs.CloudgeneJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CounterDaoTest {

	private CounterDao counterDao;

	@BeforeEach
	public void setup() throws Exception {
		TestApplication application = new TestApplication();
		Database db = application.getDatabase();
		counterDao = new CounterDao(db);
	}

	@Test
	public void testInsertAndGetAll() {
		Map<String, Long> observed;
		boolean success;

		CloudgeneJob dummyJob = new CloudgeneJob();
		dummyJob.setId("DummyJob-CounterDaoTest-testInsertAndGetAll");

		// Counter DAO starts empty.
		observed = counterDao.getAll();
		assertNotNull(observed);
		assertEquals(Map.of(), observed);

		// On first insert, we only see the same key-value pair again.
		success = counterDao.insert("hawk-sightings", 2, dummyJob);
		assertTrue(success);

		observed = counterDao.getAll();
		assertNotNull(observed);
		assertEquals(Map.of("hawk-sightings", 2L), observed);

		// Same if we add a different key: we see both keys and their original values.
		success = counterDao.insert("early-lunches", 1, dummyJob);
		assertTrue(success);

		observed = counterDao.getAll();
		assertNotNull(observed);
		assertEquals(
				Map.of(
						"hawk-sightings", 2L,
						"early-lunches", 1L),
				observed);

		// If we add again to an existing key, we get the SUM of all inserted values.
		success = counterDao.insert("hawk-sightings", 3, dummyJob);
		assertTrue(success);

		observed = counterDao.getAll();
		assertNotNull(observed);
		assertEquals(
				Map.of(
						"hawk-sightings", 5L,
						"early-lunches", 1L),
				observed);
	}
}
