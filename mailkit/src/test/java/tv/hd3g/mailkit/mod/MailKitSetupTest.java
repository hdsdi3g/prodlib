package tv.hd3g.mailkit.mod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;

import tv.hd3g.mailkit.mod.configuration.MailKitSetup;

@SpringBootTest
class MailKitSetupTest {

	@Autowired
	MailKitSetup mailKitSetup;
	@Autowired
	TemplateEngine htmlTemplateEngine;
	@Autowired
	TemplateEngine subjectTemplateEngine;

	@Test
	void testHtmlTemplateEngine() {
		assertEquals(htmlTemplateEngine, mailKitSetup.htmlTemplateEngine());
		assertNotNull(htmlTemplateEngine);
		assertEquals(1, htmlTemplateEngine.getTemplateResolvers().size());
		assertEquals(2, htmlTemplateEngine.getMessageResolvers().size());
	}

	@Test
	void testSubjectTemplateEngine() {
		assertEquals(subjectTemplateEngine, mailKitSetup.subjectTemplateEngine());
		assertNotNull(subjectTemplateEngine);
		assertEquals(1, subjectTemplateEngine.getTemplateResolvers().size());
		assertEquals(2, subjectTemplateEngine.getMessageResolvers().size());
	}

}
