/*
 * This file is part of env-version.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package tv.hd3g.commons.version;

import java.lang.management.ManagementFactory;
import java.util.Date;

public record EnvironmentVersion(
								 String appVersion,
								 String prodlibVersion,
								 String frameworkVersion,
								 String jvmVersion,
								 String jvmNameVendor,
								 long pid,
								 Date startupTime) {

	public static EnvironmentVersion makeEnvironmentVersion(final String appVersion,
										  final String prodlibVersion,
										  final String frameworkVersion) {
		final var jvmVersion = Runtime.version().toString();
		final var pid = ManagementFactory.getRuntimeMXBean().getPid();
		final var startupTime = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
		final var jvmNameVendor = ManagementFactory.getRuntimeMXBean().getVmName() + " " +
								  ManagementFactory.getRuntimeMXBean().getVmVendor();

		return new EnvironmentVersion(
				appVersion,
				prodlibVersion,
				frameworkVersion,
				jvmVersion,
				jvmNameVendor,
				pid,
				startupTime);
	}

}
