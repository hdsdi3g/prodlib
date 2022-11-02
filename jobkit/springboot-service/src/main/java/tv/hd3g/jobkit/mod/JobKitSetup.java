package tv.hd3g.jobkit.mod;

import static java.lang.Thread.MIN_PRIORITY;
import static java.util.stream.Collectors.joining;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;

import java.util.List;
import java.util.Optional;
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

@Configuration
@Slf4j
public class JobKitSetup {
	@Value("${jobkit.supervisable.maxEndEventsRetention:100}")
	private int maxEndEventsRetention;

	@Bean
	ScheduledThreadPoolExecutor getSchTaskStarter() {
		return new ScheduledThreadPoolExecutor(1, r -> {
			System.out.println("µµµµµµµµµµµµµµµµµµµµµµµµµ");
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
	@Autowired
	JobKitEngine getJobKitEngine(final ScheduledThreadPoolExecutor scheduledExecutor,
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

	static class SupervisableAspectRuntimeHints implements RuntimeHintsRegistrar {//TODO register

		@Override
		public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
			hints.reflection().registerType(WithSupervisable.class,
					builder -> builder.withMembers(INVOKE_DECLARED_METHODS));
			// hints.proxies().registerJdkProxy(FactoryBean.class, BeanClassLoaderAware.class, ApplicationListener.class);
			// hints.proxies().registerJdkProxy(ApplicationAvailability.class, ApplicationListener.class);
			hints.resources().registerResourceBundle("org.aspectj.weaver.weaver-messages");
		}
	}

}
