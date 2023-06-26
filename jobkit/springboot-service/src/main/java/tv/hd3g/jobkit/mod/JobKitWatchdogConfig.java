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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package tv.hd3g.jobkit.mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import tv.hd3g.jobkit.engine.watchdog.LimitedExecTimePolicy;
import tv.hd3g.jobkit.engine.watchdog.LimitedServiceExecTimePolicy;
import tv.hd3g.jobkit.engine.watchdog.MaxSpoolQueueSizePolicy;

@Configuration
@ConfigurationProperties(prefix = "jobkit.watchdogpolicies")
@Data
public class JobKitWatchdogConfig {

	private List<MaxSpoolQueueSizePolicy> maxSpoolQueueSize;
	private List<LimitedExecTimePolicy> limitedExecTime;
	private List<LimitedServiceExecTimePolicy> limitedServiceExecTime;

	@PostConstruct
	void init() {
		maxSpoolQueueSize = Optional.ofNullable(maxSpoolQueueSize).orElse(new ArrayList<>());
		limitedExecTime = Optional.ofNullable(limitedExecTime).orElse(new ArrayList<>());
		limitedServiceExecTime = Optional.ofNullable(limitedServiceExecTime).orElse(new ArrayList<>());
	}

}
