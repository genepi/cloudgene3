package cloudgene.mapred.api.v2.admin;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import cloudgene.mapred.Application;
import cloudgene.mapred.auth.AuthenticationService;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.JobDao;
import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.WorkflowEngine;
import genepi.io.FileUtil;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

@Controller
public class GetAllJobs {

	@Inject
	protected Application application;

	@Inject
	protected AuthenticationService authenticationService;

	@Get("/api/v2/admin/jobs")
	@Secured(SecurityRule.IS_AUTHENTICATED)
	public String getJobs(Authentication authentication, @Nullable @QueryValue("state") String state) {

		User user = authenticationService.getUserByAuthentication(authentication);

		if (!user.isAdmin()) {
			throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "The request requires administration rights.");
		}

		WorkflowEngine engine = application.getWorkflowEngine();
		JobDao dao = new JobDao(application.getDatabase());
		List<AbstractJob> jobs = new Vector<AbstractJob>();

		if (state != null) {
			switch (state) {

			case "running-ltq":

				jobs = engine.getAllJobsInLongTimeQueue();
				break;

			case "running-stq":

				jobs = engine.getAllJobsInShortTimeQueue();
				break;

			case "current":

				jobs = dao.findAllNotRetiredJobs();
				List<AbstractJob> toRemove = new Vector<AbstractJob>();
				for (AbstractJob job : jobs) {
					if (engine.isInQueue(job)) {
						toRemove.add(job);
					}
				}
				jobs.removeAll(toRemove);
				break;

			case "retired":

				jobs = dao.findAllByState(AbstractJob.STATE_RETIRED);
				break;

			}
		}

		JsonConfig config = new JsonConfig();
		config.setExcludes(new String[] { "outputParams", "inputParams", "output", "error", "s3Url", "task", "config",
				"mapReduceJob", "job", "step", "context", "hdfsWorkspace", "localWorkspace", "logOutFiles", "logs",
				"removeHdfsWorkspace", "settings", "setupComplete", "stdOutFile", "steps", "workingDirectory",
				"activationCode", "active", "admin", "awsKey", "awsSecretKey", "exportInputToS3", "exportToS3",
				"password", "apiToken", "s3Bucket", "saveCredentials", "map", "reduce", "lastLogin", "loginAttempts",
				"lockedUntil" });

		JSONObject object = new JSONObject();

		int success = 0;
		int failed = 0;
		int pending = 0;
		int waiting = 0;
		int canceld = 0;
		int running = 0;

		for (AbstractJob job : jobs) {

			String workspace = application.getSettings().getLocalWorkspace();
			String folder = FileUtil.path(workspace, job.getId());
			File file = new File(folder);
			if (file.exists()) {
				long size = FileUtils.sizeOfDirectory(file);
				job.setWorkspaceSize(FileUtils.byteCountToDisplaySize(size));
			}

			if (job.getState() == AbstractJob.STATE_EXPORTING || job.getState() == AbstractJob.STATE_RUNNING) {
				running++;
			}
			if (job.getState() == AbstractJob.STATE_SUCCESS
					|| job.getState() == AbstractJob.STATE_SUCESS_AND_NOTIFICATION_SEND) {
				success++;
			}
			if (job.getState() == AbstractJob.STATE_FAILED
					|| job.getState() == AbstractJob.STATE_FAILED_AND_NOTIFICATION_SEND) {
				failed++;
			}
			if (job.getState() == AbstractJob.STATE_DEAD) {
				pending++;
			}
			if (job.getState() == AbstractJob.STATE_WAITING) {
				waiting++;
			}
			if (job.getState() == AbstractJob.STATE_CANCELED) {
				canceld++;
			}
		}

		object.put("count", jobs.size());
		object.put("success", success);
		object.put("failed", failed);
		object.put("pending", pending);
		object.put("waiting", waiting);
		object.put("running", running);
		object.put("canceld", canceld);

		JSONArray jsonArray = JSONArray.fromObject(jobs, config);
		object.put("data", jsonArray);

		return object.toString();

	}
}
