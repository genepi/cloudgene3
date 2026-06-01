package cloudgene.mapred.database.updates;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import cloudgene.mapred.database.util.*;
import cloudgene.mapred.test.TestDbUtil;
import cloudgene.mapred.util.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class DatabaseUpdaterTest {

	private static Stream<Arguments> provideForConstructor() throws SQLException {
		return Stream.of(
				// Version == 0.0.0 (default) -> no update needed.
				arguments(
						TestDbUtil.getMemDb(),
						new File("test-data/test-updates.sql"),
						"0.0.0",
						null,
						false),

				// Version > 0.0.0 (default) -> needs update.
				arguments(
						TestDbUtil.getMemDb(),
						new File("test-data/test-updates.sql"),
						"1.0.0",
						null,
						true));
	}

	@ParameterizedTest
	@MethodSource("provideForConstructor")
	public void testConstructor(
			Database database,
			File updatesFile,
			String currentVersion,
			String errMsg,
			boolean needsUpdate) {

		try (InputStream is = new FileInputStream(updatesFile)) {
			DatabaseUpdater updater = new DatabaseUpdater(database, Configuration.getVersionFilename(), is, currentVersion);
			assertNull(errMsg);

			assertEquals("0.0.0", updater.getOldVersion());
			assertEquals(currentVersion, updater.getCurrentVersion());
			assertEquals(needsUpdate, updater.needUpdate());
		} catch (Exception e) {
			assertNotNull(errMsg);
			assertTrue(e.getMessage().startsWith(errMsg));
		}
	}

	public record UpdateDbTestCase(
			String sqlPath,
			String version,
			boolean preloadTable,
			boolean needsUpdate,
			boolean success) {
	}

	private static Stream<UpdateDbTestCase> provideForUpdateDB() {
		return Stream.of(
				// All these cases are normal and should result in successful runs.
				new UpdateDbTestCase("test-data/test-updates.sql", "0.0.0", false, false, true),
				new UpdateDbTestCase("test-data/test-updates.sql", "0.0.0", true, false, true),
				new UpdateDbTestCase("test-data/test-updates.sql", "1.0.0", false, true, true),

				// Invalid SQL statements -> updateDB() fails.
				new UpdateDbTestCase("test-data/test-updates-broken.sql", "1.0.0", false, true, false));
	}

	@ParameterizedTest
	@MethodSource("provideForUpdateDB")
	public void testUpdateDB(UpdateDbTestCase testCase) throws SQLException, IOException {
		Database db = TestDbUtil.getMemDb();
		File updatesFile = new File(testCase.sqlPath);

		if (testCase.preloadTable) {
			try (InputStream is = new FileInputStream(updatesFile)) {
				DatabaseUpdater dummy = new DatabaseUpdater(db, Configuration.getVersionFilename(), is, "0.0.0");
				dummy.writeVersion("0.0.0");
			}
		}

		try (InputStream is = new FileInputStream(updatesFile)) {
			DatabaseUpdater updater = new DatabaseUpdater(db, Configuration.getVersionFilename(), is, testCase.version);

			assertEquals(testCase.preloadTable, db.getConnector().existsTable("database_versions"));
			assertEquals(testCase.needsUpdate, updater.needUpdate());

			boolean observed = updater.updateDB();
			assertEquals(testCase.success, observed);

			if (testCase.success) {
				assertTrue(db.getConnector().existsTable("database_versions")); // Always created on success.
			}
		}
	}

	@Test
	public void testListeners() throws SQLException, IOException {
		Database db = TestDbUtil.getMemDb();
		File updatesFile = new File("test-data/test-updates.sql");

		try (InputStream is = new FileInputStream(updatesFile)) {
			DatabaseUpdater updater = new DatabaseUpdater(db, Configuration.getVersionFilename(), is, "1.0.0");

			List<String> records = new ArrayList<>();

			updater.addUpdate("0.0.1", new VersionRecorder("0.0.1", records));
			updater.addUpdate("0.0.2", new VersionRecorder("0.0.2", records));
			updater.addUpdate("0.0.3", new VersionRecorder("0.0.3", records));

			boolean result = updater.updateDB();
			assertTrue(result);

			assertEquals(
					List.of(
							// 0.0.1 is present in test-updates.sql and actually has contents.
							"Before update: 0.0.1",
							"After update: 0.0.1",

							// 0.0.2 is present in test-updates.sql but is empty.
							"Before update: 0.0.2",
							"After update: 0.0.2"

							// 0.0.3 is not present in test-updates.sql, so it's skipped.
							// 0.1.0 exists in test-updates.sql, but has no attached listener.
					),
					records);
		}

		db.disconnect();
	}

	@Test
	public void testConstructorWithExistingDBData() throws SQLException, IOException {
		Database db = TestDbUtil.getMemDb();
		File updatesFile = new File("test-data/test-updates.sql");

		try (InputStream is = new FileInputStream(updatesFile)) {
			DatabaseUpdater dummy = new DatabaseUpdater(db, Configuration.getVersionFilename(), is, "0.1.0");

			boolean success = dummy.updateDB();
			assertTrue(success);
		}

		try (InputStream is = new FileInputStream(updatesFile)) {
			DatabaseUpdater updater = new DatabaseUpdater(db, Configuration.getVersionFilename(), is, "1.0.0");

			assertTrue(updater.needUpdate());
			assertEquals("0.1.0", updater.getOldVersion());
			assertEquals("1.0.0", updater.getCurrentVersion());
		}
	}

	@Test
	public void testIncrementalUpdates() throws SQLException, IOException {
		Database db = TestDbUtil.getMemDb();
		File updatesFile = new File("test-data/test-updates.sql");
		boolean success;

		// v0.0.0: nothing to do
		try (InputStream is = new FileInputStream(updatesFile)) {
			DatabaseUpdater u000 = new DatabaseUpdater(db, Configuration.getVersionFilename(), is, "0.0.0");

			assertFalse(u000.needUpdate());

			success = u000.updateDB();
			assertTrue(success);
			assertFalse(db.getConnector().existsTable("user")); // v0.0.1
			assertFalse(db.getConnector().existsTable("job")); // v0.1.0
			assertEquals("0.0.0", u000.readVersionDB());
		}

		// v0.0.1: 'user' table created at v0.0.1
		try (InputStream is = new FileInputStream(updatesFile)) {
			DatabaseUpdater u001 = new DatabaseUpdater(db, Configuration.getVersionFilename(), is, "0.0.1");

			assertTrue(u001.needUpdate());
			assertEquals("0.0.0", u001.readVersionDB());

			success = u001.updateDB();
			assertTrue(success);
			assertTrue(db.getConnector().existsTable("user")); // v0.0.1
			assertFalse(db.getConnector().existsTable("job")); // v0.1.0
			assertEquals("0.0.1", u001.readVersionDB());
		}

		// v0.2.3: 'job' table created at v0.1.0
		try (InputStream is = new FileInputStream(updatesFile)) {
			DatabaseUpdater u023 = new DatabaseUpdater(db, Configuration.getVersionFilename(), is, "0.2.3");

			assertTrue(u023.needUpdate());
			assertEquals("0.0.1", u023.readVersionDB());

			success = u023.updateDB();
			assertTrue(success);
			assertTrue(db.getConnector().existsTable("user")); // v0.0.1
			assertTrue(db.getConnector().existsTable("job")); // v0.1.0
			assertEquals("0.2.3", u023.readVersionDB());
		}

		// v0.1.1: regression from v0.2.3. Update fails, since we cannot roll back DB state.
		try (InputStream is = new FileInputStream(updatesFile)) {
			DatabaseUpdater u011 = new DatabaseUpdater(db, Configuration.getVersionFilename(), is, "0.1.1");

			assertFalse(u011.needUpdate());
			success = u011.updateDB();
			assertFalse(success);
		}
	}

	private static class VersionRecorder implements IUpdateListener {

		private final String version;
		private final List<String> records;

		public VersionRecorder(String version, List<String> records) {
			this.version = version;
			this.records = records;
		}

		@Override
		public void beforeUpdate(Database database) {
			records.add("Before update: " + version);
		}

		@Override
		public void afterUpdate(Database database) {
			records.add("After update: " + version);
		}
	}
}
