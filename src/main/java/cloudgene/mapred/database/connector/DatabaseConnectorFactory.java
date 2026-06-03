package cloudgene.mapred.database.connector;

import java.util.Map;

public class DatabaseConnectorFactory {

	public static DatabaseConnector createConnector(Map<String, String> settings) {

		String driver = settings.get("driver");

		if (driver == null){
			return null;
		}
		
		if (driver.equals("h2")) {

			String database = settings.get("database");
			String user = settings.get("user");
			String password = settings.get("password");

			return new H2Connector(database, user, password, false);

		} else if (driver.equals("mysql")) {

			String host = settings.get("host");
			String port = settings.get("port");
			String database = settings.get("database");
			String user = settings.get("user");
			String password = settings.get("password");

			MySqlConnector connector = new MySqlConnector(host, port, database, user, password);
			if (settings.containsKey("maxActive")) {
				int maxActive = Integer.parseInt(settings.get("maxActive"));
				connector.setMaxActive(maxActive);
			}
			if (settings.containsKey("maxWait")) {
				int maxWait = Integer.parseInt(settings.get("maxWait"));
				connector.setMaxWait(maxWait);
			}
			return connector;

		} else {

			return null;

		}
	}

}
