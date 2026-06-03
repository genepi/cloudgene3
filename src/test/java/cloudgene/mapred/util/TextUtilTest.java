package cloudgene.mapred.util;

import genepi.io.FileUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TextUtilTest {

	@Test
	public void testTail() throws IOException {
		File file = new File("test-data/all-possible-inputs.yaml");
		String fullContents = FileUtil.readFileAsString(file.getPath()).replace("\r", "");

		String expected;
		String observed;

		// Asking for 6 lines gives us the last 5 text lines and the trailing empty line.
		expected =
				"    - id: output\n" +
						"      description: OutputFile\n" +
						"      type: file\n" +
						"      download: true\n" +
						"      temp: false\n";
		observed = TextUtil.tail(file, 6);
		assertEquals(expected, observed);

		// Asking for 3 lines gives us the last 2 text lines and the trailing empty line.
		expected = "      download: true\n" +
				"      temp: false\n";
		observed = TextUtil.tail(file, 3);
		assertEquals(expected, observed);

		// Asking for 1 line skips the last line and returns the rest of the text.
		// TODO(Marc): This is undesirable behavior.
		expected = fullContents.trim();
		observed = TextUtil.tail(file, 1);
		assertEquals(expected, observed);

		// Asking for <= 0 lines gives us the whole text.
		expected = fullContents;
		observed = TextUtil.tail(file, 0);
		assertEquals(expected, observed);

		// Asking for <= 0 lines gives us the whole text.
		expected = fullContents;
		observed = TextUtil.tail(file, -23);
		assertEquals(expected, observed);

		// Asking for more lines than the file has returns the whole file.
		expected = fullContents;
		observed = TextUtil.tail(file, 999);
		assertEquals(expected, observed);
	}
}
