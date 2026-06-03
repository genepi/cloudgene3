package cloudgene.mapred.test;

import cloudgene.mapred.database.connector.DatabaseConnector;
import cloudgene.mapred.database.connector.DatabaseConnectorFactory;
import cloudgene.mapred.database.util.Database;
import org.apache.commons.lang3.RandomStringUtils;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class TestDbUtil {
	private TestDbUtil() {}

	/**
	 * Returns an in-memory H2 database whose name is a random string (likely to be
	 * unique).
	 */
	public static Database getMemDb() throws SQLException {
		Database db = new Database();
		String name = RandomStringUtils.secure().nextAlphanumeric(32);

		DatabaseConnector connector = DatabaseConnectorFactory.createConnector(Map.of(
				"driver", "h2",
				"database", "mem:" + name));

		assertNotNull(connector);
		db.connect(connector);

		return db;
	}
}
