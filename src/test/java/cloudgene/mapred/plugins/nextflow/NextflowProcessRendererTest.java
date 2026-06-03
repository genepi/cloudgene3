package cloudgene.mapred.plugins.nextflow;

import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.jobs.CloudgeneJob;
import cloudgene.mapred.jobs.Message;
import cloudgene.mapred.jobs.Step;
import io.micronaut.core.annotation.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class NextflowProcessRendererTest {

	// TODO(Marc): Test other message types.
	private static Stream<Arguments> provideForRender() {
		return Stream.of(
				// configLabel | processName | configView | processTasks | msgRegex

				// The config label takes priority over the process name for the displayed
				// label.
				arguments(null, "Newt Scamander", "label", List.of(), Message.RUNNING, "^Newt Scamander$"),
				arguments("Tom Bombadil", "Newt Scamander", "label", List.of(), Message.RUNNING, "^Tom Bombadil$"),

				// list reads tasks
				arguments("Index Table", "Technique Proceedings", "list", List.of(), Message.RUNNING,
						"^<b>Index Table \\(0/1\\)</b>\\s*<br>\\s*<small>\\s*Initial Task"),

				// If we re-use the task_id, only the latest trace data shows up (this is about
				// the *Process rather than the *Renderer).
				arguments(
						"Index Table",
						"Technique Proceedings",
						"list",
						List.of(Map.of(
								"name", "Overriding the Default",
								"task_id", 0, // same as og trace
								"status", "RUNNING")),
						Message.RUNNING,
						"^<b>Index Table \\(0/1\\)</b>\\s*<br>\\s*<small>\\s*Overriding the Default"),

				// Multiple tasks are displayed in order.
				arguments(
						"Index Table",
						"Technique Proceedings",
						"list",
						List.of(
								Map.of(
										"name", "Update #1",
										"task_id", 1,
										"status", "RUNNING"),
								Map.of(
										"name", "Update #2",
										"task_id", 2,
										"status", "RUNNING"),
								Map.of(
										"name", "Update #3",
										"task_id", 3,
										"status", "COMPLETED")),
						Message.RUNNING,
						"^<b>Index Table \\(1/4\\)</b>.*<small>\\s*Initial Task.*</small>.*<small>\\s*Update #1.*</small>.*<small>\\s*Update #2.*</small>"),

				// 'status' doesn't display SUBMITTED jobs.
				arguments(
						"Index Table",
						"Technique Proceedings",
						"status",
						List.of(),
						Message.RUNNING,
						"^\\s*$"),

				// In 'status', RUNNING jobs show up as "Processing data..."
				// Here we're overriding the original trace by using the same task_id.
				arguments(
						"Index Table",
						"Technique Proceedings",
						"status",
						List.of(Map.of(
								"name", "Overriding the Default",
								"task_id", 0,
								"status", "RUNNING")),
						Message.RUNNING,
						"^\\s*Processing data...\\s*$"),

				// In 'status', KILLED jobs show up as "Data processing failed."
				// Prints all available, so we don't need to override.
				arguments(
						"Index Table",
						"Technique Proceedings",
						"status",
						List.of(
								Map.of(
										"name", "I'm Dead",
										"task_id", 1,
										"status", "KILLED")),
						Message.RUNNING,
						"^\\s*Data processing failed.\\s*$"),

				// 'progressbar' renders an accessible Bootstrap 5 multi-progress bar.
				arguments(
						"Advancement Cylinder",
						"Technique Proceedings",
						"progressbar",
						List.of(
								Map.of(
										"name", "S1",
										"task_id", 1,
										"status", "COMPLETED"),
								Map.of(
										"name", "S2",
										"task_id", 2,
										"status", "COMPLETED"),
								Map.of(
										"name", "R3",
										"task_id", 3,
										"status", "SUBMITTED"),
								Map.of(
										"name", "R4",
										"task_id", 4,
										"status", "RUNNING"),
								Map.of(
										"name", "R5",
										"task_id", 5,
										"status", "RUNNING"),
								Map.of(
										"name", "F6",
										"task_id", 6,
										"status", "FAILED")),
						Message.RUNNING,
						"Advancement Cylinder \\(2/7\\).*<div class=\"progress\""));
	}

	@ParameterizedTest
	@MethodSource("provideForRender")
	public void testRender(
			String configLabel,
			String processName,
			String configView,
			List<Map<String, Object>> processTraces,
			int expectedType,
			String msgRegex) throws IOException {

		NextflowProcessConfig config = new NextflowProcessConfig();
		config.setLabel(configLabel);
		config.setView(configView);

		NextflowProcess process = makeProcess(processName, processTraces);

		Message message = new Message();

		NextflowProcessRenderer.render(config, process, message);

		String observedMessage = message.getMessage();
		int observedType = message.getType();

		assertNotNull(observedMessage);
		Pattern rx = Pattern.compile(msgRegex);
		assertTrue(rx.matcher(observedMessage).find());

		assertEquals(expectedType, observedType);
	}

	private static @NonNull NextflowProcess makeProcess(@NonNull String name, @NonNull List<Map<String, Object>> traces)
			throws IOException {
		User user = new User();
		user.setMail("foo@bar.com");
		user.setUsername("foo");

		CloudgeneJob job = new CloudgeneJob();
		job.setUser(user);
		job.setId("job-id-123");
		job.setLocalWorkspace("local/path");

		CloudgeneContext context = new CloudgeneContext(job);

		Map<String, Object> ogTrace = Map.of(
				"process", name,
				"name", "Initial Task",
				"task_id", 0,
				"status", "SUBMITTED");

		Step step = new Step();

		NextflowProcess process = new NextflowProcess(context, ogTrace, step);
		for (Map<String, Object> trace : traces) {
			process.addTrace(trace);
		}

		return process;
	}
}
