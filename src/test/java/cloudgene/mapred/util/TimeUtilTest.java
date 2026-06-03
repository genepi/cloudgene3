package cloudgene.mapred.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeUtilTest {

	@Test
	public void testFormat() {
		long millis;

		millis = getMillis(0, 0, 0, 0);
		assertEquals("0 sec", TimeUtil.format(millis));

		millis = getMillis(0, 0, 1, 349);
		assertEquals("1 sec", TimeUtil.format(millis));

		millis = getMillis(0, 0, 59, 0);
		assertEquals("59 sec", TimeUtil.format(millis));

		millis = getMillis(0, 1, 0, 900);
		assertEquals("1 min 0 sec", TimeUtil.format(millis));

		millis = getMillis(0, 59, 34, 0);
		assertEquals("59 min 34 sec", TimeUtil.format(millis));

		millis = getMillis(1, 0, 3, 777);
		assertEquals("1 h 3 sec", TimeUtil.format(millis));

		millis = getMillis(36, 19, 47, 123);
		assertEquals("36 h 19 min 47 sec", TimeUtil.format(millis));
	}

	private long getMillis(long hours, long minutes, long seconds, long millis) {
		return millis + 1000 * (seconds + 60 * (minutes + 60 * hours));
	}
}
