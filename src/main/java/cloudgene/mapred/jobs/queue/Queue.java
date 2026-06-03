package cloudgene.mapred.jobs.queue;

import java.util.*;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.AbstractJob;

public abstract class Queue implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(Queue.class);

	private static final int POLL_FREQUENCY_MS = 100;

	private final List<AbstractJob> queue;
	private final HashMap<AbstractJob, Future<?>> futures;
	private final HashMap<AbstractJob, PriorityRunnable> runnables;
	private final PriorityThreadPoolExecutor scheduler;
	private final String name;
	private final boolean updatePositions;
	private final boolean priority;

	public Queue(String name, int threads, boolean updatePositions, boolean priority) {
		this.name = name;
		this.updatePositions = updatePositions;
		this.priority = priority;

		futures = new HashMap<>();
		runnables = new HashMap<>();
		queue = new Vector<>();
		scheduler = new PriorityThreadPoolExecutor(threads, priority);
	}

	public void submit(AbstractJob job) {
		synchronized (futures) {
			synchronized (queue) {
				PriorityRunnable runnable = createRunnable(job);
				runnables.put(job, runnable);

				Future<?> future = scheduler.submit(runnable);
				futures.put(job, future);
				queue.add(job);
				log.info("{}: Submit job{}...", name, priority ? " (P: " + job.getPriority() + ")" : "");

				if (priority) {
					// sort by state and by priority
					queue.sort(new PriorityComparator());
				}

				if (updatePositions) {
					updatePositionInQueue();
				}
			}
		}
	}

	public synchronized void cancel(AbstractJob job) {
		if (job.getState() == AbstractJob.STATE_RUNNING || job.getState() == AbstractJob.STATE_EXPORTING) {
			log.info("{}: Cancel running job {}...", name, job.getId());

			job.cancel();
			job.kill();

			log.info("{}: Job {} canceled.", name, job.getId());

			if (updatePositions) {
				updatePositionInQueue();
			}
		} else if (job.getState() == AbstractJob.STATE_WAITING) {
			log.info("{}: Cancel waiting job {}...", name, job.getId());

			synchronized (futures) {
				synchronized (queue) {

					PriorityRunnable runnable = runnables.get(job);
					if (runnable != null) {
						System.out.println("Kill runnable");
						scheduler.kill(runnable);
						runnables.remove(job);
					}

					job.cancel();
					queue.remove(job);
					futures.remove(job);
					onComplete(job);

					log.info("{}: Job {} canceled.", name, job.getId());

					if (updatePositions) {
						updatePositionInQueue();
					}
				}
			}
		} else {
			log.info("{}: Cancel job {}. Unkown state: {}", name, job.getId(), job.getState());
		}
	}

	@Override
	public void run() {
		List<AbstractJob> complete = new Vector<>();

		while (true) {
			try {
				synchronized (futures) {
					synchronized (queue) {

						complete.clear();

						for (AbstractJob job : futures.keySet()) {
							Future<?> future = futures.get(job);
							if (future.isDone() || future.isCancelled()) {
								log.info("{}: Job {}: finished", name, job.getId());
								queue.remove(job);
								complete.add(job);
							}
						}

						for (AbstractJob job : complete) {
							try {
								onComplete(job);
							} catch (Exception e) {
								log.warn("{}: Job {}: On complete failed. ", name, job.getId(), e);
							}
							futures.remove(job);
							if (updatePositions) {
								updatePositionInQueue();
							}
						}
					}
				}
			} catch (Exception e) {
				log.warn("{}: Concurrency Exception!! ", name, e);
			}

			try {
				Thread.sleep(POLL_FREQUENCY_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void pause() {
		log.info("{}: Pause...", name);
		scheduler.pause();
	}

	public void resume() {
		log.info("{}: Resume...", name);
		scheduler.resume();
	}

	public boolean isRunning() {
		return scheduler.isRunning();
	}

	public int getActiveCount() {
		return scheduler.getActiveCount();
	}

	public List<AbstractJob> getJobsByUser(User user) {
		List<AbstractJob> result = new Vector<>();

		synchronized (queue) {
			for (AbstractJob job : queue) {
				if (job.getUser().getId() == user.getId()) {
					result.add(job);
				}
			}
		}

		return result;
	}

	public List<AbstractJob> getAllJobs() {
		List<AbstractJob> result;

		synchronized (queue) {
			result = new Vector<>(queue);
		}

		return result;
	}

	public AbstractJob getJobById(String id) {
		synchronized (queue) {
			for (AbstractJob job : queue) {
				if (job.getId().equals(id)) {
					return job;
				}
			}
		}

		return null;
	}

	protected void updatePositionInQueue() {
		synchronized (queue) {
			int position = 0;
			for (AbstractJob job : queue) {
				job.setPositionInQueue(position);
				if (job.getState() == AbstractJob.STATE_WAITING) {
					position++;
				}
			}
		}
	}

	public boolean updatePriority(AbstractJob job, long priority) {
		log.info("Update priority");

		if (!this.priority) {
			return false;
		}

		if (!isInQueue(job)) {
			return false;
		}

		if (job.getState() != AbstractJob.STATE_WAITING) {
			return false;
		}

		synchronized (futures) {
			synchronized (queue) {
				Future<?> oldFuture = futures.get(job);
				if (oldFuture != null) {
					oldFuture.cancel(false);
				}
				job.setPriority(priority);
				Future<?> future = scheduler.resubmit(job);
				if (future != null) {
					futures.put(job, future);
					log.info("{}: Update priority of {} (P: {})...", name, job.getId(), job.getPriority());

					// Sort by state and by priority
					queue.sort(new PriorityComparator());

					if (updatePositions) {
						updatePositionInQueue();
					}

					return true;
				}
			}
		}

		return false;
	}

	public boolean isInQueue(AbstractJob job) {
		synchronized (queue) {
			return queue.contains(job);
		}
	}

	public Future<?> getFuture(AbstractJob job) {
		synchronized (futures) {
			return futures.get(job);
		}
	}

	public int getSize() {
		return queue.size();
	}

	abstract public void onComplete(AbstractJob job);

	abstract public PriorityRunnable createRunnable(AbstractJob job);

	protected static class PriorityComparator implements Comparator<AbstractJob> {

		@Override
		public int compare(AbstractJob o1, AbstractJob o2) {
			if (o1.getState() != o2.getState()) {
				if (o1.getState() == AbstractJob.STATE_RUNNING) {
					return -1;
				} else {
					return 1;
				}
			}

			return Long.compare(o1.getPriority(), o2.getPriority());
		}
	}
}
