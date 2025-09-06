package cloudgene.mapred.jobs.workspace;

import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.util.Settings;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class WorkspaceFactory {

	@Inject
	protected Application application;

	public IWorkspace getDefault() {

		Settings settings = application.getSettings();

		String type = settings.getExternalWorkspaceType();

		if (type == null) {
			return new LocalWorkspace(settings);
		}

		if (type.equalsIgnoreCase("S3")) {
			return new S3Workspace(settings);
		}

		if (type.equalsIgnoreCase("SSH")) {
			return new SshWorkspace(settings);
		}

		return new LocalWorkspace(settings);

	}

	public IWorkspace getByUrl(String url) {

		Settings settings = application.getSettings();

		if (url == null || url.isEmpty()) {
			throw new RuntimeException("Workspace type could not determined for empty url.");
		}

		if (url.startsWith("s3://")) {
			return new S3Workspace(settings);
		}

		if (url.startsWith("ssh://")) {
			return new SshWorkspace(settings);
		}

		return new LocalWorkspace(settings);

	}

	public IWorkspace getByJob(AbstractJob job) {
		IWorkspace workspace = getDefault();
		workspace.setJob(job.getId());
		return workspace;
	}

}
