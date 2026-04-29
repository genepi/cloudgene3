package cloudgene.mapred.util;

import java.io.File;
import java.io.IOException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

public class S3Util {

	public record UrlParts(String bucket, String key) {};

	private static AmazonS3 s3;

	private static TransferManager tm;

	public static AmazonS3 getAmazonS3() {
		if (s3 == null) {
			s3 = AmazonS3ClientBuilder.defaultClient();
		}
		return s3;
	}

	public static TransferManager getTransferManager() {
		if (tm == null) {
			s3 = getAmazonS3();
			tm = TransferManagerBuilder.standard().withS3Client(s3).build();
		}
		return tm;
	}

	public static UrlParts getParts(String url) {
		if (!url.startsWith("s3://")) {
			throw new IllegalArgumentException("S3 URLs must start with 's3://'; found: '" + url + "'");
		}
		url = url.replaceAll("s3://", "");

		String[] rawParts = url.split("/", 2);

		if (rawParts.length != 2) {
			throw new IllegalArgumentException("S3 URLs must contain at least one forward slash (/) separating the bucket from the key; found: '" + url + "'");
		}

		return new UrlParts(rawParts[0], rawParts[1]);
	}

	public static boolean isValidS3Url(String url) {
		try {
			getParts(url);
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	public static void copyToFile(String url, File file) throws IOException {
		UrlParts urlParts = getParts(url);
		copyToFile(urlParts.bucket(), urlParts.key(), file);
	}

	public static void copyToFile(String bucket, String key, File file) throws IOException {
		TransferManager tm = getTransferManager();
		Download download = tm.download(bucket, key, file);

		try {
			download.waitForCompletion();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	public static void copyToS3(File file, String url) throws IOException {
		UrlParts urlParts = getParts(url);
		copyToS3(file, urlParts.bucket(), urlParts.key());
	}

	public static void copyToS3(String content, String url) throws IOException {
		UrlParts urlParts = getParts(url);
		copyToS3(content, urlParts.bucket(), urlParts.key());
	}

	public static void copyToS3(File file, String bucket, String key) throws IOException {
		TransferManager tm = getTransferManager();
		Upload upload = tm.upload(bucket, key, file);

		try {
			upload.waitForCompletion();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	public static void copyToS3(String content, String bucket, String key) throws IOException {
		AmazonS3 s3 = getAmazonS3();
		s3.putObject(bucket, key, content);
	}

	public static ObjectListing listObjects(String url) throws IOException {
		UrlParts urlParts = getParts(url);
		AmazonS3 s3 = getAmazonS3();
		ObjectListing objects = s3.listObjects(urlParts.bucket(), urlParts.key());
		return objects;
	}

	public static void deleteFolder(String url) {
		UrlParts urlParts = getParts(url);
		AmazonS3 s3 = S3Util.getAmazonS3();

		ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(urlParts.bucket()).withPrefix(urlParts.key());
		ObjectListing objectListing = s3.listObjects(listObjectsRequest);

		while (true) {
			for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
				s3.deleteObject(urlParts.bucket(), objectSummary.getKey());
			}
			if (objectListing.isTruncated()) {
				objectListing = s3.listNextBatchOfObjects(objectListing);
			} else {
				break;
			}
		}
	}
}
