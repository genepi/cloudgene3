package cloudgene.mapred.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapValueParserTest {

	@Test
	public void testParseMap() {
		Map<String, Object> input;
		Map<String, Object> observed;
		Map<String, Object> expected;

		input = Map.of();
		observed = MapValueParser.parseMap(input);
		expected = Map.of();
		assertEquals(expected, observed);

		input = Map.of("foo", "42");
		observed = MapValueParser.parseMap(input);
		expected = Map.of("foo", 42);
		assertEquals(expected, observed);

		input = Map.of(
				"foo", "42",
				"bar", "13.37");
		observed = MapValueParser.parseMap(input);
		expected = Map.of("foo", 42,
				"bar", 13.37);
		assertEquals(expected, observed);

		input = Map.of(
				"foo", "42",
				"bar", Map.of(
						"baz", "13.37",
						"hello", "true",
						"sailor", "hi there"));
		observed = MapValueParser.parseMap(input);
		expected = Map.of(
				"foo", 42,
				"bar", Map.of(
						"baz", 13.37,
						"hello", true,
						"sailor", "hi there"));
		assertEquals(expected, observed);

		// Nesting level doesn't matter; booleans are case-insensitive.
		input = Map.of("very", Map.of("nested", Map.of("tree", Map.of("value", "FALSE"))));
		observed = MapValueParser.parseMap(input);
		expected = Map.of("very", Map.of("nested", Map.of("tree", Map.of("value", false))));
		assertEquals(expected, observed);
	}
}
