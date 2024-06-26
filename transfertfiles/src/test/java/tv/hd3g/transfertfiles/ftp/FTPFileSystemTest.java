/*
 * This file is part of transfertfiles.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.transfertfiles.ftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.listener.ListenerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tv.hd3g.transfertfiles.AbstractFile;

class FTPFileSystemTest {

	static File root;

	static int port;
	static InetAddress host;
	static String username;
	static String password;
	static DefaultFtpServer ftpd;

	@BeforeAll
	static void load() throws IOException, FtpException {
		root = new File("target/testfs-ftp");
		FileUtils.forceMkdir(root);
		FileUtils.cleanDirectory(root);

		host = InetAddress.getLocalHost();
		username = "testusr";
		password = String.valueOf(System.nanoTime());

		final var serverFactory = new FtpServerFactory();
		serverFactory.setUserManager(new FTPLetUserManager(username, password, new File("")));
		final var factory = new ListenerFactory();
		factory.setPort(0);
		factory.setServerAddress(host.getHostAddress());
		serverFactory.addListener("default", factory.createListener());
		ftpd = (DefaultFtpServer) serverFactory.createServer();
		ftpd.start();
		port = ftpd.getListener("default").getPort();
	}

	@AfterAll
	static void ends() {
		ftpd.stop();
	}

	FTPFileSystem fs;
	char[] goodPassword;
	char[] noPassword;
	char[] badPassword;

	@BeforeEach
	void init() throws IOException {
		goodPassword = password.toCharArray();
		noPassword = "".toCharArray();
		badPassword = "nope".toCharArray();
		fs = new FTPFileSystem(host, port, username, goodPassword, true, "");
	}

	@AfterEach
	void end() {
		if (fs != null) {
			fs.close();
		}
	}

	@Test
	void testFTPFileSystem_noUserName() {
		assertThrows(IllegalArgumentException.class, () -> {// NOSONAR S5778
			new FTPFileSystem(host, port, "", goodPassword, true, "").close();
		});
		fs = new FTPFileSystem(host, port, username, noPassword, true, "");
		fs = new FTPFileSystem(host, port, username, null, true, "");
	}

	@Test
	void testIsPassiveMode() {
		assertTrue(fs.isPassiveMode());
	}

	@Test
	void testGetClient() {
		assertNotNull(fs.getClient());
	}

	@Test
	void testGetHost() throws UnknownHostException {
		assertEquals(InetAddress.getLocalHost(), fs.getHost());
	}

	@Test
	void testGetUsername() {
		assertEquals(username, fs.getUsername());
	}

	@Test
	void testIsReusable() {
		assertTrue(fs.isReusable());
	}

	@Test
	void testConnectIsAvaliable() {
		assertFalse(fs.isAvaliable());
		fs.connect();
		assertTrue(fs.isAvaliable());
		fs.connect();
		assertTrue(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testConnectIsAvaliable_badPassword() {
		fs = new FTPFileSystem(host, port, username, badPassword, false, "");
		assertFalse(fs.isAvaliable());
		assertThrows(UncheckedIOException.class, () -> fs.connect());
		assertFalse(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testConnectIsAvaliable_noPassword() {
		fs = new FTPFileSystem(host, port, username, noPassword, false, "");
		assertFalse(fs.isAvaliable());
		assertThrows(UncheckedIOException.class, () -> fs.connect());
		assertFalse(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testCloseIsAvaliable() {
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testGetIOBufferSize() {
		assertTrue(fs.getIOBufferSize() > 0);
		fs.connect();
		assertTrue(fs.getIOBufferSize() > 0);
		fs.close();
		assertTrue(fs.getIOBufferSize() > 0);
	}

	@Test
	void testGetFromPath() {
		assertThrows(UncheckedIOException.class, () -> fs.getFromPath("target"));
		fs.connect();
		final var f = fs.getFromPath("target");
		assertTrue(f.list()
				.map(AbstractFile::getName)
				.anyMatch(sf -> sf.equalsIgnoreCase("testfs-ftp")));
		fs.close();
	}

	@Test
	void testGetFtpListing() {
		assertNull(fs.getFtpListing());
	}

	@Test
	void testSetFtpListing() {
		fs.setFtpListing(FTPListing.MLSD);
		assertEquals(FTPListing.MLSD, fs.getFtpListing());
	}

	@Test
	void testEqualsHashCode() {
		final var lfs = new FTPFileSystem(host, port, username, noPassword, false, "");
		assertEquals(lfs, fs);
		assertEquals(lfs.hashCode(), fs.hashCode());
	}

	@Test
	void testToString() {
		assertEquals("ftp://" + username + "@" + host.getHostName() + ":" + port + "/", fs.toString());
		fs = new FTPFileSystem(host, port, username, noPassword, false, "");
		assertEquals("ftp://" + username + "@" + host.getHostName() + ":" + port + "/", fs.toString());
	}

	@Test
	void testTimeout() {
		fs.setTimeout(2000, TimeUnit.MILLISECONDS);
		fs.connect();
		final var client = fs.getClient();
		assertEquals(fs.getTimeout(), client.getConnectTimeout());
		assertEquals(fs.getTimeout(), client.getDefaultTimeout());
	}

	@Test
	void testReusableHashCode() {
		final var code0 = fs.reusableHashCode();
		assertNotEquals(0, code0);
		final var lfs = new FTPFileSystem(host, port, username, noPassword, false, "");
		assertNotEquals(code0, lfs.reusableHashCode());

		lfs.close();
	}
}
