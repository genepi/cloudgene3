package cloudgene.mapred.database.dao;

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;
import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.JobValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

public class JobValueDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(JobValueDao.class);

	public JobValueDao(Database database) {
		super(database);
	}

	public boolean insert(String name, String value, AbstractJob job) {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into job_values (name, job_id, `value`) ");
		sql.append("values (?,?,?)");

		try {
			Object[] params = new Object[3];
			params[0] = name;
			params[1] = job.getId();
			params[2] = value;

			update(sql.toString(), params);

			log.debug("insert value successful.");
		} catch (SQLException e) {
			log.error("insert value failed.", e);
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public List<JobValue> getAll() {
		StringBuilder sql = new StringBuilder();
		sql.append("select name, `value`, count(*) as n ");
		sql.append("from job_values ");
		sql.append("group by name, `value` ");
		sql.append("order by name, `value` ");

		List<JobValue> result = new Vector<>();

		try {
			result = query(sql.toString(), new ValueMapper());
			log.debug("find counters successful. results: {}", result);
			return result;
		} catch (SQLException e) {
			log.error("find all counters failed", e);
		}

		return result;
	}

	static class ValueMapper implements IRowMapper {
		@Override
		public Object mapRow(ResultSet rs, int row) throws SQLException {
			return new JobValue(
					rs.getString("name"),
					rs.getString("value"),
					rs.getInt("n"));
		}
	}
}
