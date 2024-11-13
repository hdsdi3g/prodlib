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

import static org.assertj.core.api.Assertions.assertThat;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.dontKeepChunk;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.dontKeepChunkThenMarkActualArchived;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.dontKeepChunkThenMarkActualDeleted;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.keepChunk;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.keepChunkThenMarkActualArchived;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.keepChunkThenMarkActualDeleted;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DatablockMigrateChunkPolicyTest {

	DatablockMigrateChunkPolicy p;

	@Test
	void testKeepChunk() {
		p = keepChunk();

		assertThat(p.keepActualChunk()).isTrue();
		assertThat(p.markActualChunkAsArchived()).isFalse();
		assertThat(p.markActualChunkAsDeleted()).isFalse();
	}

	@Test
	void testDontKeepChunk() {
		p = dontKeepChunk();

		assertThat(p.keepActualChunk()).isFalse();
		assertThat(p.markActualChunkAsArchived()).isFalse();
		assertThat(p.markActualChunkAsDeleted()).isFalse();
	}

	@Test
	void testKeepChunkMarkActualDeleted() {
		p = keepChunkThenMarkActualDeleted();

		assertThat(p.keepActualChunk()).isTrue();
		assertThat(p.markActualChunkAsArchived()).isFalse();
		assertThat(p.markActualChunkAsDeleted()).isTrue();
	}

	@Test
	void testKeepChunkMarkActualArchived() {
		p = keepChunkThenMarkActualArchived();

		assertThat(p.keepActualChunk()).isTrue();
		assertThat(p.markActualChunkAsArchived()).isTrue();
		assertThat(p.markActualChunkAsDeleted()).isFalse();
	}

	@Test
	void testDontKeepChunkMarkActualDeleted() {
		p = dontKeepChunkThenMarkActualDeleted();

		assertThat(p.keepActualChunk()).isFalse();
		assertThat(p.markActualChunkAsArchived()).isFalse();
		assertThat(p.markActualChunkAsDeleted()).isTrue();
	}

	@Test
	void testDontKeepChunkMarkActualArchived() {
		p = dontKeepChunkThenMarkActualArchived();

		assertThat(p.keepActualChunk()).isFalse();
		assertThat(p.markActualChunkAsArchived()).isTrue();
		assertThat(p.markActualChunkAsDeleted()).isFalse();
	}

	@ParameterizedTest
	@MethodSource("tv.hd3g.commons.testtools.MockToolsExtendsJunit#provide3Booleans")
	void testChangeActualChunk(final boolean keep, final boolean pArchived, final boolean pDeleted) {
		assertThat(new DatablockMigrateChunkPolicy(keep, pArchived, pDeleted).changeActualChunk())
				.isEqualTo(pArchived || pDeleted);
	}
}
