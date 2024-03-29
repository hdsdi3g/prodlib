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
package tv.hd3g.transfertfiles.sftp;

import static tv.hd3g.transfertfiles.InvalidURLException.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import tv.hd3g.transfertfiles.CommonAbstractFileSystem;
import tv.hd3g.transfertfiles.InvalidURLException;

@Slf4j
public class SFTPFileSystem extends CommonAbstractFileSystem<SFTPFile> {

	private final SSHClient client;
	private final InetAddress host;
	private final int port;
	private final String username;
	private final Set<KeyProvider> authKeys;
	private final boolean absoluteBasePath;

	private char[] password;
	private SFTPClient sftpClient;
	private boolean statefulSFTPClient;
	private volatile boolean wasConnected;

	public SFTPFileSystem(final InetAddress host, final int port, final String username, final String basePath) {
		this(host, port, username, basePath, false);
	}

	public SFTPFileSystem(final InetAddress host,
						  final int port,
						  final String username,
						  final String basePath,
						  final boolean absoluteBasePath) {
		super(basePath, username + "@" + host.getHostName() + ":" + port);
		client = new SSHClient();
		wasConnected = false;
		this.host = requireNonNull(host, "Missing host name");
		this.port = port;
		this.username = requireNonNull(username, "Missing SSH username");
		if (username.length() == 0) {
			throw new InvalidURLException("Invalid (empty) SSH username");
		}
		authKeys = new HashSet<>();
		this.absoluteBasePath = absoluteBasePath;
		log.debug("Init ssh client to {}", this);

		final var defaultKhFile = System.getProperty("user.home")
								  + File.separator + ".ssh" + File.separator + "known_hosts";
		final var knownHostFile = new File(System.getProperty("ssh.knownhosts", defaultKhFile));
		try {
			FileUtils.forceMkdirParent(knownHostFile);
			client.addHostKeyVerifier(new DefaultKnownHostsVerifier(knownHostFile));
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't load known_hosts file during SSH/SFTP client loading: "
										   + knownHostFile, e);
		}
	}

	@Override
	public boolean isReusable() {
		return false;
	}

	@Override
	public synchronized boolean isAvaliable() {
		return client.isConnected() && client.isAuthenticated();
	}

	@Override
	public int getIOBufferSize() {
		if (sftpClient != null) {
			final var system = sftpClient.getSFTPEngine().getSubsystem();
			return Math.min(system.getLocalMaxPacketSize(), system.getRemoteMaxPacketSize());
		} else {
			return -1;
		}
	}

	@Override
	public String toString() {
		return "sftp://" + username + "@" + host.getHostName() + ":" + port + getBasePath();
	}

	public void setPasswordAuth(final char[] password) {
		this.password = Objects.requireNonNull(password, "password");
		if (password.length == 0) {
			throw new IllegalArgumentException("Invalid (empty) password");
		}
		final var strStars = logPassword(password);
		log.debug("Set password auth for {}: {}", this, strStars);
	}

	private static String logPassword(final char[] password) {
		final var stars = new char[password.length];
		Arrays.fill(stars, '*');
		return String.valueOf(stars);
	}

	private KeyProvider loadPrivateKey(final File privateKey, final char[] keyPassword) {
		try {
			if (keyPassword != null && keyPassword.length > 0) {
				final var strStars = logPassword(keyPassword);
				log.debug("Add private key auth for {}: {}, with password {}", this, privateKey, strStars);
				return client.loadKeys(privateKey.getPath(), clonePassword(keyPassword));
			} else {
				log.debug("Add private key auth for {}: {}", this, privateKey);
				return client.loadKeys(privateKey.getPath());
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't load provided SSH/SCP private key: " + privateKey, e);
		}
	}

	/**
	 * @param privateKey can be a file, or a directory of key files (like "~/.ssh/")
	 */
	public void manuallyAddPrivatekeyAuth(final File privateKey, final char[] keyPassword) {
		Objects.requireNonNull(privateKey);
		if (privateKey.exists() == false) {
			throw new UncheckedIOException("Can't found SSH/SCP private key: " + privateKey,
					new FileNotFoundException());
		} else if (privateKey.isDirectory()) {
			authKeys.addAll(Stream.of("id_rsa", "id_dsa", "id_ed25519", "id_ecdsa")
					.map(f -> new File(privateKey, f))
					.filter(File::exists)
					.map(fPrivateKey -> loadPrivateKey(fPrivateKey, keyPassword))
					.toList());
		} else {
			authKeys.add(loadPrivateKey(privateKey, keyPassword));
		}
	}

	/**
	 * @param privateKey can be a file, or a directory of key files (like "~/.ssh/")
	 */
	public void manuallyAddPrivatekeyAuth(final File privateKey) {
		manuallyAddPrivatekeyAuth(privateKey, null);
	}

	public SSHClient getClient() {
		return client;
	}

	public SFTPClient getSFTPClient() {
		return sftpClient;
	}

	private static final char[] clonePassword(final char[] password) {
		final var disposablePassword = new char[password.length];
		System.arraycopy(password, 0, disposablePassword, 0, password.length);
		return disposablePassword;
	}

	@Override
	public void setTimeout(final long duration, final TimeUnit unit) {
		if (duration > 0) {
			timeoutDuration = unit.toMillis(duration);
		}
		if (timeoutDuration > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Can't set a timeoutDuration > Integer.MAX_VALUE: "
											   + timeoutDuration);
		}
	}

	@Override
	public synchronized void connect() {
		if (isAvaliable()) {
			return;
		} else if (wasConnected == true) {
			throw new UncheckedIOException("Client is not avaliable to use " + this, new IOException());
		}
		log.debug("Start to connect to {}", this);
		wasConnected = true;

		if (timeoutDuration > 0) {
			client.setConnectTimeout((int) timeoutDuration);
			client.setTimeout((int) timeoutDuration);
		}
		try {
			client.connect(host, port);
			if (password != null && password.length > 0) {
				log.trace("Connect to {} with password", this);
				client.authPassword(username, clonePassword(password));
			} else {
				log.trace("Connect to {} with Publickey", this);
				client.authPublickey(username, authKeys);
			}
			log.info("Connected to {}", this);
			createANewSFTPClient();
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't connect to server: " + this, e);
		}
	}

	public boolean isStatefulSFTPClient() {
		return statefulSFTPClient;
	}

	public void setStatefulSFTPClient(final boolean statefulSFTPClient) {
		this.statefulSFTPClient = statefulSFTPClient;
	}

	/**
	 * Needed for simultaneous transferts:
	 * a = getFromPath() + createANewSFTPClient() + b = getFromPath()
	 * &gt; a and b can do actions in same time.
	 */
	public synchronized void createANewSFTPClient() {
		try {
			if (statefulSFTPClient) {
				log.debug("Create a new stateful SFTP client for {}", this);
				sftpClient = client.newStatefulSFTPClient();
			} else {
				log.debug("Create a new SFTP client for {}", this);
				sftpClient = client.newSFTPClient();
			}
			sftpClient.getFileTransfer().setPreserveAttributes(false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't start a new SFTP client on server: " + this, e);
		}
	}

	@Override
	public synchronized void close() {
		try {
			if (client.isConnected()) {
				log.info("Manually disconnect client for {}", this);
				client.disconnect();
			}
			sftpClient = null;
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't disconnect from the SSH/SFTP server: " + this, e);
		}
	}

	@Override
	public synchronized SFTPFile getFromPath(final String path) {
		if (isAvaliable() == false) {
			if (wasConnected == false) {
				throw new UncheckedIOException("Can't use client, inactive SSH/SCP client on server: " + this,
						new IOException());
			} else {
				throw new UncheckedIOException("Can't use client, disconnected SSH/SCP client on server: " + this,
						new IOException());
			}
		}
		final var aPath = getPathFromRelative(path);
		log.trace("Create new SFTPFile to {}/{}", this, aPath);
		return new SFTPFile(this, sftpClient, path, aPath);
	}

	public boolean isAbsoluteBasePath() {
		return absoluteBasePath;
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
	public int reusableHashCode() {
		if (sftpClient == null) {
			throw new IllegalStateException("Please connect before get reusableHashCode");
		}
		return sftpClient.hashCode();
	}
}
