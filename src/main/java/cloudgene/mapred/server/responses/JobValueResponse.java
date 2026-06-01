package cloudgene.mapred.server.responses;

import cloudgene.mapred.jobs.JobValue;
import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.List;
import java.util.Vector;

@JsonClassDescription
public record JobValueResponse(String name, String value, int count) {

	public static JobValueResponse build(JobValue jobValue) {
		return new JobValueResponse(jobValue.name(), jobValue.value(), jobValue.count());
	}

	public static List<JobValueResponse> build(List<JobValue> data) {
		List<JobValueResponse> responses = new Vector<>();
		for (JobValue jobValue : data) {
			responses.add(JobValueResponse.build(jobValue));
		}
		return responses;
	}
}
