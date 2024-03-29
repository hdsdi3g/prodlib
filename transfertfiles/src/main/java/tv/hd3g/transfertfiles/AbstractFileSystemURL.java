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
package tv.hd3g.transfertfiles;

import static java.util.concurrent.TimeUnit.SECONDS;
import static tv.hd3g.transfertfiles.AbstractFile.normalizePath;
import static tv.hd3g.transfertfiles.InvalidURLException.requireNonNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.transfertfiles.ftp.FTPESFileSystem;
import tv.hd3g.transfertfiles.ftp.FTPFileSystem;
import tv.hd3g.transfertfiles.ftp.FTPSFileSystem;
import tv.hd3g.transfertfiles.local.LocalFileSystem;
import tv.hd3g.transfertfiles.sftp.SFTPFileSystem;

@Slf4j
public class AbstractFileSystemURL implements Closeable {

	private final String protectedRessourceURL;
	private final AbstractFileSystem<?> fileSystem;
	private final String basePath;

	protected AbstractFileSystemURL(final String protectedRessourceURL,
									final AbstractFileSystem<?> fileSystem,
									final String basePath) {
		this.protectedRessourceURL = protectedRessourceURL;
		this.fileSystem = fileSystem;
		this.basePath = basePath;
	}

	/**
	 * @param ressourceURL like "protocol://user:password@host/basePath?password=secret&active&key=/path/to/privateOpenSSHkey"
	 *        query must not be encoded
	 *        use ":password" or "password=" as you want.
	 *        Never set an "?" in the user:password place. Quotation mark here are ignored (simply used as quotation marks).
	 *        Never add directly an "&" in the password in query, but you can use " (quotation mark) like password="s&e?c=r+t"
	 *        key password can be set by... set password
	 *        -
	 *        FTP(S|ES) is passive by default.
	 *        -
	 *        Add "ignoreInvalidCertificates" to bypass TLS verification with FTPS/FTPES clients.
	 *        --
	 *        Add "timeout=5" for set 5 seconds of connect/socket timeout
	 */
	public AbstractFileSystemURL(final String ressourceURL) {
		final var url = new URLAccess(ressourceURL);

		final var protocol = requireNonNull(url.getProtocol(), "Missing URL Protocol", url);
		basePath = normalizePath(Optional.ofNullable(url.getPath()).orElse("/"));
		final var host = resolveHostname(url);
		final var query = requireNonNull(url.getOptionZone(), "Missing option zone", url);
		final var username = url.getUsername();
		protectedRessourceURL = url.getProtectedRessourceURL() + " [" + host.getHostAddress() + "]";
		final var password = url.getPassword();
		final var port = url.getPort();

		final var passive = query.containsKey("active") == false;
		final var ignoreInvalidCertificates = query.containsKey("ignoreInvalidCertificates");
		final var keys = query.getOrDefault("key", List.of()).stream()
				.map(File::new)
				.filter(File::exists)
				.toList();

		log.trace("Parsed URL: {}", Map.of(
				"protocol", protocol,
				"basePath", Optional.ofNullable(basePath).orElse("null"),
				"host", Optional.ofNullable(host).map(Object::toString).orElse("null"),
				"query", query,
				"username", Optional.ofNullable(username).orElse("null"),
				"protectedRessourceURL", Optional.ofNullable(protectedRessourceURL).orElse("null"),
				"port", port,
				"passive", passive,
				"ignoreInvalidCertificates", ignoreInvalidCertificates,
				"keys", Optional.ofNullable(query.get("key")).map(Object::toString).orElse("null")));

		if (protocol.contentEquals("file")) {
			log.debug("Init URL LocalFileSystem: {}", toString());
			fileSystem = new LocalFileSystem(new File(basePath));
		} else if (protocol.contentEquals("sftp")) {
			log.debug("Init URL SFTPFileSystem: {}", toString());
			fileSystem = new SFTPFileSystem(host, port, username, basePath);
			final var sFTPfileSystem = (SFTPFileSystem) fileSystem;

			if (keys.isEmpty()) {
				if (password.length > 0) {
					sFTPfileSystem.setPasswordAuth(password);
				}
			} else {
				keys.forEach(privateKey -> sFTPfileSystem.manuallyAddPrivatekeyAuth(privateKey, password));
			}
		} else if (protocol.contentEquals("ftp")) {
			log.debug("Init URL FTPFileSystem: {}", toString());
			fileSystem = new FTPFileSystem(host, port, username, password, passive, basePath);
		} else if (protocol.contentEquals("ftps")) {
			log.debug("Init URL FTPSFileSystem: {}", toString());
			fileSystem = new FTPSFileSystem(host, port, username, password, passive,
					ignoreInvalidCertificates, basePath);
		} else if (protocol.contentEquals("ftpes")) {
			log.debug("Init URL FTPESFileSystem: {}", toString());
			fileSystem = new FTPESFileSystem(host, port, username, password, passive,
					ignoreInvalidCertificates, basePath);
		} else {
			throw new InvalidURLException("Can't manage protocol \"" + protocol + "\"", url);
		}

		final var timeout = query.getOrDefault("timeout", List.of()).stream()
				.map(Integer::valueOf)
				.findFirst()
				.orElse(0);
		if (timeout > 0) {
			fileSystem.setTimeout(timeout, SECONDS);
		} else {
			fileSystem.setTimeout(30, SECONDS);
		}
	}

	private InetAddress resolveHostname(final URLAccess url) {
		try {
			return InetAddress.getByName(Optional.ofNullable(url.getHost()).orElse("localhost"));
		} catch (final UnknownHostException e) {
			log.debug("Can't resolve hostname in URL {}", url, e);
			throw new InvalidURLException("Can't resolve hostname", url);
		}
	}

	/**
	 * Passwords will be replaced by "*"
	 */
	@Override
	public String toString() {
		return protectedRessourceURL;
	}

	public AbstractFileSystem<?> getFileSystem() {// NOSONAR S1452
		return fileSystem;
	}

	/**
	 * @param relative path only
	 * @return after fileSystem.connect
	 */
	public AbstractFile getFromPath(final String path) {
		final var p = normalizePath(path);
		log.trace("Create new AbstractFile to \"{}\" with \"{}\"", p, fileSystem);
		fileSystem.connect();
		return fileSystem.getFromPath(p);
	}

	/**
	 * @return after fileSystem.connect
	 */
	public AbstractFile getRootPath() {
		return getFromPath("");
	}

	@Override
	public void close() throws IOException {
		fileSystem.close();
	}

	/**
	 * If disconnected, can we re-connect after ?
	 */
	public boolean isReusable() {
		return fileSystem.isReusable();
	}

	public String getBasePath() {
		return basePath;
	}
}
