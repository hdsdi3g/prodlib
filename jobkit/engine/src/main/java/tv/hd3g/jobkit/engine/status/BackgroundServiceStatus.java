package tv.hd3g.jobkit.engine.status;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import tv.hd3g.jobkit.engine.BackgroundService;

public class BackgroundServiceStatus {

	private final String name;
	private final String spoolName;
	private final boolean enabled;
	private final long nextRunReferenceDelay;
	private final long timedInterval;
	private final long previousScheduledDate;
	private final int priority;
	private final double retryAfterTimeFactor;
	private final int sequentialErrorCount;
	private final String task;

	public BackgroundServiceStatus(final String name,
	                               final String spoolName,
	                               final long nextRunReferenceDelay,
	                               final long previousScheduledDate,
	                               final BackgroundService backgroundService,
	                               final int sequentialErrorCount,
	                               final String task) {
		this.name = name;
		this.spoolName = spoolName;
		enabled = backgroundService.isEnabled();
		this.nextRunReferenceDelay = nextRunReferenceDelay;
		timedInterval = backgroundService.getTimedInterval(MILLISECONDS);
		this.previousScheduledDate = previousScheduledDate;
		priority = backgroundService.getPriority();
		retryAfterTimeFactor = backgroundService.getRetryAfterTimeFactor();
		this.sequentialErrorCount = sequentialErrorCount;
		this.task = task;
	}

	public String getName() {
		return name;
	}

	public String getSpoolName() {
		return spoolName;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public long getNextRunReferenceDelay() {
		return nextRunReferenceDelay;
	}

	public long getTimedInterval() {
		return timedInterval;
	}

	public long getPreviousScheduledDate() {
		return previousScheduledDate;
	}

	public int getPriority() {
		return priority;
	}

	public double getRetryAfterTimeFactor() {
		return retryAfterTimeFactor;
	}

	public int getSequentialErrorCount() {
		return sequentialErrorCount;
	}

	public String getTask() {
		return task;
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("BackgroundServiceStatus [");
		if (name != null) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if (spoolName != null) {
			builder.append("spoolName=");
			builder.append(spoolName);
			builder.append(", ");
		}
		builder.append("enabled=");
		builder.append(enabled);
		builder.append(", nextRunReferenceDelay=");
		builder.append(nextRunReferenceDelay);
		builder.append(", timedInterval=");
		builder.append(timedInterval);
		builder.append(", previousScheduledDate=");
		builder.append(previousScheduledDate);
		builder.append(", priority=");
		builder.append(priority);
		builder.append(", retryAfterTimeFactor=");
		builder.append(retryAfterTimeFactor);
		builder.append(", sequentialErrorCount=");
		builder.append(sequentialErrorCount);
		builder.append(", ");
		if (task != null) {
			builder.append("task=");
			builder.append(task.getClass());
		}
		builder.append("]");
		return builder.toString();
	}

}
