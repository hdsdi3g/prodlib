package tv.hd3g.mailkit.mod.service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public interface SendMailToFileService {

	void write(MimeMessage mimeMessage) throws MessagingException;

}
