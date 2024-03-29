package tv.hd3g.jobkit.mod;

import static java.lang.Thread.MIN_PRIORITY;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.jobkit.engine.BackgroundServiceEvent;
import tv.hd3g.jobkit.engine.ExecutionEvent;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.engine.SupervisableManager;
import tv.hd3g.jobkit.engine.SupervisableServiceSupplier;

@Configuration
@Slf4j
public class JobKitSetup {
	@Value("${jobkit.supervisable.maxEndEventsRetention:100}")
	private int maxEndEventsRetention;

	@Bean
	ScheduledExecutorService getScheduledExecutor() {
		return new ScheduledThreadPoolExecutor(1, r -> {
			final var t = new Thread(r);
			t.setDaemon(false);
			t.setPriority(MIN_PRIORITY + 1);
			t.setName("SchTaskStarter");
			t.setUncaughtExceptionHandler((thrd, e) -> log.error("Regular scheduled thread have an uncaught error", e));
			return t;
		});
	}

	@Bean
	SupervisableManager getSupervisableManager(final ApplicationContext applicationContext,
											   final ObjectMapper jacksonObjectMapper) {
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

		return new SupervisableManager((appName + " " + env).trim(), jacksonObjectMapper, maxEndEventsRetention);
	}

	@Bean
	SupervisableServiceSupplier getSupervisableSupplier(final SupervisableManager supervisableManager) {
		return new SupervisableServiceSupplier(supervisableManager);
	}

	@Bean
	@Autowired
	JobKitEngine getJobKitEngine(final ScheduledExecutorService scheduledExecutor,
								 final ExecutionEvent executionEvent,
								 final BackgroundServiceEvent backgroundServiceEvent,
								 final SupervisableManager supervisableManager,
								 final JobKitWatchdogConfig watchdogConfig) {
		final var jobKit = new JobKitEngine(scheduledExecutor, executionEvent, backgroundServiceEvent,
				supervisableManager);
		final var watchdog = jobKit.getJobKitWatchdog();
		watchdogConfig.getMaxSpoolQueueSize().forEach(watchdog::addPolicies);
		watchdogConfig.getLimitedExecTime().forEach(watchdog::addPolicies);
		watchdogConfig.getLimitedServiceExecTime().forEach(watchdog::addPolicies);
		return jobKit;
	}

}
