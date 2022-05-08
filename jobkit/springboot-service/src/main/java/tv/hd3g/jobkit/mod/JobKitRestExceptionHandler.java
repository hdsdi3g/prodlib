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
package tv.hd3g.jobkit.mod;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import tv.hd3g.jobkit.mod.exception.JobKitRestException;

/**
 * See https://www.toptal.com/java/spring-boot-rest-api-error-handling
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class JobKitRestExceptionHandler extends ResponseEntityExceptionHandler {
	private static Logger log = LogManager.getLogger();

	@ExceptionHandler(JobKitRestException.class)
	protected ResponseEntity<Object> handleRESTException(final JobKitRestException e, final WebRequest request) {
		log.warn("REST Error for {}", request.getDescription(true), e);
		return new ResponseEntity<>(Map.of("message", e.getMessage()),
		        Optional.ofNullable(HttpStatus.resolve(e.getReturnCode())).orElse(HttpStatus.INTERNAL_SERVER_ERROR));
	}
}
