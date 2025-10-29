package cloudgene.mapred.util;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

@MicronautTest
public class SettingsTest {

    @Test
    public void testLoadFileBasic() throws Exception {
        Settings settings = withEnvironmentVariable("CG_CONFIG_DIRECTORY", "test-data/cloudgene-config-basic")
                .execute(Settings::load);
        assertEquals("Test Name 123", settings.getName());
    }

    @Test
    public void testLoadFileNoUrl() {
        assertThrows(IOException.class, () ->
                withEnvironmentVariable("CG_CONFIG_DIRECTORY", "test-data/cloudgene-config-no-url")
                        .execute(Settings::load)
        );
    }

    @Test
    public void testGetTempFilename() throws Exception {
        Settings settings = Settings.load();
        String filename = settings.getTempFilename("foo.txt");
        assertEquals("tmp" + File.separator + "foo.txt", filename);
    }

    @Test
    public void testGetExternalWorkspace() throws Exception {
        Settings settings = Settings.load();

        // External workspace initially unset
        assertNull(settings.getExternalWorkspace());

        String defaultLocation = settings.getExternalWorkspaceLocation();

        // Defaults to local workspace, which defaults to "workspace"
        assertEquals("workspace", defaultLocation);

        Map<String, String> externalWorkspace = settings.getExternalWorkspace();

        // Calling the method has initialized the external workspace to { type="local", location="workspace" }
        assertNotNull(externalWorkspace);
        assertEquals("local", externalWorkspace.get("type"));
        assertEquals("workspace", externalWorkspace.get("location"));
    }

}
