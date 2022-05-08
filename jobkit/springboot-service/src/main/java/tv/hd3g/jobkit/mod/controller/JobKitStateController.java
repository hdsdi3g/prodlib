package tv.hd3g.jobkit.mod.controller;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tv.hd3g.commons.authkit.CheckBefore;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.mod.BackgroundServiceId;
import tv.hd3g.jobkit.mod.dto.BackgroundServiceIdDto;
import tv.hd3g.jobkit.mod.dto.JobKitEngineStatusDto;

@RestController
@CheckBefore("jobkitState")
@RequestMapping(value = "/v1/jobkit/state", produces = APPLICATION_JSON_VALUE)
public class JobKitStateController {

	@Autowired
	private JobKitEngine jobKitEngine;
	@Autowired
	private BackgroundServiceId backgroundServiceId;

	@GetMapping(value = "status")
	@CheckBefore("jobkitStatus")
	public ResponseEntity<JobKitEngineStatusDto> getLastStatus() {
		final var result = new JobKitEngineStatusDto(jobKitEngine.getLastStatus());
		return new ResponseEntity<>(result, OK);
	}

	@GetMapping(value = "ids")
	@CheckBefore("jobkitStatus")
	public ResponseEntity<BackgroundServiceIdDto> getIds() {
		final var result = backgroundServiceId.getAllRegistedAsDto();
		return new ResponseEntity<>(result, OK);
	}

}
