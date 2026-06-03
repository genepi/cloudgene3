package cloudgene.mapred.api.v2.jobs;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;

import cloudgene.mapred.jobs.AbstractJob;
import org.junit.jupiter.api.Test;

import cloudgene.mapred.test.TestApplication;
import cloudgene.mapred.test.CloudgeneClientRestAssured;
import genepi.io.FileUtil;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import jakarta.inject.Inject;

@MicronautTest
public class DeleteJobTest {

	@Inject
	TestApplication application;

	@Inject
	CloudgeneClientRestAssured client;

	@Test
	public void testIfDeleteJobCleansUpWorkspace() throws InterruptedException {
		Header accessToken = client.loginAsPublicUser();

		// submit job
		String id = RestAssured
				.given()
				.header(accessToken)
				.multiPart("inputtext", "lukas_text")
				.when()
				.post("/api/v2/jobs/submit/write-text-to-file")
				.then()
				.statusCode(200)
				.extract().jsonPath().getString("id");

		// wait until submitted job is complete
		client.waitForJob(id, accessToken);

		// get details
		Response response = RestAssured
				.given()
				.header(accessToken)
				.when()
				.get("/api/v2/jobs/" + id)
				.thenReturn();

		response.then()
				.statusCode(200)
				.body("state", equalTo(AbstractJob.STATE_SUCCESS));

		// get file details
		String name = response.jsonPath().getString("outputParams[0].files[0].name");
		String hash = response.jsonPath().getString("outputParams[0].files[0].hash");

		// download file and check content
		RestAssured
				.given()
				.header(accessToken)
				.when()
				.get("/downloads/" + id + "/" + hash + "/" + name)
				.then()
				.statusCode(200)
				.body(equalTo("lukas_text"));

		// delete job with wrong permissions
		RestAssured
				.when()
				.delete("/api/v2/jobs/" + id)
				.then()
				.statusCode(401);

		// delete job with wrong id
		RestAssured
				.given()
				.header(accessToken)
				.when()
				.delete("/api/v2/jobs/blabla")
				.then()
				.statusCode(404);

		// delete job
		RestAssured
				.given()
				.header(accessToken)
				.when()
				.delete("/api/v2/jobs/" + id)
				.then()
				.statusCode(200);

		// check if all data are deleted
		assertFalse(new File(FileUtil.path(application.getSettings().getLocalWorkspace(), id)).exists());

		// check if job was deleted from database (return 404)
		RestAssured
				.given()
				.header(accessToken)
				.when()
				.get("/api/v2/jobs/" + id)
				.then()
				.statusCode(404);
	}
}
