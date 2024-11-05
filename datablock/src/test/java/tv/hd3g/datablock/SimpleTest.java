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

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SimpleTest {

	@Test
	void test() throws IOException {
		System.out.println(aa());

		try (var channel = FileChannel.open(Path.of("test.bin"), CREATE, READ, WRITE)) {
			final var m = channel.map(READ_WRITE, 0, 1, Arena.ofShared());
			final var bb = m.asByteBuffer();
			// System.out.println(bb.get());
			// bb.clear();
			// bb.put((byte) 0x67);
			// m.unload();
		}
	}

	String aa() {
		try {
			return "upe";
		} finally {
			System.out.println("+++");
		}

	}

}
