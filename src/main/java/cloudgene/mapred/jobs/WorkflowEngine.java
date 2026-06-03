package cloudgene.mapred.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.queue.PriorityRunnable;
import cloudgene.mapred.jobs.queue.Queue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

public class WorkflowEngine implements Runnable {

	private Thread threadLongTimeQueue;

	private final Queue longTimeQueue;

	private boolean running = false;

	private final AtomicLong priorityCounter = new AtomicLong();

	public WorkflowEngine(int ltqThreads) {
		longTimeQueue = new Queue("LongTimeQueue", ltqThreads, true, true) {

			@Override
			public PriorityRunnable createRunnable(AbstractJob job) {
				return job;
			}

			@Override
			public void onComplete(AbstractJob job) {
				job.setEndTime(System.currentTimeMillis());
				jobCompleted(job);
			}
		};
	}

	public void submit(AbstractJob job) {
		submit(job, priorityCounter.incrementAndGet());
	}

	public void submit(AbstractJob job, long priority) {
		job.setPriority(priority);
		job.setSubmittedOn(System.currentTimeMillis());
		jobSubmitted(job);

		if (job.afterSubmission()) {
			longTimeQueue.submit(job);
		} else {
			job.setEndTime(System.currentTimeMillis());
			statusUpdated(job);
		}
	}

	public void restart(AbstractJob job) {
		restart(job, priorityCounter.incrementAndGet());
	}

	public void restart(AbstractJob job, long priority) {
		job.setPriority(priority);
		job.setSubmittedOn(System.currentTimeMillis());
		job.setStartTime(0);
		job.setEndTime(0);
		job.setState(AbstractJob.STATE_WAITING);
		statusUpdated(job);

		if (job.afterSubmission()) {
			longTimeQueue.submit(job);
		} else {
			job.setEndTime(System.currentTimeMillis());
			statusUpdated(job);
		}
	}

	public void updatePriority(AbstractJob job, long priority) {
		longTimeQueue.updatePriority(job, priority);
	}

	public void cancel(AbstractJob job) {
		if (longTimeQueue.isInQueue(job)) {
			longTimeQueue.cancel(job);
		}
	}

	@Override
	public void run() {
		threadLongTimeQueue = new Thread(longTimeQueue);
		threadLongTimeQueue.start();
		running = true;
	}

	public void stop() {
		threadLongTimeQueue.stop();
	}

	public void block() {
		longTimeQueue.pause();
		running = false;
	}

	public void resume() {
		longTimeQueue.resume();
		running = true;
	}

	public boolean isRunning() {
		return running && longTimeQueue.isRunning();
	}

	public int getActiveCount() {
		return longTimeQueue.getActiveCount();
	}

	public AbstractJob getJobById(String id) {
		return longTimeQueue.getJobById(id);
	}

	@NonNull
	public Map<String, Long> getCounters(int state, @Nullable List<String> names) {
		Map<String, Long> result = new HashMap<>();
		List<AbstractJob> jobs = longTimeQueue.getAllJobs();

		for (AbstractJob job : jobs) {
			if (job.getState() == state) {
				Map<String, Integer> counters = job.getContext().getCounters();
				List<String> keys = (names == null) ? counters.keySet().stream().toList() : names;
				for (String name : keys) {
					Integer value = counters.get(name);
					Long oldvalue = result.get(name);
					if (oldvalue == null) {
						oldvalue = 0L;
					}
					result.put(name, oldvalue + value);
				}
			}
		}

		return result;
	}

	public List<AbstractJob> getJobsByUser(User user) {
		List<AbstractJob> jobs = longTimeQueue.getJobsByUser(user);

		for (AbstractJob job : jobs) {
			if (job instanceof CloudgeneJob) {
				((CloudgeneJob) job).updateProgress();
			}
		}

		return jobs;
	}

	public List<AbstractJob> getAllJobsInLongTimeQueue() {
		List<AbstractJob> jobs = longTimeQueue.getAllJobs();

		for (AbstractJob job : jobs) {
			if (job instanceof CloudgeneJob) {
				((CloudgeneJob) job).updateProgress();
			}
		}

		return jobs;
	}

	public boolean isInQueue(AbstractJob job) {
		return longTimeQueue.isInQueue(job);
	}

	protected void statusUpdated(AbstractJob job) {
	}

	protected void jobCompleted(AbstractJob job) {
	}

	protected void jobSubmitted(AbstractJob job) {
	}

	public int getSize() {
		return longTimeQueue.getSize();
	}

	public Future<?> getFuture(AbstractJob job) {
		return longTimeQueue.getFuture(job);
	}
}
