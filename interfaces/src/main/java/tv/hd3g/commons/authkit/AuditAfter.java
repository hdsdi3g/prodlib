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
package tv.hd3g.commons.authkit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Repeatable(AuditAllAfter.class)
public @interface AuditAfter {

	/**
	 * Audit as this name.
	 */
	String value();

	/**
	 * This action use (read) security: make an audit report.
	 */
	boolean useSecurity() default false;

	/**
	 * This action change (write) security: make an audit report.
	 */
	boolean changeSecurity() default false;

	/**
	 * Make an audit report if an error occurs during this action.
	 */
	boolean cantDoErrors() default false;

}
