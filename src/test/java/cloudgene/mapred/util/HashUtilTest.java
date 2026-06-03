package cloudgene.mapred.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class HashUtilTest {

	@Test
	public void testActivationHash() {
		String hash = HashUtil.getSecureHash();
		assert64CharAlnum(hash);
	}

	@Test
	public void testGetSha256() {
		List<String> passwords = List.of("", " ", "a", "a ", " A", "1", "11", "111", "pwd", "foobar123",
				"P@assW0rd!Test");

		List<String> hashes = passwords.stream().map(HashUtil::getSha256).toList();

		// Triangle comparison: we want to check each element by itself, but also
		// against each following element.
		for (int i = 0; i < hashes.size(); ++i) {
			String first = hashes.get(i);

			// Own check: hash is a 64-character lowercase hex string.
			assert64CharHex(first);

			for (int j = i + 1; j < hashes.size(); ++j) {
				String second = hashes.get(j);

				// Comparison check: different strings produce different hashes.
				assertNotEquals(first, second);
			}
		}

		// The process is deterministic.
		List<String> moreHashes = passwords.stream().map(HashUtil::getSha256).toList();
		assertEquals(hashes, moreHashes);
	}

	@Test
	public void testBCryptHash() {
		String password = "P@assW0rd!Test";

		// Multiple hashes are different but still verifiable against the same password.
		String hash1 = HashUtil.hashPassword(password);
		String hash2 = HashUtil.hashPassword(password);

		// Basic sanity checks.
		assertNotNull(hash1);
		assertFalse(hash1.isEmpty());
		assertNotNull(hash2);
		assertFalse(hash2.isEmpty());

		// Both verifiable...
		assertTrue(HashUtil.checkPassword(password, hash1));
		assertTrue(HashUtil.checkPassword(password, hash2));

		// ...but different.
		assertNotEquals(hash1, hash2);
	}

	private void assert64CharHex(String test) {
		// Basic sanity checks.
		assertNotNull(test);
		assertFalse(test.isBlank());

		// Hash is a 64-digit hex number.
		Pattern format = Pattern.compile("[a-f0-9]{64}");
		assertTrue(format.matcher(test).matches());
	}

	private void assert64CharAlnum(String test) {
		// Basic sanity checks.
		assertNotNull(test);
		assertFalse(test.isBlank());

		// Hash is a 64-digit hex number.
		Pattern format = Pattern.compile("[a-zA-Z0-9]{64}");
		assertTrue(format.matcher(test).matches());
	}
}
