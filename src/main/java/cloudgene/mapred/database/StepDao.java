package cloudgene.mapred.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import cloudgene.mapred.jobs.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;
import cloudgene.mapred.jobs.CloudgeneJob;
import cloudgene.mapred.jobs.Message;

public class StepDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(StepDao.class);

	public StepDao(Database database) {
		super(database);
	}

	public boolean insert(Step step) {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into steps (state, name, start_time, end_time, job_id) ");
		sql.append("values (?,?,?,?,?)");

		try {

			Object[] params = new Object[5];
			params[0] = 0;
			params[1] = step.getName();
			params[2] = System.currentTimeMillis();
			params[3] = System.currentTimeMillis();
			params[4] = step.getJob().getId();

			int id = insert(sql.toString(), params);
			step.setId(id);

			log.debug("insert step successful.");

		} catch (SQLException e) {
			log.error("insert step failed.", e);
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public List<Step> findAllByJob(CloudgeneJob job) {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from steps ");
		sql.append("where job_id = ? ");
		sql.append("order by start_time ");

		Object[] params = new Object[1];
		params[0] = job.getId();

		List<Step> result = new Vector<Step>();

		try {

			result = query(sql.toString(), params, new CloudgeneStepMapper());

			// load messages for all steps
			MessageDao messageDao = new MessageDao(database);
			for (Step step : result) {
				List<Message> logMessages = messageDao.findAllByStep(step);
				step.setLogMessages(logMessages);
				step.setJob(job);
			}

			log.debug("find all log step successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all log step failed", e);
			return null;
		}
	}

	class CloudgeneStepMapper implements IRowMapper {

		@Override
		public Object mapRow(ResultSet rs, int row) throws SQLException {

			Step step = new Step();
			step.setId(rs.getInt("id"));
			step.setName(rs.getString("name"));
			return step;

		}

	}

}
