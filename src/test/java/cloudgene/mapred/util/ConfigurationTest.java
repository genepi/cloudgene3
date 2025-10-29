package cloudgene.mapred.util;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

@MicronautTest
public class ConfigurationTest {

    @Test
    public void testGet() {
        String shouldDefault = Configuration.get("DOES_NOT_EXIST", "default-value");
        assertEquals("default-value", shouldDefault);

        String shouldRead = Configuration.get("PATH", "default-value");
        assertNotEquals("default-value", shouldRead);
    }

    @Test public void testGetConfigDirectory() throws Exception {
        String shouldDefault = Configuration.getConfigDirectory();
        assertEquals("config", shouldDefault);

        String shouldRead = withEnvironmentVariable("CG_CONFIG_DIRECTORY", "modified-value")
                .execute(Configuration::getConfigDirectory);
        assertEquals("modified-value", shouldRead);
    }
}
