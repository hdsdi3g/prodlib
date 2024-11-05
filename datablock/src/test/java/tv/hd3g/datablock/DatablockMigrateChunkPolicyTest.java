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
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.dontKeepChunkMarkActualArchived;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.dontKeepChunkMarkActualDeleted;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.keepChunk;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.keepChunkMarkActualArchived;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.keepChunkMarkActualDeleted;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
		p = keepChunkMarkActualDeleted();

		assertThat(p.keepActualChunk()).isTrue();
		assertThat(p.markActualChunkAsArchived()).isFalse();
		assertThat(p.markActualChunkAsDeleted()).isTrue();
	}

	@Test
	void testKeepChunkMarkActualArchived() {
		p = keepChunkMarkActualArchived();

		assertThat(p.keepActualChunk()).isTrue();
		assertThat(p.markActualChunkAsArchived()).isTrue();
		assertThat(p.markActualChunkAsDeleted()).isFalse();
	}

	@Test
	void testDontKeepChunkMarkActualDeleted() {
		p = dontKeepChunkMarkActualDeleted();

		assertThat(p.keepActualChunk()).isFalse();
		assertThat(p.markActualChunkAsArchived()).isFalse();
		assertThat(p.markActualChunkAsDeleted()).isTrue();
	}

	@Test
	void testDontKeepChunkMarkActualArchived() {
		p = dontKeepChunkMarkActualArchived();

		assertThat(p.keepActualChunk()).isFalse();
		assertThat(p.markActualChunkAsArchived()).isTrue();
		assertThat(p.markActualChunkAsDeleted()).isFalse();
	}

	private static Stream<Arguments> provideBooleans() {
		return IntStream.range(0, 8)
				.mapToObj(i -> Arguments.of(
						(i & 1) == 1,
						(i & 2) == 2,
						(i & 4) == 4));
	}

	@ParameterizedTest
	@MethodSource("provideBooleans")
	void testChangeActualChunk(final boolean keep, final boolean pArchived, final boolean pDeleted) {
		assertThat(new DatablockMigrateChunkPolicy(keep, pArchived, pDeleted).changeActualChunk())
				.isEqualTo(pArchived || pDeleted);
	}
}
