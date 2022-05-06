package tv.hd3g.commons.mailkit;

import static net.datafaker.Faker.instance;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.datafaker.service.RandomService;
import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;

class SendMailDtoTest {

	public final static RandomService random = instance().random();

	private SendMailDto sendMailDto;

	private String templateName;
	private String senderAddr;
	private String replyToAddr;
	private MessageGrade grade;
	private String externalReference;
	private String senderReference;

	@Mock
	private Locale lang;
	@Mock
	private Map<String, Object> templateVars;
	@Mock
	private List<String> recipientsAddr;
	@Mock
	private List<String> recipientsCCAddr;
	@Mock
	private List<String> recipientsBCCAddr;
	@Mock
	private SortedSet<File> attachedFiles;
	@Mock
	private Set<String> resourceFiles;

	@BeforeEach
	public void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		templateName = makeRandomString();
		senderAddr = makeRandomString();
		replyToAddr = makeRandomString();
		grade = getRandomEnum(MessageGrade.class);
		externalReference = makeRandomString();
		senderReference = makeRandomString();

		sendMailDto = new SendMailDto(templateName, lang, templateVars, senderAddr,
		        recipientsAddr, recipientsCCAddr, recipientsBCCAddr);
	}

	@Test
	void testSendMailDtoStringLocaleMapOfStringObjectStringListOfStringListOfStringListOfString() {
		final var list = makeRandomThings().collect(Collectors.toList());
		final var recipientsAddr = new String[list.size()];
		for (var pos = 0; pos < recipientsAddr.length; pos++) {
			recipientsAddr[pos] = list.get(pos);
		}

		sendMailDto = new SendMailDto(templateName, lang, templateVars, senderAddr, recipientsAddr);
		assertEquals(templateName, sendMailDto.getTemplateName());
		assertEquals(lang, sendMailDto.getLang());
		assertEquals(templateVars, sendMailDto.getTemplateVars());
		assertEquals(senderAddr, sendMailDto.getSenderAddr());

		final var rList = sendMailDto.getRecipientsAddr();
		final var rRecipientsAddr = new String[rList.size()];
		for (var pos = 0; pos < rRecipientsAddr.length; pos++) {
			rRecipientsAddr[pos] = rList.get(pos);
		}
		assertArrayEquals(recipientsAddr, rRecipientsAddr);
	}

	@Test
	void testGetTemplateName() {
		assertEquals(templateName, sendMailDto.getTemplateName());
	}

	@Test
	void testGetLang() {
		assertEquals(lang, sendMailDto.getLang());
	}

	@Test
	void testGetTemplateVars() {
		assertEquals(templateVars, sendMailDto.getTemplateVars());
	}

	@Test
	void testGetSenderAddr() {
		assertEquals(senderAddr, sendMailDto.getSenderAddr());
	}

	@Test
	void testGetRecipientsAddr() {
		assertEquals(recipientsAddr, sendMailDto.getRecipientsAddr());
	}

	@Test
	void testGetRecipientsCCAddr() {
		assertEquals(recipientsCCAddr, sendMailDto.getRecipientsCCAddr());
	}

	@Test
	void testGetRecipientsBCCAddr() {
		assertEquals(recipientsBCCAddr, sendMailDto.getRecipientsBCCAddr());
	}

	@Test
	void testSetGrade() {
		sendMailDto.setGrade(grade);
		assertEquals(grade, sendMailDto.getGrade());
	}

	@Test
	void testGetGrade() {
		assertNull(sendMailDto.getGrade());
	}

	@Test
	void testSetReplyToAddr() {
		sendMailDto.setReplyToAddr(replyToAddr);
		assertEquals(replyToAddr, sendMailDto.getReplyToAddr());
	}

	@Test
	void testGetReplyToAddr() {
		assertNull(sendMailDto.getReplyToAddr());
	}

	@Test
	void testSetSenderReference() {
		sendMailDto.setSenderReference(senderReference);
		assertEquals(senderReference, sendMailDto.getSenderReference());
	}

	@Test
	void testGetSenderReference() {
		assertNull(sendMailDto.getSenderReference());
	}

	@Test
	void testSetExternalReference() {
		sendMailDto.setExternalReference(externalReference);
		assertEquals(externalReference, sendMailDto.getExternalReference());
	}

	@Test
	void testGetExternalReference() {
		assertNull(sendMailDto.getExternalReference());
	}

	@Test
	void testSetAttachedFiles() {
		sendMailDto.setAttachedFiles(attachedFiles);
		assertEquals(attachedFiles, sendMailDto.getAttachedFiles());
	}

	@Test
	void testGetAttachedFiles() {
		assertNull(sendMailDto.getAttachedFiles());
	}

	@Test
	void testSetResourceFiles() {
		sendMailDto.setResourceFiles(resourceFiles);
		assertEquals(resourceFiles, sendMailDto.getResourceFiles());
	}

	@Test
	void testGetResourceFiles() {
		assertNull(sendMailDto.getResourceFiles());
	}

	public static <T extends Enum<?>> T getRandomEnum(final Class<T> enum_class) {
		final var x = random.nextInt(enum_class.getEnumConstants().length);
		return enum_class.getEnumConstants()[x];
	}

	public static String makeRandomString() {
		return RandomStringUtils.randomAscii(5000, 10000);
	}

	public static String makeRandomThing() {
		switch (random.nextInt(10)) {
		case 0:
			return instance().aviation().aircraft();
		case 1:
			return instance().app().name();
		case 2:
			return instance().commerce().material();
		case 3:
			return instance().company().name();
		case 4:
			return instance().food().dish();
		case 5:
			return instance().food().ingredient();
		case 6:
			return instance().food().fruit();
		case 7:
			return instance().food().spice();
		case 8:
			return instance().food().sushi();
		case 9:
			return instance().food().vegetable();
		default:
			return instance().food().vegetable();
		}
	}

	public static Stream<String> makeRandomThings() {
		return IntStream.range(0, random.nextInt(1, 20)).distinct().mapToObj(i -> makeRandomThing());
	}

}
