package cloudgene.mapred.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GlobUtilTest {

	@Test
	public void testIsFileIncluded() {
		boolean result;

		result = GlobUtil.isFileIncluded("foo/bar", List.of(), List.of());
		assertTrue(result);

		result = GlobUtil.isFileIncluded("foo/bar", List.of(), List.of("foo/"));
		assertTrue(result);

		result = GlobUtil.isFileIncluded("foo/bar", List.of(), List.of("foo/bar"));
		assertFalse(result);

		result = GlobUtil.isFileIncluded("foo/bar", List.of(), List.of("foo/*"));
		assertFalse(result);

		result = GlobUtil.isFileIncluded("foo/bar", List.of(), List.of("baz", "foo/*"));
		assertFalse(result);

		result = GlobUtil.isFileIncluded("foo/bar", List.of("foo/"), List.of());
		assertFalse(result);

		result = GlobUtil.isFileIncluded("foo/bar", List.of("foo/*"), List.of());
		assertTrue(result);

		result = GlobUtil.isFileIncluded("foo/bar", List.of("baz", "foo/*"), List.of());
		assertTrue(result);

		result = GlobUtil.isFileIncluded("foo/bar", List.of("foo/*"), List.of("foo/*"));
		assertFalse(result);

		result = GlobUtil.isFileIncluded("foo/bar", List.of("baz/*"), List.of());
		assertFalse(result);

		result = GlobUtil.isFileIncluded(
				"foo/bar",
				List.of("baz/*", "foo/*", "beep"),
				List.of("hi", "foo/bar", "there"));
		assertFalse(result);
	}
}
