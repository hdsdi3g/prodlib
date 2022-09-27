/*
 * This file is part of MailKit.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.commons.mailkit;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

public class SendMailDto {

	private final String templateName;
	private final Locale lang;
	private final Map<String, Object> templateVars;

	private final String senderAddr;
	private final List<String> recipientsAddr;
	private final List<String> recipientsCCAddr;
	private final List<String> recipientsBCCAddr;

	private String replyToAddr;
	private MessageGrade grade;
	private String externalReference;
	private String senderReference;
	private SortedSet<File> attachedFiles;
	private Set<String> resourceFiles;

	public enum MessageGrade {
		EVENT_NOTICE(3),
		MARKETING(3),
		URGENT(2),
		SECURITY(1),
		TEST(4);

		private final int messagePriority;

		MessageGrade(final int messagePriority) {
			this.messagePriority = messagePriority;
		}

		public int getMessagePriority() {
			return messagePriority;
		}
	}

	public SendMailDto(final String templateName,
					   final Locale lang,
					   final Map<String, Object> templateVars,
					   final String senderAddr,
					   final List<String> recipientsAddr,
					   final List<String> recipientsCCAddr,
					   final List<String> recipientsBCCAddr) {
		this.templateName = Objects.requireNonNull(templateName);
		this.lang = Objects.requireNonNull(lang);
		this.templateVars = Objects.requireNonNull(templateVars);
		this.senderAddr = Objects.requireNonNull(senderAddr);
		this.recipientsAddr = Objects.requireNonNull(recipientsAddr);
		this.recipientsCCAddr = Objects.requireNonNull(recipientsCCAddr);
		this.recipientsBCCAddr = Objects.requireNonNull(recipientsBCCAddr);
	}

	public SendMailDto(final String templateName,
					   final Locale lang,
					   final Map<String, Object> templateVars,
					   final String senderAddr,
					   final String... recipientsAddr) {
		this(templateName, lang, templateVars, senderAddr,
				List.of(Objects.requireNonNull(recipientsAddr)),
				List.of(),
				List.of());
	}

	public void setReplyToAddr(final String replyToAddr) {
		this.replyToAddr = replyToAddr;
	}

	public void setExternalReference(final String externalReference) {
		this.externalReference = externalReference;
	}

	public void setSenderReference(final String senderReference) {
		this.senderReference = senderReference;
	}

	public void setGrade(final MessageGrade grade) {
		this.grade = grade;
	}

	public String getTemplateName() {
		return templateName;
	}

	public Locale getLang() {
		return lang;
	}

	public Map<String, Object> getTemplateVars() {
		return templateVars;
	}

	public String getSenderAddr() {
		return senderAddr;
	}

	public List<String> getRecipientsAddr() {
		return recipientsAddr;
	}

	public List<String> getRecipientsCCAddr() {
		return recipientsCCAddr;
	}

	public List<String> getRecipientsBCCAddr() {
		return recipientsBCCAddr;
	}

	public MessageGrade getGrade() {
		return grade;
	}

	/**
	 * @return can be null
	 */
	public String getExternalReference() {
		return externalReference;
	}

	/**
	 * @return can be null
	 */
	public String getSenderReference() {
		return senderReference;
	}

	/**
	 * @return can be null
	 */
	public String getReplyToAddr() {
		return replyToAddr;
	}

	/**
	 * @return can be null
	 */
	public SortedSet<File> getAttachedFiles() {
		return attachedFiles;
	}

	public void setAttachedFiles(final SortedSet<File> attachedFiles) {
		this.attachedFiles = attachedFiles;
	}

	/**
	 * @return can be null
	 */
	public Set<String> getResourceFiles() {
		return resourceFiles;
	}

	public void setResourceFiles(final Set<String> resourceFiles) {
		this.resourceFiles = resourceFiles;
	}
}
