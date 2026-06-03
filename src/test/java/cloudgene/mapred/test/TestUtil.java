package cloudgene.mapred.test;

import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.WorkflowEngine;

import java.util.concurrent.TimeUnit;

public final class TestUtil {
	private TestUtil() {}

	/**
	 * Attempts to wait for job completion with a timeout of 10s. If anything goes
	 * wrong, returns immediately.
	 */
	public static void waitForJob(WorkflowEngine engine, AbstractJob job) {
		try {
			engine.getFuture(job).get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			// pass
		}
	}
}
