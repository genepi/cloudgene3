package cloudgene.mapred.api.v2.jobs;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cloudgene.mapred.jobs.AbstractJob;
import org.junit.jupiter.api.Test;

import cloudgene.mapred.test.TestApplication;
import cloudgene.mapred.test.CloudgeneClientRestAssured;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import jakarta.inject.Inject;

@MicronautTest
public class DownloadResultsTest {

	@Inject
	TestApplication application;

	@Inject
	CloudgeneClientRestAssured client;

	@Test
	public void testDownloadSingleFile() {
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
				.and()
				.body("state", equalTo(AbstractJob.STATE_SUCCESS))
				.body("outputParams[0].name", equalTo("output"))
				.body("outputParams[0].files.size()", equalTo(1));

		String path = response.jsonPath().getString("outputParams[0].files[0].path");
		String name = response.jsonPath().getString("outputParams[0].files[0].name");
		String hash = response.jsonPath().getString("outputParams[0].files[0].hash");

		assertEquals(id + "/output/output", path);

		// download file and check content
		RestAssured
				.given()
				.header(accessToken)
				.when()
				.get("/downloads/" + id + "/" + hash + "/" + name)
				.then()
				.statusCode(200)
				.body(equalTo("lukas_text"));

		// check if it returns 404
		String randomPath = id + "/hash/lukas.txt";

		RestAssured
				.given()
				.header(accessToken)
				.when()
				.get("/downloads/" + randomPath)
				.then()
				.statusCode(404)
				.body("success", equalTo(false))
				.body("message", equalTo("download not found."));
	}

	@Test
	public void testDownloadSingleFolder() {
		Header accessToken = client.loginAsPublicUser();

		// submit job
		String id = RestAssured
				.given()
				.header(accessToken)
				.multiPart("inputtext", "lukas_text")
				.when()
				.post("/api/v2/jobs/submit/write-files-to-folder")
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
				.body("state", equalTo(AbstractJob.STATE_SUCCESS))
				.body("outputParams[0].name", equalTo("output"))
				.body("outputParams[0].files.size()", equalTo(5));

		// get path and download all 5 files
		for (int i = 0; i < 5; i++) {
			String path = response.jsonPath().getString("outputParams[0].files[" + i + "].path");
			String name = response.jsonPath().getString("outputParams[0].files[" + i + "].name");
			String hash = response.jsonPath().getString("outputParams[0].files[" + i + "].hash");

			assertEquals(id + "/output/file" + (i + 1) + ".txt", path);

			RestAssured
					.given()
					.header(accessToken)
					.when()
					.get("/downloads/" + id + "/" + hash + "/" + name)
					.then()
					.statusCode(200)
					.body(equalTo("lukas_text"));
		}
	}

	@Test
	public void testDownloadCounter() {
		Header accessToken = client.loginAsPublicUser();

		// submit job
		String id = RestAssured
				.given()
				.header(accessToken)
				.multiPart("inputtext", "lukas_text")
				.when()
				.post("/api/v2/jobs/submit/write-files-to-folder")
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
				.body("state", equalTo(AbstractJob.STATE_SUCCESS))
				.body("outputParams[0].name", equalTo("output"))
				.body("outputParams[0].files.size()", equalTo(5));

		String path = response.jsonPath().getString("outputParams[0].files[0].path");
		String name = response.jsonPath().getString("outputParams[0].files[0].name");
		String hash = response.jsonPath().getString("outputParams[0].files[0].hash");

		assertEquals(id + "/output/file1.txt", path);

		int maxDownloads = application.getSettings().getMaxDownloads();

		for (int i = 0; i < maxDownloads; i++) {
			RestAssured
					.given()
					.header(accessToken)
					.when()
					.get("/downloads/" + id + "/" + hash + "/" + name)
					.then()
					.statusCode(200)
					.body(equalTo("lukas_text"));
		}

		RestAssured
				.given()
				.header(accessToken)
				.when()
				.get("/downloads/" + id + "/" + hash + "/" + name)
				.then()
				.statusCode(400)
				.body("success", equalTo(false))
				.body("message", equalTo("number of max downloads exceeded."));
	}

	@Test
	public void testJobNotFound() {
		Header accessToken = client.login("admin", "admin1978");

		RestAssured
				.given()
				.header(accessToken)
				.when()
				.get("/downloads/job-lukas277/HASH/file1.txt")
				.then()
				.statusCode(404)
				.body("success", equalTo(false))
				.body("message", equalTo("Job job-lukas277 not found."));
	}
}
