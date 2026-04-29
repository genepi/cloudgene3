package cloudgene.mapred.util;

import cloudgene.mapred.jobs.AbstractJob;
import io.micronaut.context.annotation.Prototype;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;

import cloudgene.mapred.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Prototype
public class CloudgeneClientRestAssured {


	private static final Logger log = LoggerFactory.getLogger(CloudgeneClientRestAssured.class);

	public static int POLL_INTERVAL_MS = 500;

	public Header login(String username, String password) {

		Response response = RestAssured.given().formParams("username", username, "password", password).when()
				.post("/login").thenReturn();
		log.warn("login_test_restassured");
		response.then().statusCode(200);
		log.warn(response.body().jsonPath().getString("access_token"));
		return new Header("X-Auth-Token", response.body().jsonPath().getString("access_token"));
	}

	public Header loginAsPublicUser() {
		return login("publicly", "publicly");
	}

	public void waitForJob(String id, Header accessToken) {

		Response response = RestAssured.given().when().header(accessToken).get("/api/v2/jobs/" + id + "/status")
				.thenReturn();
		response.then().statusCode(200);

		int state = response.body().jsonPath().getInt("state");

		boolean running = state == AbstractJob.STATE_WAITING || state == AbstractJob.STATE_RUNNING
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
