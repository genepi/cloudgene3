package cloudgene.mapred.jobs;

import cloudgene.mapred.jobs.workspace.IWorkspace;
import cloudgene.mapred.jobs.workspace.WorkspaceFactory;
import cloudgene.mapred.util.FormUtil;
import cloudgene.mapred.wdl.WdlApp;
import cloudgene.mapred.wdl.WdlReader;
import genepi.io.FileUtil;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class JobParameterParserTest {

    @Inject
    WorkspaceFactory workspaceFactory;

    /**
     * Helper factory for parameter name-value pairs.
     */
    private static FormUtil.Parameter param(String name, Object value) {
        return new FormUtil.Parameter(name, value);
    }

    /**
     * Helper factory for maps with nullable keys and values. Similar interface to Map.of()
     *
     * @param parts key1 (String), value1 (Object), key2 (String), value2 (Object), ... (requires an even number of entries)
     */
    private static Map<String, Object> map(Object... parts) {
        Map<String, Object> out = new HashMap<>();
        for (int i = 0; i < parts.length; i += 2) {
            String key = (String)parts[i];
            Object value = parts[i+1];
            out.put(key, value);
        }
        return out;
    }

    @BeforeEach
    public void setup() {
        File inputDir = new File("input");
        if (inputDir.exists()) {
            FileUtil.deleteDirectory(inputDir);
        }

        File uploadFileOriginal = new File("test-data/foo.txt");
        File uploadFileCopy = new File("test-data/foo-up.txt");

        if (!uploadFileCopy.exists()) {
            assert(uploadFileOriginal.isFile());
            FileUtil.copy(uploadFileOriginal.toString(), uploadFileCopy.toString());
        }
    }

    @AfterEach
    public void teardown() {
        File inputDir = new File("input");
        if (inputDir.exists()) {
            FileUtil.deleteDirectory(inputDir);
        }
    }

    private static Stream<Arguments> provideTestParseArgs() {
        return Stream.of(

                // Simplest case: nothing in, empty job-name out
                Arguments.of(
                        "test-data/return-true.yaml", // appFile
                        List.of(), // form
                        map("job-name", null), // expected
                        null // errorMessage
                ),

                // Expected param in, same param out
                Arguments.of(
                        "test-data/return-true.yaml", // appFile
                        List.of(param("input", "hello.txt")), // form
                        map( // expected
                                "input", "hello.txt",
                                "job-name", null
                        ),
                        null // errorMessage
                ),

                // Repeated param in, first version out
                Arguments.of(
                        "test-data/return-true.yaml", // appFile
                        List.of( // form
                                param("input", "hello.txt"),
                                param("input", "sailor.txt")
                        ),
                        map( // expected
                                "input", "hello.txt",
                                "job-name", null
                        ),
                        null // errorMessage
                ),

                // 'input-' prefix gets ignored
                Arguments.of(
                        "test-data/return-true.yaml", // appFile
                        List.of(param("input-input", "hello.txt")), // form
                        map( // expected
                                "input", "hello.txt",
                                "job-name", null
                        ),
                        null // errorMessage
                ),

                // Expected param and job-name in, same out
                Arguments.of(
                        "test-data/return-true.yaml", // appFile
                        List.of( // form
                                param("input", "bye"),
                                param("job-name", "hello-world")
                        ),
                        map( // expected
                                "input", "bye",
                                "job-name", "hello-world"
                        ),
                        null // errorMessage
                ),

                // Adding a non-existing parameter (that doesn't match *-pattern) -> error raised.
                Arguments.of(
                        "test-data/return-true.yaml", // appFile
                        List.of( // form
                                param("input", "bye"),
                                param("job-name", "hello-world"),
                                param("does-not-exist", "in-this-app")
                        ),
                        null, // expected
                        "Parameter 'does-not-exist' not found." // errorMessage
                ),

                // Adding a *-pattern input that doesn't exist -> gets ignored.
                Arguments.of(
                        "test-data/return-true.yaml", // appFile
                        List.of( // form
                                param("input", "bye"),
                                param("job-name", "ok"),
                                param("some-pattern", "some-value")
                        ),
                        map( // expected
                                "input", "bye",
                                "job-name", "ok"
                        ),
                        null // errorMessage
                ),

                // If there are visible checkboxes in the app and they're missing from props,
                // they get filled to the contents of the false value:
                // App YAML > 'inputs' > (match list item by 'id') > 'values' > 'false' > (value)
                //
                // NOTE: This application has several inputs we're ignoring. By default, Cloudgene inputs are marked as
                // `required=true`, and the UI will enforce that they are filled. In imputationserver2 for example, no
                // inputs explicitly set `required=false`, so they're all considered essential. However, there is no
                // validation at any point that distinguishes mandatory form optional inputs, and we know for a fact
                // some imputationserver2 inputs are optional. Ideally, we'd want to validate that all mandatory inputs
                // are provided
                Arguments.of(
                        "test-data/all-possible-inputs.yaml", // appFile
                        List.of(), // form
                        map( // expected
                                "job-name", null,
                                "checkbox", "valueFalse"
                        ),
                        null // errorMessage
                ),

                // Setting a checkbox to ANY value returns the true value:
                // App YAML > 'inputs' > (match list item by 'id') > 'values' > 'true' > (value)
                Arguments.of(
                        "test-data/all-possible-inputs.yaml", // appFile
                        List.of(param("checkbox", "potato")), // form
                        map( // expected
                                "job-name", null,
                                "checkbox", "valueTrue"
                        ),
                        null // errorMessage
                ),

                // ...even if you literally set it to the bool value `false` or the string "false"
                Arguments.of(
                        "test-data/all-possible-inputs.yaml", // appFile
                        List.of(param("checkbox", false)), // form
                        map( // expected
                                "job-name", null,
                                "checkbox", "valueTrue"
                        ),
                        null // errorMessage
                ),

                // Passing a value of type `java.io.File` causes it to be uploaded to a location determined by the workspace.
                Arguments.of(
                        "test-data/all-possible-inputs.yaml", // appFile
                        List.of(param("file", new File("test-data/foo-up.txt"))), // form
                        map( // expected
                                "job-name", null,
                                "file", "input/file/foo-up.txt",
                                "checkbox", "valueFalse"
                        ),
                        null // errorMessage
                ),

                // ...however, passing a string in the same key-value pair just returns the same string, doing nothing.
                Arguments.of(
                        "test-data/all-possible-inputs.yaml", // appFile
                        List.of(param("file", "test-data/foo-up.txt")), // form
                        map( // expected
                                "job-name", null,
                                "file", "test-data/foo-up.txt",
                                "checkbox", "valueFalse"
                        ),
                        null // errorMessage
                ),

                // PATTERNS: If (1) input `foo` exists, and (2) `foo` is of type folder, and (3) a `pattern` field is
                // provided in the input description in the application YAML file, then `foo-pattern` is used to
                // override the default pattern. In all other cases, `foo-pattern` does not raise an error, but gets
                // silently discarded.
                Arguments.of(
                        "test-data/all-possible-inputs.yaml", // appFile
                        List.of( // form
                                param("folder-glob", "test-data"),
                                param("folder-glob-pattern", "*.yaml")
                        ),
                        map( // expected
                                "job-name", null,
                                "folder-glob", "test-data/*.yaml",
                                "checkbox", "valueFalse"
                        ),
                        null // errorMessage
                )
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestParseArgs")
    public void testParse(String appFile, List<FormUtil.Parameter> form, Map<String, String> expected, String errorMessage) throws Exception {
        WdlApp app = WdlReader.loadAppFromFile(appFile);
        IWorkspace workspace = workspaceFactory.getDefault();

        if (expected != null) {
            assert(errorMessage == null);

            Map<String, String> observed = JobParameterParser.parse(form, app, workspace);
            assertEquals(expected, observed);

        } else {
            assert(errorMessage != null);

            try {
                JobParameterParser.parse(form, app, workspace);
                fail("Expected error to be thrown");
            } catch (Exception e) {
                assertEquals(e.getMessage(), errorMessage);
            }
        }
    }

    @Test
    public void testParseFile() throws Exception {
        WdlApp app = WdlReader.loadAppFromFile("test-data/all-possible-inputs.yaml");
        IWorkspace workspace = workspaceFactory.getDefault();
        File uploadFolder = new File("input");

        // ---------------------------------------------------------------- //
        // Passing a string -> nothing gets uploaded
        // ---------------------------------------------------------------- //

        List<FormUtil.Parameter> formWithString = List.of(
                param("file", "test-data/foo-up.txt")
        );

        Map<String, Object> expectedWithString = map(
                "job-name", null,
                "file", "test-data/foo-up.txt",
                "checkbox", "valueFalse"
        );

        assertFalse(uploadFolder.exists()); // Upload folder does not exist at the beginning.

        Map<String, String> observedWithString = JobParameterParser.parse(formWithString, app, workspace);
        assertEquals(expectedWithString, observedWithString);

        assertFalse(uploadFolder.exists()); // Upload folder is NOT created when passing a string.

        // ---------------------------------------------------------------- //
        // Passing a java.io.File -> file gets uploaded
        // ---------------------------------------------------------------- //

        List<FormUtil.Parameter> formWithFile = List.of(
                param("file", new File("test-data/foo-up.txt"))
        );

        Map<String, Object> expectedWithFile = map(
                "job-name", null,
                "file", "input/file/foo-up.txt",
                "checkbox", "valueFalse"
        );

        assertFalse(uploadFolder.exists()); // Upload folder still hasn't been created yet.

        Map<String, String> observedWithFile = JobParameterParser.parse(formWithFile, app, workspace);
        assertEquals(expectedWithFile, observedWithFile);

        assertTrue(uploadFolder.exists()); // Upload folder was created when calling parse() with a File input.
        assertTrue(new File("input/file/foo-up.txt").isFile()); // The specific file exists in there.
    }

}
