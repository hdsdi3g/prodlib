package tv.hd3g.jobkit.mod;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import tv.hd3g.jobkit.engine.JobKitEngine;

@Configuration
@EnableAsync
public class JobKitAsyncConfigurer implements AsyncConfigurer {

	public static final String POOL_NAME = "springboot";

	private static final Logger log = LogManager.getLogger();

	@Autowired
	JobKitEngine jobKitEngine;

	@Override
	public Executor getAsyncExecutor() {
		final var pattern = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss,S");
		return task -> {
			final var dateText = LocalDateTime.now().format(pattern);
			final var sended = jobKitEngine.runOneShot("SpringBoot Async " + dateText, POOL_NAME, 0, task, e -> {
				if (e != null) {
					log.error("Can't execute SpringBootAsync task", e);
				}
			});
			if (sended == false) {
				log.error("Can't queue new @async executor task, jobKitEngine refuse new jobs");
			}
		};
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (e, method, params) -> {
			final var className = method.getDeclaringClass().getName();
			final var methodName = method.getName();
			final var fullParams = Stream.of(method.getParameters())
			        .map(p -> p.getType().getSimpleName() + " " + p.getName())
			        .collect(Collectors.joining(", "));
			log.error("UncaughtException during SpringBootAsync task: {}.{}({})", className, methodName, fullParams, e);
		};
	}

}
