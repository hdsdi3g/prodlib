package tv.hd3g.commons.mailkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;

class MessageGradeTest {

	@Test
	void testGetMessagePriority() {
		assertEquals(3, MessageGrade.EVENT_NOTICE.getMessagePriority());
		assertEquals(3, MessageGrade.MARKETING.getMessagePriority());
		assertEquals(1, MessageGrade.SECURITY.getMessagePriority());
		assertEquals(4, MessageGrade.TEST.getMessagePriority());
	}

}
