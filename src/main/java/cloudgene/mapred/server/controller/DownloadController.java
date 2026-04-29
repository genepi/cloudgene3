package cloudgene.mapred.server.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.DownloadDao;
import cloudgene.mapred.database.ParameterDao;
import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.CloudgeneParameterOutput;
import cloudgene.mapred.jobs.Download;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.server.auth.AuthenticationService;
import cloudgene.mapred.server.auth.AuthenticationType;
import cloudgene.mapred.server.exceptions.JsonHttpStatusException;
import cloudgene.mapred.server.services.DownloadService;
import cloudgene.mapred.server.services.JobService;
import cloudgene.mapred.wdl.WdlParameterOutputType;
import genepi.io.FileUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DownloadController {

	protected static final Logger log = LoggerFactory.getLogger(DownloadController.class);

	@Inject
	protected Application application;

	@Inject
	protected AuthenticationService authenticationService;

	@Inject
	protected DownloadService downloadService;

	@Inject
	protected JobService jobService;

	@Get("/downloads/{jobId}/{hash}/{filename:.+}")
	@Secured(SecurityRule.IS_ANONYMOUS)
	public MutableHttpResponse<InputStream> downloadExternalResults(String jobId, String hash, String filename)
			throws URISyntaxException, IOException {

		AbstractJob job = jobService.getById(jobId);

		DownloadDao dao = new DownloadDao(application.getDatabase());
		Download download = dao.findByHash(hash);

		// job is running and not in database
		if (download == null) {
			download = job.findDownloadByHash(hash);
		}
		String message = String.format("Job: Downloading file '%s' for job %s", filename, job.getId());
		log.info(message);
		return downloadService.download(download);

	}

	@Get("/share/results/{hash}/{filename:.+}")
	@Secured(SecurityRule.IS_ANONYMOUS)
	public MutableHttpResponse<InputStream> downloadPublicLink(String hash, String filename)
			throws URISyntaxException, IOException {

		DownloadDao dao = new DownloadDao(application.getDatabase());
		Download download = dao.findByHash(hash);

		String message = String.format("Job: Anonymously downloading file '%s' (hash %s)", filename, hash);
		log.info(message);
		try {
			return downloadService.download(download);
		} catch (IOException e) {
			log.error("Downloading file failed.", e);
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, "File not found in workspace.");
		}
	}

	@Get("/browse/{hash}/{filename:.+}")
	@Secured(SecurityRule.IS_ANONYMOUS)
	public MutableHttpResponse<InputStream> downloadByParamHash(String hash, String filename)
			throws URISyntaxException, IOException {

		ParameterDao parameterDao = new ParameterDao(application.getDatabase());
		CloudgeneParameterOutput param = parameterDao.findByHash(hash);

		if (param == null) {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, "Param for hash " + hash + " not found.");
		}

		DownloadDao dao = new DownloadDao(application.getDatabase());
		Download download = dao.findByParameterAndName(param, filename);

		String message = String.format("Job: Anonymously downloading file '%s' (hash %s)", filename, hash);
		log.info(message);
		return downloadService.download(download);

	}

	@Get("/get/{hash}")
	@Secured(SecurityRule.IS_ANONYMOUS)
	public String downloadScript(String hash) {

		ParameterDao parameterDao = new ParameterDao(application.getDatabase());
		CloudgeneParameterOutput param = parameterDao.findByHash(hash);

		if (param == null) {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, "Param for hash " + hash + " not found.");
		}

		DownloadDao dao = new DownloadDao(application.getDatabase());
		List<Download> downloads = dao.findAllByParameter(param);

		String hostname = application.getSettings().getServerUrl();
		hostname += application.getSettings().getBaseUrl();

		StringBuffer script = new StringBuffer();
		script.append("#!/bin/bash\n");
		script.append("set -e\n");
		script.append("GREEN='\033[0;32m'\n");
		script.append("NC='\033[0m'\n");
		int i = 1;
		for (Download download : downloads) {
			script.append("echo \"\"\n");
			script.append(
					"echo \"Downloading file " + download.getName() + " (" + i + "/" + downloads.size() + ")...\"\n");
			script.append("curl -L " + hostname + "/share/results/" + download.getHash() + "/" + download.getName()
					+ " -o " + download.getName() + " --create-dirs \n");
			i++;
		}
		script.append("echo \"\"\n");
		script.append("echo -e \"${GREEN}All " + downloads.size() + " file(s) downloaded.${NC}\"\n");
		script.append("echo \"\"\n");
		script.append("echo \"\"\n");
		return script.toString();

	}

	@Get("/api/v2/jobs/{jobId}/chunks/{filename}")
	@Secured(SecurityRule.IS_ANONYMOUS)
	public File downloadChunk(Authentication authentication, String jobId, String filename) {

		User user = authenticationService.getUserByAuthentication(authentication, AuthenticationType.ALL_TOKENS);

		AbstractJob job = jobService.getByIdAndUser(jobId, user);

		String resultFile = FileUtil.path(application.getSettings().getLocalWorkspace(), job.getId(), "chunks",
				filename);

		return new File(resultFile);

	}

	@Get("/api/v2/jobs/{jobId}/webpage/{hash}/{path:.+}")
	@Secured(SecurityRule.IS_ANONYMOUS)
	public MutableHttpResponse<InputStream> serveWebpageFile(String jobId, String hash, String path)
			throws IOException {

		ParameterDao parameterDao = new ParameterDao(application.getDatabase());
		CloudgeneParameterOutput param = parameterDao.findByHash(hash);

		if (param == null) {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, "Webpage output not found.");
		}

		if (param.getType() != WdlParameterOutputType.WEBPAGE) {
			throw new JsonHttpStatusException(HttpStatus.FORBIDDEN, "Output is not a webpage type.");
		}

		if (!jobId.equals(param.getJobId())) {
			throw new JsonHttpStatusException(HttpStatus.FORBIDDEN, "Access denied.");
		}

		// Resolve the file path and prevent directory traversal
		String outputFolder = FileUtil.path(application.getSettings().getLocalWorkspace(), jobId, param.getName());
		Path basePath = Paths.get(outputFolder).normalize();
		Path filePath = basePath.resolve(path).normalize();

		if (!filePath.startsWith(basePath)) {
			throw new JsonHttpStatusException(HttpStatus.FORBIDDEN, "Access denied.");
		}

		File file = filePath.toFile();
		if (!file.exists() || !file.isFile()) {
			// If index.html not ready yet, serve a placeholder page
			if (path.equals("index.html")) {
				String placeholder = "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
						+ "<meta http-equiv=\"refresh\" content=\"10\">"
						+ "<style>body{font-family:sans-serif;display:flex;justify-content:center;"
						+ "align-items:center;min-height:60vh;color:#555;}"
						+ ".container{text-align:center;}"
						+ ".spinner{border:4px solid #eee;border-top:4px solid #007bff;"
						+ "border-radius:50%;width:40px;height:40px;animation:spin 1s linear infinite;"
						+ "margin:0 auto 20px;}"
						+ "@keyframes spin{to{transform:rotate(360deg);}}"
						+ "</style></head><body><div class=\"container\">"
						+ "<div class=\"spinner\"></div>"
						+ "<h3>Webpage output is not yet available</h3>"
						+ "<p>The job is still running. This page will refresh automatically.</p>"
						+ "</div></body></html>";
				byte[] bytes = placeholder.getBytes(java.nio.charset.StandardCharsets.UTF_8);
				return HttpResponse.<InputStream>ok(new java.io.ByteArrayInputStream(bytes))
						.contentType(MediaType.of("text/html"))
						.contentLength(bytes.length);
			}
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, "File not found: " + path);
		}

		String contentType = getContentType(filePath);

		log.info("Job: Serving webpage file '{}' for job {}", path, jobId);
		return HttpResponse.<InputStream>ok(new FileInputStream(file))
				.contentType(MediaType.of(contentType))
				.contentLength(file.length());
	}

	private static final Map<String, String> MIME_TYPES = new HashMap<>();
	static {
		MIME_TYPES.put("html", "text/html");
		MIME_TYPES.put("htm", "text/html");
		MIME_TYPES.put("css", "text/css");
		MIME_TYPES.put("js", "application/javascript");
		MIME_TYPES.put("json", "application/json");
		MIME_TYPES.put("png", "image/png");
		MIME_TYPES.put("jpg", "image/jpeg");
		MIME_TYPES.put("jpeg", "image/jpeg");
		MIME_TYPES.put("gif", "image/gif");
		MIME_TYPES.put("svg", "image/svg+xml");
		MIME_TYPES.put("ico", "image/x-icon");
		MIME_TYPES.put("woff", "font/woff");
		MIME_TYPES.put("woff2", "font/woff2");
		MIME_TYPES.put("ttf", "font/ttf");
		MIME_TYPES.put("csv", "text/csv");
		MIME_TYPES.put("tsv", "text/tab-separated-values");
		MIME_TYPES.put("txt", "text/plain");
		MIME_TYPES.put("xml", "application/xml");
		MIME_TYPES.put("pdf", "application/pdf");
	}

	private String getContentType(Path filePath) {
		String filename = filePath.getFileName().toString();
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex > 0) {
			String ext = filename.substring(dotIndex + 1).toLowerCase();
			String mime = MIME_TYPES.get(ext);
			if (mime != null) {
				return mime;
			}
		}
		try {
			String probed = Files.probeContentType(filePath);
			if (probed != null) {
				return probed;
			}
		} catch (IOException e) {
			// fall through
		}
		return "application/octet-stream";
	}

}
