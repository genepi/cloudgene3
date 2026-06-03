package cloudgene.mapred.util;

import cloudgene.mapred.test.TestApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@MicronautTest
public class BinaryFinderTest {

	@Inject
	TestApplication application;

	@Test
	public void testPath() {
		BinaryFinder finder;
		String file;

		finder = new BinaryFinder("binary-finder.txt");

		// If no method is invoked, the file is null.
		file = finder.find();
		assertNull(file);

		// If the provided path does not exist, find() returns null.
		file = finder.path("test-data/this-dir-does-not-exist").find();
		assertNull(file);

		// If the provided path is a dir but does not contain an appropriately named
		// file, find() returns null.
		file = finder.path("test-data/cloudgene-config-basic").find();
		assertNull(file);

		// If the provided dir exists and contains a correctly named file, find() gives
		// us the path to the file.
		file = finder.path("test-data").find();
		assertEquals("test-data/binary-finder.txt", file);

		// Once a valid path has been found, it stays cached.
		file = finder.path("test-data/this-dir-does-not-exist").find();
		assertEquals("test-data/binary-finder.txt", file);

		// Let's reset the finder.
		finder = new BinaryFinder("binary-finder.txt");

		// Even if two paths are valid, find() returns the first valid path.
		// (also, .path() is chainable)
		file = finder.path("test-data/binary-finder-dir").path("test-data").find();
		assertEquals("test-data/binary-finder-dir/binary-finder.txt", file);
	}

	// TODO(Marc): Plugins seem abandoned. Remove?
	@Test
	public void testSettings() {
		BinaryFinder finder;
		String file;

		Settings settings = application.getSettings();
		settings.setPlugins(
				Map.of(
						"foo", Map.of(
								"bar", "test-data",
								"baz", "test-data/binary-finder-dir",
								"cloudgene", "test-data/cloudgene-config-basic",
								"fake", "does-not-exist")));

		// Plugin 'foo' exists, parameter 'bar' exists, 'bar' points to a valid path,
		// binary present -> we get the full path.
		finder = new BinaryFinder("binary-finder.txt");
		file = finder.settings(settings, "foo", "bar").find();
		assertEquals("test-data/binary-finder.txt", file);

		// Plugin 'foo' exists, parameter 'baz' exists, 'baz' points to a valid path,
		// binary present -> we get the full path.
		finder = new BinaryFinder("binary-finder.txt");
		file = finder.settings(settings, "foo", "baz").find();
		assertEquals("test-data/binary-finder-dir/binary-finder.txt", file);

		// Plugin 'foo' exists, parameter 'baz' exists, 'cloudgene' points to a valid
		// path, binary missing -> null.
		finder = new BinaryFinder("binary-finder.txt");
		file = finder.settings(settings, "foo", "cloudgene").find();
		assertNull(file);

		// Plugin 'foo' exists, parameter 'fake' exists, 'fake' does NOT point to a
		// valid path -> null.
		finder = new BinaryFinder("binary-finder.txt");
		file = finder.settings(settings, "foo", "fake").find();
		assertNull(file);

		// Plugin 'foo' exists, parameter 'missing-parameter' does not exist -> null.
		finder = new BinaryFinder("binary-finder.txt");
		file = finder.settings(settings, "foo", "missing-parameter").find();
		assertNull(file);

		// Plugin 'hello' does NOT exist -> null.
		finder = new BinaryFinder("binary-finder.txt");
		file = finder.settings(settings, "hello", "there").find();
		assertNull(file);
	}
}
