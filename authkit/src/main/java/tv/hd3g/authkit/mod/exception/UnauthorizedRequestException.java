/*
 * This file is part of authkit.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2021
 *
 */
package tv.hd3g.authkit.mod.exception;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import tv.hd3g.authkit.mod.service.AuditReportService;

public class UnauthorizedRequestException extends SecurityRejectedRequestException {

	public UnauthorizedRequestException(final String logMessage) {
		super(logMessage, UNAUTHORIZED);
	}

	public UnauthorizedRequestException(final String logMessage, final String userUUID) {
		super(logMessage, UNAUTHORIZED, UUID.fromString(userUUID));
	}

	@Override
	public void pushAudit(final AuditReportService auditService, final HttpServletRequest request) {
		auditService.interceptUnauthorizedRequest(request);
	}
}
