package tv.hd3g.jobkit.mod.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import tv.hd3g.jobkit.engine.status.JobKitEngineStatus;
import tv.hd3g.testtools.HashCodeEqualsTest;

class JobKitEngineStatusDtoTest extends HashCodeEqualsTest {

	@Mock
	JobKitEngineStatus jobKitEngineStatus;

	JobKitEngineStatusDto jobKitEngineStatusDto;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		jobKitEngineStatusDto = new JobKitEngineStatusDto(jobKitEngineStatus);
	}

	@Test
	void testGetLastStatus() {
		assertEquals(jobKitEngineStatus, jobKitEngineStatusDto.getLastStatus());
	}

	@Override
	protected Object[] makeSameInstances() {
		return new Object[] { jobKitEngineStatusDto, new JobKitEngineStatusDto(jobKitEngineStatus) };
	}

}
