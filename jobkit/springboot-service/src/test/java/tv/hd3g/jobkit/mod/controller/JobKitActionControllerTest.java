package tv.hd3g.jobkit.mod.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.NestedServletException;

import com.fasterxml.jackson.databind.ObjectMapper;

import tv.hd3g.jobkit.engine.BackgroundService;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.mod.BackgroundServiceId;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "DefaultMock" })
class JobKitActionControllerTest {

	private static final String baseMapping = JobKitActionController.class.getAnnotation(RequestMapping.class)
	        .value()[0];
	private static final ResultMatcher statusOk = status().isOk();
	static Random random = new Random();

	@Mock
	HttpServletRequest request;
	@Mock
	BackgroundService backgroundService;
	@Captor
	ArgumentCaptor<Consumer<BackgroundService>> captorConsumerBackgroundService;

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	JobKitEngine jobKitEngine;
	@Autowired
	BackgroundServiceId backgroundServiceId;

	HttpHeaders baseHeaders;
	UUID uuid;

	@BeforeEach
	private void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		Mockito.reset(jobKitEngine, backgroundServiceId);
		// DataGenerator.setupMock(request);

		baseHeaders = new HttpHeaders();
		baseHeaders.setAccept(Arrays.asList(APPLICATION_JSON));
		uuid = UUID.randomUUID();
		when(backgroundServiceId.getByUUID(uuid)).thenReturn(backgroundService);
	}

	@Test
	void testEnable() throws Exception {
		mvc.perform(put(baseMapping + "/" + uuid + "/" + "enable")
		        .headers(baseHeaders))
		        .andExpect(statusOk);

		verify(backgroundServiceId, times(1)).getByUUID(uuid);
		verify(backgroundService, times(1)).enable();
	}

	@Test
	void testDisable() throws Exception {
		mvc.perform(put(baseMapping + "/" + uuid + "/" + "disable")
		        .headers(baseHeaders))
		        .andExpect(statusOk);

		verify(backgroundServiceId, times(1)).getByUUID(uuid);
		verify(backgroundService, times(1)).disable();
	}

	@Test
	void testSetTimedInterval() throws Exception {
		final var duration = Math.abs(random.nextLong());
		mvc.perform(put(baseMapping + "/" + uuid + "/timed-interval/" + duration)
		        .headers(baseHeaders))
		        .andExpect(statusOk);

		verify(backgroundServiceId, times(1)).getByUUID(uuid);

		final var durationCaptor = ArgumentCaptor.forClass(Duration.class);
		verify(backgroundService, times(1)).setTimedInterval(durationCaptor.capture());
		assertEquals(duration, durationCaptor.getValue().toSeconds());
	}

	@Test
	void testSetPriority() throws Exception {
		final var priority = random.nextInt();
		mvc.perform(put(baseMapping + "/" + uuid + "/priority/" + priority)
		        .headers(baseHeaders))
		        .andExpect(statusOk);

		verify(backgroundServiceId, times(1)).getByUUID(uuid);

		final var priorityCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(backgroundService, times(1)).setPriority(priorityCaptor.capture());
		assertEquals(priority, priorityCaptor.getValue().intValue());
	}

	@Test
	void testSetRetryAfterTimeFactor() throws Exception {
		final var factor = random.nextDouble();
		mvc.perform(put(baseMapping + "/" + uuid + "/retry-after-time-factor/" + factor)
		        .headers(baseHeaders))
		        .andExpect(statusOk);

		verify(backgroundServiceId, times(1)).getByUUID(uuid);

		final var factorCaptor = ArgumentCaptor.forClass(Double.class);
		verify(backgroundService, times(1)).setRetryAfterTimeFactor(factorCaptor.capture());
		assertEquals(factor, factorCaptor.getValue().doubleValue());
	}

	@Test
	void testEnableAll() throws Exception {
		mvc.perform(put(baseMapping + "/all/enable")
		        .headers(baseHeaders))
		        .andExpect(statusOk);

		verify(backgroundServiceId, times(1)).forEach(captorConsumerBackgroundService.capture());
		final var consumerBackgroundService = captorConsumerBackgroundService.getValue();
		consumerBackgroundService.accept(backgroundService);
		verify(backgroundService, times(1)).enable();
	}

	@Test
	void testDisableAll() throws Exception {
		mvc.perform(put(baseMapping + "/all/disable")
		        .headers(baseHeaders))
		        .andExpect(statusOk);

		verify(backgroundServiceId, times(1)).forEach(captorConsumerBackgroundService.capture());
		final var consumerBackgroundService = captorConsumerBackgroundService.getValue();
		consumerBackgroundService.accept(backgroundService);
		verify(backgroundService, times(1)).disable();
	}

	@Test
	void testShutdown() throws Exception {
		mvc.perform(put(baseMapping + "/shutdown")
		        .headers(baseHeaders))
		        .andExpect(statusOk);
		verify(jobKitEngine, times(1)).shutdown();
	}

	@Test
	void testActionExceptionFail() throws Exception {
		Mockito.when(backgroundServiceId.getByUUID(any(UUID.class))).thenThrow(new RuntimeException("test"));

		final var builder = put(baseMapping + "/" + uuid + "/" + "enable");
		assertThrows(NestedServletException.class, () -> mvc.perform(builder));

		verify(backgroundServiceId, times(1)).getByUUID(uuid);
		verify(backgroundService, never()).enable();
	}

	@Test
	void testActionExceptionNull() throws Exception {
		Mockito.when(backgroundServiceId.getByUUID(any(UUID.class))).thenReturn(null);

		mvc.perform(put(baseMapping + "/" + uuid + "/" + "enable")
		        .headers(baseHeaders))
		        .andExpect(status().isNotFound())
		        .andExpect(content().contentType(APPLICATION_JSON_VALUE));

		verify(backgroundServiceId, times(1)).getByUUID(uuid);
		verify(backgroundService, never()).enable();
	}

}
