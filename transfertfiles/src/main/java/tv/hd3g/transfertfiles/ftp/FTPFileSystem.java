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

import static tv.hd3g.transfertfiles.InvalidURLException.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.transfertfiles.CommonAbstractFileSystem;
import tv.hd3g.transfertfiles.InvalidURLException;

public class FTPFileSystem extends CommonAbstractFileSystem<FTPFile> {
	protected static final Logger log = LogManager.getLogger();

	protected final InetAddress host;
	protected final int port;
	protected final String username;
	private final char[] password;
	private final boolean passiveMode;

	private FTPListing ftpListing;
	/**
	 * Never use directly, prefer getClient (maybe overrided)
	 */
	private final FTPClient client;

	public FTPFileSystem(final InetAddress host,
						 final int port,
						 final String username,
						 final char[] password,
						 final boolean passiveMode,
						 final String basePath) {
		super(basePath, username + "@" + host.getHostName() + ":" + port);
		this.host = requireNonNull(host, "Missing FTP host name");
		this.port = port;
		this.username = requireNonNull(username, "Missing FTP username");
		if (username.length() == 0) {
			throw new InvalidURLException("Invalid (empty) username");
		}
		this.password = Optional.ofNullable(password).orElse(new char[] {});
		this.passiveMode = passiveMode;

		client = new FTPClient();
		log.debug("Init ftp client to {}", this);
	}

	@Override
	public String toString() {
		return "ftp://" + username + "@" + host.getHostName() + ":" + port + getBasePath();
	}

	public boolean isPassiveMode() {
		return passiveMode;
	}

	protected void afterLogin() throws IOException {
		/**
		 * By default, do nothing.
		 */
	}

	@Override
	public void connect() {
		if (isAvaliable()) {
			return;
		}
		final var ftpClient = getClient();

		if (timeoutDuration > 0) {
			client.setConnectTimeout((int) timeoutDuration);
			client.setControlKeepAliveReplyTimeout((int) timeoutDuration);
			client.setControlKeepAliveTimeout(timeoutDuration);
			client.setDefaultTimeout((int) timeoutDuration);
			client.setDataTimeout((int) timeoutDuration);
		}
		try {
			log.debug("Start to connect to {}", this);
			ftpClient.connect(host, port);

			log.trace("Login to {}", this);
			final var loginOk = ftpClient.login(username, new String(password));
			if (loginOk == false) {
				throw new IOException("Can't login to FTP server");
			}
			afterLogin();
			if (passiveMode) {
				log.debug("Switch to passive for {}", this);
				ftpClient.enterLocalPassiveMode();
				checkIsPositiveCompletion(ftpClient.getReplyCode());
			}
			log.trace("Set binary connection to {}", this);
			final var setToBin = ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			if (setToBin == false) {
				throw new IOException("Can't switch to binary the FTP connection");
			}
			ftpClient.setListHiddenFiles(true);
		} catch (final IOException e) {
			if (ftpClient.isConnected() || ftpClient.isAvailable()) {
				try {
					ftpClient.disconnect();
				} catch (final IOException e1) {
					log.error("Can't disconnect properly after login error", e1);
				}
			}
			throw new UncheckedIOException(e);
		}
	}

	protected static void checkIsPositiveCompletion(final int replyCode) throws IOException {
		if (FTPReply.isPositiveCompletion(replyCode)) {
			return;
		}
		throw new IOException("FTP error: " + replyCode);
	}

	@Override
	public InetAddress getHost() {
		return host;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isReusable() {
		return true;
	}

	@Override
	public boolean isAvaliable() {
		return getClient().isAvailable();
	}

	@Override
	public void close() {
		try {
			final var ftpClient = getClient();
			if (ftpClient.isConnected()) {
				log.info("Manually disconnect client for {}", this);
				ftpClient.disconnect();
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public FTPClient getClient() {
		return client;
	}

	@Override
	public int getIOBufferSize() {
		final var ftpClient = getClient();
		return IntStream.of(0x1000,
				ftpClient.getBufferSize(),
				ftpClient.getSendDataSocketBufferSize(),
				ftpClient.getReceiveDataSocketBufferSize())
				.max().getAsInt();
	}

	@Override
	public FTPFile getFromPath(final String path) {
		if (isAvaliable() == false) {
			throw new UncheckedIOException(new IOException("FTP client not connected"));
		}
		final var aPath = getPathFromRelative(path);
		log.trace("Create new FTPFile to \"{}\" relative to \"{}\"", this, aPath);
		return new FTPFile(this, path, aPath);
	}

	public void setFtpListing(final FTPListing ftpListing) {
		this.ftpListing = ftpListing;
	}

	public FTPListing getFtpListing() {
		return ftpListing;
	}

	@Override
	public int reusableHashCode() {
		return getClient().hashCode();
	}
}
