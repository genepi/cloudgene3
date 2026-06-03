package cloudgene.mapred.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.bcrypt.BCrypt;

public final class HashUtil {

	private HashUtil() {}

	/**
	 * Returns a secure random alphanumeric string with 64 characters.
	 */
	public static String getSecureHash() {
		return RandomStringUtils.secure().nextAlphanumeric(64);
	}

	/**
	 * Calculates the SHA-256 digest and returns the value as a hex string.
	 * (wrapps {@link org.apache.commons.codec.digest.DigestUtils}{@code .sha256Hex()}).
	 */
	public static String getSha256(String name) {
		return DigestUtils.sha256Hex(name);
	}

	/** Return BCrypt hash from input */
	public static String hashPassword(String input) {
		String hashed = DigestUtils.md5Hex(input);
		return BCrypt.hashpw(hashed, BCrypt.gensalt());
	}

	/** Check if a provided candidate password is the same as an existing hash */
	public static boolean checkPassword(String candidate, String hash) {
		String hashedCandidate = DigestUtils.md5Hex(candidate);
		return BCrypt.checkpw(hashedCandidate, hash);
	}
}
