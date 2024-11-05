/*
 * This file is part of datablock.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2024
 *
 */
package tv.hd3g.datablock;

public record DatablockMigrateChunkPolicy(boolean keepActualChunk,
										  boolean markActualChunkAsArchived,
										  boolean markActualChunkAsDeleted) {

	public static DatablockMigrateChunkPolicy keepChunk() {
		return new DatablockMigrateChunkPolicy(true, false, false);
	}

	public static DatablockMigrateChunkPolicy dontKeepChunk() {
		return new DatablockMigrateChunkPolicy(false, false, false);
	}

	public static DatablockMigrateChunkPolicy keepChunkMarkActualDeleted() {
		return new DatablockMigrateChunkPolicy(true, false, true);
	}

	public static DatablockMigrateChunkPolicy keepChunkMarkActualArchived() {
		return new DatablockMigrateChunkPolicy(true, true, false);
	}

	public static DatablockMigrateChunkPolicy dontKeepChunkMarkActualDeleted() {
		return new DatablockMigrateChunkPolicy(false, false, true);
	}

	public static DatablockMigrateChunkPolicy dontKeepChunkMarkActualArchived() {
		return new DatablockMigrateChunkPolicy(false, true, false);
	}

	boolean changeActualChunk() {
		return markActualChunkAsArchived || markActualChunkAsDeleted;
	}

}
