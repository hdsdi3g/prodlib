package tv.hd3g.mailkit.mod.service;

import static java.io.File.separator;
import static java.lang.System.lineSeparator;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SendMailToFileServiceImpl implements SendMailToFileService, InitializingBean {
	private static Logger log = LogManager.getLogger();

	@Value("${mailkit.sendtoFile:#{null}}")
	private File sendtoFile;
	@Value("${mailkit.sendtoFileWipeOnStart:false}")
	private boolean wipeOnStart;
	@Value("${mailkit.sendtoFileAutomaticExt:false}")
	private boolean fileAutomaticExt;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (sendtoFile != null) {
			log.debug("Prepare MailKit output file mail directory in {}", sendtoFile);
			FileUtils.forceMkdir(sendtoFile);
			if (wipeOnStart) {
				log.debug("Clean all content of output file mail directory in {}", sendtoFile);
				FileUtils.cleanDirectory(sendtoFile);
			}
		}
	}

	@Override
	public void write(final MimeMessage mimeMessage) throws MessagingException {
		Objects.requireNonNull(sendtoFile);
		final var dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSSZ");
		log.debug("Prepare mail files of \"{}\"", mimeMessage.getSubject());

		try {
			final var baseName = dateFormat.format(new Date());
			log.info("Write mail files of \"{}\" to {}{}{}/*",
			        mimeMessage.getSubject(),
			        sendtoFile.getPath(), separator, baseName);

			final var partList = new ArrayList<String>();
			extractPart(mimeMessage, baseName, partList);
			final var headers = partList.stream().collect(Collectors.joining(lineSeparator()));

			final var headersPath = Path.of(sendtoFile.getPath(), baseName + "_headers.txt");
			Files.writeString(headersPath, headers, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void extractMultipart(final Multipart multipart,
	                              final String baseName,
	                              final List<String> partList) throws MessagingException, IOException {
		final var count = multipart.getCount();
		for (var pos = 0; pos < count; pos++) {
			final var ref = "_mp" + pos;
			partList.add("=== start multipart: " + pos + " ===");
			extractPart(multipart.getBodyPart(pos), baseName + ref, partList);
			partList.add("=== end multipart: " + pos + " ===");
		}
	}

	private void extractPart(final Part part,
	                         final String baseName,
	                         final List<String> partList) throws MessagingException, IOException {
		final var content = part.getContent();

		partList.addAll(StreamSupport.stream(spliteratorUnknownSize(
		        part.getAllHeaders().asIterator(), ORDERED), false)
		        .map(header -> header.getName() + ": " + header.getValue())
		        .collect(toUnmodifiableList()));

		if (part.getSize() > -1) {
			partList.add("Size: " + part.getSize());
		}
		if (part.getLineCount() > -1) {
			partList.add("LineCount: " + part.getLineCount());
		}
		Optional.ofNullable(part.getContentType()).ifPresent(t -> partList.add("ContentType: " + t));
		Optional.ofNullable(part.getDescription()).ifPresent(t -> partList.add("Description: " + t));
		Optional.ofNullable(part.getFileName()).ifPresent(t -> partList.add("FileName: " + t));

		if (content instanceof String) {
			final var strContent = (String) content;
			var extension = ".txt";
			if (fileAutomaticExt
			    && (strContent.startsWith("<!DOCTYPE html>")
			        || strContent.contains("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\""))) {
				extension = ".html";
			}
			final var contentPath = Path.of(sendtoFile.getPath(), baseName + extension);
			Files.writeString(contentPath, (String) content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		} else if (content instanceof MimeMultipart) {
			extractMultipart((Multipart) content, baseName, partList);
		} else {
			log.error("Can't parse \"{}\" mail type in {}", content.getClass(), baseName);
		}
	}

}
