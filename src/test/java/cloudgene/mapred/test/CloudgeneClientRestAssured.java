package cloudgene.mapred.test;

import cloudgene.mapred.jobs.AbstractJob;
import io.micronaut.context.annotation.Prototype;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import jakarta.inject.Inject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.fail;

@Prototype
public class CloudgeneClientRestAssured {

	@Inject
	private TestApplication application;

	/**
	 * Logs in with the provided {@code username} and {@code password}, and returns
	 * the bearer token header required for API authentication. Fails the test if
	 * the credentials are not valid.
	 */
	public Header login(String username, String password) {
		Response response = RestAssured
				.given()
				.formParams("username", username, "password", password)
				.when()
				.post("/login")
				.thenReturn();

		response.then().statusCode(200);

		String accessToken = response.body().jsonPath().getString("access_token");
		return new Header("X-Auth-Token", accessToken);
	}

	public Header loginAsPublicUser() {
		return login("public", "public-password");
	}

	/**
	 * Sleeps until the job with the given {@code id} completes. Errors if there is
	 * no such reachable job. Times out after 30 seconds.
	 */
	public void waitForJob(String id, Header accessToken) {
		Response response = RestAssured
				.given()
				.header(accessToken)
				.when()
				.get("/api/v2/jobs/" + id + "/status")
				.thenReturn();

		response.then().statusCode(200);

		int state = response.body().jsonPath().getInt("state");

		boolean running = state == AbstractJob.STATE_WAITING
				|| state == AbstractJob.STATE_RUNNING
				|| state == AbstractJob.STATE_EXPORTING;

		if (running) {
			try {
				AbstractJob job = application.getWorkflowEngine().getJobById(id);
				Future<?> future = application.getWorkflowEngine().getFuture(job);
				future.get(30, TimeUnit.SECONDS);
			} catch (ExecutionException | InterruptedException | TimeoutException e) {
				fail();
			}
		}
	}

	public void waitForJobWithApiToken(String id, String apiToken) {
		Header header = new Header("X-Auth-Token", apiToken);
		waitForJob(id, header);
	}
}
