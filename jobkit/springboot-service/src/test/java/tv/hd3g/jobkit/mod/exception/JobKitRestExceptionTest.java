package tv.hd3g.jobkit.mod.exception;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

class JobKitRestExceptionTest {

	static Random random = new Random();

	@Test
	void testGetReturnCode_default() {
		final JobKitRestException e = new JobKitRestException("message");
		assertEquals("message", e.getMessage());
		assertEquals(SC_BAD_REQUEST, e.getReturnCode());
	}

	@Test
	void testGetReturnCode_specified() {
		final var code = random.nextInt();
		final JobKitRestException e = new JobKitRestException(code, "message2");
		assertEquals("message2", e.getMessage());
		assertEquals(code, e.getReturnCode());
	}

}
