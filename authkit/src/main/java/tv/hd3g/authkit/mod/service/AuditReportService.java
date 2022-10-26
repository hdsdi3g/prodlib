/*
 * This file is part of AuthKit.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.authkit.mod.service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import tv.hd3g.authkit.mod.entity.Audit;

public interface AuditReportService {

	/**
	 * @return Event ref
	 */
	String interceptUnauthorizedRequest(HttpServletRequest request);

	/**
	 * @return Event ref
	 */
	String interceptForbiddenRequest(HttpServletRequest request);

	/**
	 * @return Event ref
	 */
	String onImportantError(HttpServletRequest request, List<String> names, Exception e);

	/**
	 * @return Event ref
	 */
	String onChangeSecurity(HttpServletRequest request, List<String> names);

	/**
	 * @return Event ref
	 */
	String onUseSecurity(HttpServletRequest request, List<String> names);

	/**
	 * @return Event ref
	 */
	String onSimpleEvent(HttpServletRequest request, List<String> names);

	/**
	 * @return Event ref
	 */
	String onRejectLogin(HttpServletRequest request, RejectLoginCause cause, String realm, String what);

	/**
	 * @return Event ref
	 */
	String onLogin(HttpServletRequest request, Duration longSessionDuration, Set<String> tags);

	/**
	 * @return Event ref
	 */
	String onReport(HttpServletRequest request,
	                String reportName,
	                String subject,
	                Duration sinceTime);

	Collection<Audit> reportLastUserActivities(HttpServletRequest originalRequest,
	                                           String userUUID,
	                                           Duration sinceTime);

	Collection<Audit> reportLastRemoteIPActivity(HttpServletRequest originalRequest,
	                                             String address,
	                                             Duration sinceTime);

	Collection<Audit> reportLastEventActivity(HttpServletRequest originalRequest,
	                                          String eventName,
	                                          Duration sinceTime);

	Collection<String> reportAllEventNames(HttpServletRequest originalRequest);

	Collection<String> reportLastClientsourcehosts(HttpServletRequest originalRequest,
	                                               Duration sinceTime);

	public enum RejectLoginCause {
		USER_NOT_FOUND("Can't found user from login name and realm", false),
		MISSING_PASSWORD("User has not send a password", true),
		EMPTY_PASSWORD("User has send an empty password", true),
		INVALID_PASSWORD("User has send an INVALID password", false),
		DISABLED_LOGIN("User login is set to disabled", false);

		private final String cause;
		private final boolean noPasswordUser;

		RejectLoginCause(final String cause, final boolean noPasswordUser) {
			this.cause = cause;
			this.noPasswordUser = noPasswordUser;
		}

		@Override
		public String toString() {
			return cause;
		}

		public boolean isNoPasswordUser() {
			return noPasswordUser;
		}
	}
}
