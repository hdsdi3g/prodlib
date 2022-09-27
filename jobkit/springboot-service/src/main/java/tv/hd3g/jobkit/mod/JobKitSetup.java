package tv.hd3g.jobkit.mod;

import static java.lang.Thread.MIN_PRIORITY;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import tv.hd3g.jobkit.engine.BackgroundServiceEvent;
import tv.hd3g.jobkit.engine.ExecutionEvent;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.engine.SupervisableManager;

@Configuration
public class JobKitSetup {

	private static final Logger log = LogManager.getLogger();

	@Bean
	ScheduledExecutorService getScheduledExecutor() {
		return new ScheduledThreadPoolExecutor(1, r -> {
			final var t = new Thread(r);
			t.setDaemon(false);
			t.setPriority(MIN_PRIORITY + 1);
			t.setName("SchTaskStarter");
			t.setUncaughtExceptionHandler((thrd, e) -> log.fatal("Regular scheduled thread have an uncaught error", e));
			return t;
		});
	}

	@Bean
	public SupervisableManager getSupervisableManager(final ApplicationContext applicationContext) {
		var appName = applicationContext.getApplicationName();
		if (appName.isEmpty()) {
			appName = "Default";
		}
		final var env = Optional.ofNullable(applicationContext.getEnvironment())
				.map(Environment::getActiveProfiles)
				.map(List::of)
				.map(List::stream)
				.map(s -> s.collect(joining(", ")))
				.orElse("");

		return new SupervisableManager((appName + " " + env).trim());
	}

	@Bean
	@Autowired
	JobKitEngine getJobKitEngine(final ScheduledExecutorService scheduledExecutor,
								 final ExecutionEvent executionEvent,
								 final BackgroundServiceEvent backgroundServiceEvent,
								 final SupervisableManager supervisableManager) {
		return new JobKitEngine(scheduledExecutor, executionEvent, backgroundServiceEvent, supervisableManager);
	}

}
