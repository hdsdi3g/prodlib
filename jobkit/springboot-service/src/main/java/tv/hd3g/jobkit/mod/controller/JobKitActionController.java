package tv.hd3g.jobkit.mod.controller;

import static java.time.temporal.ChronoUnit.SECONDS;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tv.hd3g.commons.authkit.CheckBefore;
import tv.hd3g.jobkit.engine.BackgroundService;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.mod.BackgroundServiceId;
import tv.hd3g.jobkit.mod.exception.JobKitRestException;

@RestController
@CheckBefore("jobkitAction")
@RequestMapping(value = "/v1/jobkit/action", produces = APPLICATION_JSON_VALUE)
@Validated
public class JobKitActionController {

	@Autowired
	private JobKitEngine jobKitEngine;
	@Autowired
	private BackgroundServiceId backgroundServiceId;

	@PutMapping(value = "{uuid}/enable")
	public ResponseEntity<Object> enable(@PathVariable("uuid") @NotEmpty final String uuid) {
		getBackgroundServiceByUUID(uuid).enable();
		return new ResponseEntity<>(OK);
	}

	@PutMapping(value = "{uuid}/disable")
	public ResponseEntity<Object> disable(@PathVariable("uuid") @NotEmpty final String uuid) {
		getBackgroundServiceByUUID(uuid).disable();
		return new ResponseEntity<>(OK);
	}

	@PutMapping(value = "{uuid}/timed-interval/{duration}")
	public ResponseEntity<Object> setTimedInterval(@PathVariable("uuid") @NotEmpty final String uuid,
	                                               @PathVariable("duration") @Positive final long duration) {
		getBackgroundServiceByUUID(uuid).setTimedInterval(Duration.of(duration, SECONDS));
		return new ResponseEntity<>(OK);
	}

	@PutMapping(value = "{uuid}/priority/{priority}")
	public ResponseEntity<Object> setPriority(@PathVariable("uuid") @NotEmpty final String uuid,
	                                          @PathVariable("priority") @NotNull final int priority) {
		getBackgroundServiceByUUID(uuid).setPriority(priority);
		return new ResponseEntity<>(OK);
	}

	@PutMapping(value = "{uuid}/retry-after-time-factor/{factor}")
	public ResponseEntity<Object> setRetryAfterTimeFactor(@PathVariable("uuid") @NotEmpty final String uuid,
	                                                      @PathVariable("factor") @Positive final double factor) {
		getBackgroundServiceByUUID(uuid).setRetryAfterTimeFactor(factor);
		return new ResponseEntity<>(OK);
	}

	private BackgroundService getBackgroundServiceByUUID(final String uuid) {
		return Optional.ofNullable(backgroundServiceId.getByUUID(UUID.fromString(uuid)))
		        .orElseThrow(() -> new JobKitRestException(SC_NOT_FOUND, "Can't found this service UUID"));
	}

	@PutMapping(value = "all/enable")
	public ResponseEntity<Object> enableAll() {
		backgroundServiceId.forEach(BackgroundService::enable);
		return new ResponseEntity<>(OK);
	}

	@PutMapping(value = "all/disable")
	public ResponseEntity<Object> disableAll() {
		backgroundServiceId.forEach(BackgroundService::disable);
		return new ResponseEntity<>(OK);
	}

	@PutMapping(value = "shutdown")
	public ResponseEntity<Object> shutdown() {
		jobKitEngine.shutdown();
		return new ResponseEntity<>(OK);
	}

}
