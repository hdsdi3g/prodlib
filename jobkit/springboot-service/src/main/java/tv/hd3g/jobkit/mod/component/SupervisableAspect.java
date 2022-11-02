/*
 * This file is part of jobkit.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2022
 *
 */
package tv.hd3g.jobkit.mod.component;

import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;

import tv.hd3g.jobkit.WithSupervisable;
import tv.hd3g.jobkit.engine.Supervisable;
import tv.hd3g.jobkit.engine.SupervisableServiceSupplier;

// @Aspect
// @Component
public class SupervisableAspect {

	@Autowired
	private SupervisableServiceSupplier supervisableServiceSupplier;

	@Around("annotationWithSupervisable()")
	public Object manageSupervisable(final ProceedingJoinPoint joinPoint) throws Throwable {
		try {
			Supervisable.getSupervisable();
			return joinPoint.proceed(joinPoint.getArgs());
		} catch (final IllegalThreadStateException e) {
			/**
			 * Now, we will need to create it manually
			 */
		}

		var jobName = ((MethodSignature) joinPoint.getSignature())
				.getMethod()
				.getAnnotation(WithSupervisable.class)
				.value();
		if (jobName.isEmpty()) {
			jobName = joinPoint.getSignature().getDeclaringType().getSimpleName()
					  + "."
					  + joinPoint.getSignature().getName();
		}
		final var supervisable = supervisableServiceSupplier.createAndStart(jobName);
		try {
			final var result = joinPoint.proceed(joinPoint.getArgs());
			supervisableServiceSupplier.end(supervisable, Optional.empty());
			return result;
		} catch (final Exception e) {
			supervisableServiceSupplier.end(supervisable, Optional.ofNullable(e));
			throw e;
		}
	}

	@Pointcut("@annotation(tv.hd3g.jobkit.WithSupervisable)")
	public void annotationWithSupervisable() {
		/**
		 *
		 */
	}
}
