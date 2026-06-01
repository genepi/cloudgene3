package cloudgene.mapred.server.responses;

import cloudgene.mapred.database.dao.JobValueDao;
import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.List;
import java.util.Vector;

@JsonClassDescription
public class JobValueResponse {

	private String name;
	private String value;
	private int count;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getCount() {
		return count;
	}

	public static JobValueResponse build(JobValueDao.JobValue jobValue) {
		JobValueResponse response = new JobValueResponse();
		response.setName(jobValue.getName());
		response.setValue(jobValue.getValue());
		response.setCount(jobValue.getCount());
		return response;
	}

	public static List<JobValueResponse> build(List<JobValueDao.JobValue> data) {
		List<JobValueResponse> responses = new Vector<JobValueResponse>();
		for (JobValueDao.JobValue jobValue : data) {
			responses.add(JobValueResponse.build(jobValue));
		}
		return responses;
	}
	
}
