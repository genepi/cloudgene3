package cloudgene.mapred.database.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2Connector implements DatabaseConnector {

	protected static final Logger log = LoggerFactory.getLogger(H2Connector.class);

	private BasicDataSource dataSource;

	private final String path;
	private final String user;
	private final String password;
	private final boolean multiuser;

	public H2Connector(String path, String user, String password, boolean multiuser) {
		this.user = user;
		this.password = password;
		this.multiuser = multiuser;

		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException("path must be non-null and non-empty");
		}

		if (path.startsWith("mem:")) {
			// In-memory database
			this.path = path;
		} else if (path.startsWith("/")) {
			// Absolute path
			this.path = path;
		} else {
			// Relative path
			this.path = "./" + path;
		}
	}

	public void connect() throws SQLException {
		log.debug("Establishing connection to {}@{}", user, path);

		if (DbUtils.loadDriver("org.h2.Driver")) {
			try {
				dataSource = new BasicDataSource();

				dataSource.setDriverClassName("org.h2.Driver");

				if (multiuser) {
					dataSource.setUrl("jdbc:h2:" + path + ";AUTO_SERVER=TRUE;MODE=MySQL");
				} else {
					dataSource.setUrl("jdbc:h2:" + path + ";MODE=MySQL");
				}
				dataSource.setUsername(user);
				dataSource.setPassword(password);
				dataSource.setMaxIdle(10_000);
				dataSource.setDefaultAutoCommit(true);

			} catch (Exception e) {
				log.error("Failed to connect to H2 database", e);
			}
		} else {
			log.error("H2 Driver Class not found");
		}
	}

	public void disconnect() throws SQLException {
		dataSource.close();
	}

	public void executeSQL(InputStream is) throws SQLException, IOException, URISyntaxException {
		String sqlContent = readFileAsString(is);

		if (!sqlContent.isEmpty()) {
			try (Connection connection = dataSource.getConnection();
			     PreparedStatement ps = connection.prepareStatement(sqlContent)) {
				ps.executeUpdate();
			}
		}
	}

	public static String readFileAsString(InputStream is) throws java.io.IOException, URISyntaxException {
		String strLine;
		StringBuilder builder = new StringBuilder();

		try (InputStreamReader isr = new InputStreamReader(is);
		     BufferedReader br = new BufferedReader(isr)) {

			while ((strLine = br.readLine()) != null) {
				builder.append("\n");
				builder.append(strLine);
			}
		}

		return builder.toString();
	}

	public BasicDataSource getDataSource() {
		return dataSource;
	}

	@Override
	public String getSchema() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean existsTable(String table) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData meta = connection.getMetaData();

			try (ResultSet res = meta.getTables(null, null, table.toUpperCase(), new String[]{"TABLE"})) {
				boolean exists = res.next();
				res.close();

				if (!exists) {
					log.warn("Table '{}' not found'", table);
				}

				return exists;
			}
		}
	}
}
