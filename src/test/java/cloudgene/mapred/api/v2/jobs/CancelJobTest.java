package cloudgene.mapred.api.v2.jobs;

import static org.hamcrest.core.IsEqual.equalTo;

import cloudgene.mapred.jobs.AbstractJob;
import org.junit.jupiter.api.Test;

import cloudgene.mapred.test.CloudgeneClientRestAssured;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import jakarta.inject.Inject;

@MicronautTest
public class CancelJobTest {

	@Inject
	CloudgeneClientRestAssured client;

	@Test
	public void testCancelSleepJob() throws InterruptedException {
		Header accessToken = client.loginAsPublicUser();

		// submit job
		String id = RestAssured
				.given()
				.header(accessToken)
				.multiPart("input", "dummy")
				.when()
				.post("/api/v2/jobs/submit/long-sleep")
				.then()
				.statusCode(200)
				.extract().jsonPath().getString("id");

		Thread.sleep(8_000);

		// cancel job after 8 secs
		RestAssured
				.given()
				.header(accessToken)
				.when()
				.get("/api/v2/jobs/{id}/cancel", id)
				.then()
				.statusCode(200);

		// get details and check state
		RestAssured.given()
				.header(accessToken)
				.when()
				.get("/api/v2/jobs/{id}", id)
				.then()
				.statusCode(200)
				.and()
				.body("state", equalTo(AbstractJob.STATE_CANCELED));

	}

	@Test
	public void testCancelWithWrongJobId() {
		String id = "some-random-id";
		Header accessToken = client.loginAsPublicUser();

		RestAssured.given()
				.header(accessToken)
				.when()
				.get("/api/v2/jobs/{id}/cancel", id)
				.then()
				.statusCode(404)
				.and()
				.body("success", equalTo(false))
				.and()
				.body("message", equalTo("Job " + id + " not found."));
	}

	@Test
	public void testCancelWithoutLogin() {
		String id = "some-random-id";
		RestAssured.when()
				.get("/api/v2/jobs/{id}/cancel", id)
				.then()
				.statusCode(401);
	}
}
