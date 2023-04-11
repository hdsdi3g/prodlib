/*
 * This file is part of jobkit-watchfolder-jpa.
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
package tv.hd3g.jobkit.watchfolder.mod.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFileScanner;

@Component
public class WatchedFileScannerSupplier {

	@Value("${watchfolder.maxDeep:10}")
	private int maxDeep;

	public WatchedFileScanner create(final ObservedFolder observedFolder) {
		return new WatchedFileScanner(observedFolder, maxDeep);
	}

}
