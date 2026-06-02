package cloudgene.mapred.test;

import cloudgene.mapred.jobs.AbstractJob;
import io.micronaut.context.annotation.Prototype;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;

@Prototype
public class CloudgeneClientRestAssured {

	public static int POLL_INTERVAL_MS = 500;

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

	public void waitForJob(String id, Header accessToken) {
		Response response = RestAssured
				.given()
				.when()
				.header(accessToken).get("/api/v2/jobs/" + id + "/status")
				.thenReturn();

		response.then().statusCode(200);

		int state = response.body().jsonPath().getInt("state");

		boolean running = state == AbstractJob.STATE_WAITING
				|| state == AbstractJob.STATE_RUNNING
				|| state == AbstractJob.STATE_EXPORTING;

		if (running) {
			try {
				Thread.sleep(POLL_INTERVAL_MS);
				waitForJob(id, accessToken);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void waitForJobWithApiToken(String id, String apiToken) {
		Header header = new Header("X-Auth-Token", apiToken);
		waitForJob(id, header);
	}
}
