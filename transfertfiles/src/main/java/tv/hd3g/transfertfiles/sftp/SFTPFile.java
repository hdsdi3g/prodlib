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

import static java.time.temporal.ChronoUnit.MILLIS;
import static tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection.DISTANTTOLOCAL;
import static tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection.LOCALTODISTANT;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.common.StreamCopier.Listener;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode.Type;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.LocalDestFile;
import net.schmizz.sshj.xfer.LocalFileFilter;
import net.schmizz.sshj.xfer.LocalSourceFile;
import net.schmizz.sshj.xfer.TransferListener;
import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.AbstractFileSystem;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.CannotDeleteException;
import tv.hd3g.transfertfiles.CommonAbstractFile;
import tv.hd3g.transfertfiles.SizedStoppableCopyCallback;
import tv.hd3g.transfertfiles.TransfertObserver;
import tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection;

@Slf4j
public class SFTPFile extends CommonAbstractFile<SFTPFileSystem> { // NOSONAR S2160
	private static final String CAN_T_STAT = "Can't stat \"";

	private final SFTPClient sftpClient;
	private final String sftpAbsolutePath;

	SFTPFile(final SFTPFileSystem fileSystem, final SFTPClient sftpClient,
			 final String relativePath,
			 final String absolutePath) {
		super(fileSystem, relativePath);
		if (fileSystem.isAbsoluteBasePath()) {
			sftpAbsolutePath = absolutePath;
		} else if (absolutePath.isEmpty() == false) {
			sftpAbsolutePath = absolutePath.substring(1);
		} else {
			sftpAbsolutePath = "";
		}
		this.sftpClient = sftpClient;
	}

	private boolean isNoSuchFileInError(final IOException e) {
		return e.getMessage().toUpperCase().startsWith("No such file".toUpperCase());
	}

	@Override
	public AbstractFileSystem<?> getFileSystem() {
		return fileSystem;
	}

	@Override
	public long length() {
		try {
			return sftpClient.size(sftpAbsolutePath);
		} catch (final IOException e) {
			if (isNoSuchFileInError(e)) {
				return 0L;
			}
			throw new UncheckedIOException("Can't extract size \"" + sftpAbsolutePath + "\"", e);
		}
	}

	@Override
	public boolean exists() {
		try {
			return sftpClient.statExistence(sftpAbsolutePath) != null;
		} catch (final IOException e) {
			throw new UncheckedIOException(CAN_T_STAT + sftpAbsolutePath + "\"", e);
		}
	}

	@Override
	public void delete() {
		final var directory = isDirectory();
		try {
			if (directory) {
				sftpClient.rmdir(sftpAbsolutePath);
			} else {
				sftpClient.rm(sftpAbsolutePath);
			}
		} catch (final IOException e) {
			throw new CannotDeleteException(this, directory, e);
		}
	}

	@Override
	public boolean isDirectory() {
		try {
			return sftpClient.stat(sftpAbsolutePath).getType() == Type.DIRECTORY;
		} catch (final IOException e) {
			if (isNoSuchFileInError(e)) {
				return false;
			}
			throw new UncheckedIOException(CAN_T_STAT + sftpAbsolutePath + "\"", e);
		}
	}

	@Override
	public boolean isFile() {
		try {
			return sftpClient.stat(sftpAbsolutePath).getType() == Type.REGULAR;
		} catch (final IOException e) {
			if (isNoSuchFileInError(e)) {
				return false;
			}
			throw new UncheckedIOException(CAN_T_STAT + sftpAbsolutePath + "\"", e);
		}
	}

	@Override
	public boolean isLink() {
		try {
			return sftpClient.stat(sftpAbsolutePath).getType() == Type.SYMLINK;
		} catch (final IOException e) {
			if (isNoSuchFileInError(e)) {
				return false;
			}
			throw new UncheckedIOException(CAN_T_STAT + sftpAbsolutePath + "\"", e);
		}
	}

	@Override
	public boolean isSpecial() {
		try {
			final var type = sftpClient.stat(sftpAbsolutePath).getType();
			return type != Type.REGULAR
				   && type != Type.DIRECTORY
				   && type != Type.SYMLINK;
		} catch (final IOException e) {
			if (isNoSuchFileInError(e)) {
				return false;
			}
			throw new UncheckedIOException(CAN_T_STAT + sftpAbsolutePath + "\"", e);
		}
	}

	@Override
	public long lastModified() {
		try {
			return sftpClient.stat(sftpAbsolutePath).getMtime() * 1000L;
		} catch (final IOException e) {
			if (isNoSuchFileInError(e)) {
				return 0;
			}
			throw new UncheckedIOException(CAN_T_STAT + sftpAbsolutePath + "\"", e);
		}
	}

	private CachedFileAttributes makeCachedFileAttributesFromStat(final AbstractFile related, final FileAttributes f) {
		return new CachedFileAttributes(related,
				f.getSize(), f.getMtime() * 1000L, true,
				f.getType() == Type.DIRECTORY,
				f.getType() == Type.REGULAR,
				f.getType() == Type.SYMLINK,
				f.getType() != Type.REGULAR && f.getType() != Type.DIRECTORY && f.getType() != Type.SYMLINK);
	}

	@Override
	public CachedFileAttributes toCache() {
		try {
			return makeCachedFileAttributesFromStat(this, sftpClient.stat(sftpAbsolutePath));
		} catch (final IOException e) {
			if (isNoSuchFileInError(e)) {
				return CachedFileAttributes.notExists(this);
			}
			throw new UncheckedIOException(CAN_T_STAT + sftpAbsolutePath + "\"", e);
		}
	}

	private final UnaryOperator<String> toRelativePath = ftpClientAbsolutePath -> {
		if (fileSystem.isAbsoluteBasePath()) {
			return AbstractFile.normalizePath(ftpClientAbsolutePath.substring(fileSystem.getBasePath().length()));
		} else {
			return AbstractFile.normalizePath(ftpClientAbsolutePath.substring(fileSystem.getBasePath().length() - 1));
		}
	};

	@Override
	public Stream<AbstractFile> list() {
		try {
			return sftpClient.ls(sftpAbsolutePath).stream()
					.map(RemoteResourceInfo::getPath)
					.map(toRelativePath)
					.map(fileSystem::getFromPath);
		} catch (final IOException e) {
			if (isNoSuchFileInError(e) || e.getMessage().equals("Accessed location is not a directory")) {
				return Stream.empty();
			}
			throw new UncheckedIOException("Can't list \"" + sftpAbsolutePath + "\"", e);
		}
	}

	@Override
	public Stream<CachedFileAttributes> toCachedList() {
		try {
			return sftpClient.ls(sftpAbsolutePath).stream()
					.map(rri -> makeCachedFileAttributesFromStat(
							fileSystem.getFromPath(toRelativePath.apply(rri.getPath())), rri.getAttributes()));
		} catch (final IOException e) {
			if (isNoSuchFileInError(e) || e.getMessage().equals("Accessed location is not a directory")) {
				return Stream.empty();
			}
			throw new UncheckedIOException("Can't list \"" + sftpAbsolutePath + "\"", e);
		}
	}

	@Override
	public void mkdir() {
		try {
			sftpClient.mkdirs(sftpAbsolutePath);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't mkdir(s) \"" + sftpAbsolutePath + "\"", e);
		}
	}

	@Override
	public void mkdirs() {
		mkdir();
	}

	@Override
	public AbstractFile renameTo(final String path) {
		try {
			final var newPath = AbstractFile.normalizePath(path);
			if (fileSystem.isAbsoluteBasePath()) {
				sftpClient.rename(sftpAbsolutePath, newPath);
			} else {
				sftpClient.rename(sftpAbsolutePath, newPath.substring(1));
			}
			return fileSystem.getFromPath(newPath);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't rename \"" + sftpAbsolutePath + "\"", e);
		}
	}

	@Override
	public void copyAbstractToLocal(final File localFile, final TransfertObserver observer) {
		copy(sftpAbsolutePath, localFile.getPath(), localFile, observer, DISTANTTOLOCAL);
	}

	@Override
	public void sendLocalToAbstract(final File localFile, final TransfertObserver observer) {
		copy(localFile.getPath(), sftpAbsolutePath, localFile, observer, LOCALTODISTANT);
	}

	private class StoppedTransfertException extends IOException {
		private final long transferred;

		public StoppedTransfertException(final String source, final long transferred) {
			super("Observer has stopped the transfert of " + source);
			this.transferred = transferred;
		}
	}

	private void copy(final String source,
					  final String dest,
					  final File localFile,
					  final TransfertObserver observer,
					  final TransfertDirection transfertDirection) {
		var sizeToTransfert = 0L;
		try {
			synchronized (sftpClient) {
				final var thisRef = this;
				final var now = System.currentTimeMillis();
				final var ft = sftpClient.getFileTransfer();
				ft.setTransferListener(new TransferListener() {

					@Override
					public Listener file(final String name, final long size) {
						return transferred -> {
							if (observer.onTransfertProgress(
									localFile, thisRef, transfertDirection, now, transferred) == false) {
								throw new StoppedTransfertException(localFile.getPath(), transferred);
							}
						};
					}

					@Override
					public TransferListener directory(final String name) {
						return this;
					}
				});

				observer.beforeTransfert(localFile, this, transfertDirection);

				if (transfertDirection == DISTANTTOLOCAL) {
					if (sftpClient.stat(sftpAbsolutePath).getType() == Type.DIRECTORY) {
						throw new UncheckedIOException(
								new IOException("Source file is a directory, can't copy from it"));
					}
					sizeToTransfert = sftpClient.size(sftpAbsolutePath);
					log.info("Download file from SSH host \"{}@{}:{}\" to \"{}\" ({} bytes)",
							fileSystem.getUsername(), fileSystem.getHost(), source, dest, sizeToTransfert);
					ft.download(source, dest);
				} else if (transfertDirection == LOCALTODISTANT) {
					sizeToTransfert = localFile.length();
					log.info("Upload file \"{}\" ({} bytes) to SSH host \"{}@{}:{}\"",
							localFile, sizeToTransfert, fileSystem.getUsername(), fileSystem.getHost(), dest);
					ft.upload(source, dest);
				}

				observer.afterTransfert(localFile, this, transfertDirection,
						Duration.of(System.currentTimeMillis() - now, MILLIS));
			}
		} catch (final StoppedTransfertException e) {
			log.info("Stop copy SSH file from \"{}\" to \"{}\", ({}/{} bytes)",
					source, dest, e.transferred, sizeToTransfert);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't copy \"" + sftpAbsolutePath + "\"", e);
		}
	}

	@Override
	public long downloadAbstract(final OutputStream outputStream,
								 final int bufferSize,
								 final SizedStoppableCopyCallback copyCallback) {
		final var dest = new LocalDestFileImpl(outputStream, copyCallback);
		synchronized (sftpClient) {
			try {
				sftpClient.getFileTransfer().setTransferListener(dest);
				sftpClient.get(sftpAbsolutePath, dest);
			} catch (final StoppedTransfertException e) {
				log.debug("Manually stop ssh download", e);
			} catch (final IOException e) {
				throw new UncheckedIOException("Can't download from \"" + sftpAbsolutePath + "\"", e);
			} finally {
				sftpClient.getFileTransfer().setTransferListener(null);
				try {
					outputStream.close();
				} catch (final IOException e) {
					log.error("Can't close provided outputStream after use", e);
				}
			}
		}

		return dest.getTotalSize();

	}

	@Override
	public long uploadAbstract(final InputStream inputStream,
							   final int bufferSize,
							   final SizedStoppableCopyCallback copyCallback) {
		final var source = new LocalSourceFileImpl(getName(), inputStream, copyCallback);
		synchronized (sftpClient) {
			try {
				sftpClient.getFileTransfer().setTransferListener(source);
				sftpClient.put(source, sftpAbsolutePath);
			} catch (final StoppedTransfertException e) {
				log.debug("Manually stop ssh upload", e);
			} catch (final IOException e) {
				throw new UncheckedIOException("Can't upload from \"" + sftpAbsolutePath + "\"", e);
			} finally {
				sftpClient.getFileTransfer().setTransferListener(null);
				try {
					inputStream.close();
				} catch (final IOException e) {
					log.error("Can't close provided inputStream after use", e);
				}
			}
		}

		return source.getTotalSize();
	}

	private class TransferListenerImpl implements TransferListener {
		private final SizedStoppableCopyCallback copyCallback;
		private final AtomicLong totalSize;

		TransferListenerImpl(final SizedStoppableCopyCallback copyCallback) {
			this.copyCallback = copyCallback;
			totalSize = new AtomicLong();
		}

		@Override
		public Listener file(final String name, final long size) {
			return transferred -> {
				totalSize.set(transferred);
				if (copyCallback.apply(transferred).equals(false)) {
					throw new StoppedTransfertException(name, transferred);
				}
			};
		}

		@Override
		public TransferListener directory(final String name) {
			return this;
		}

		public long getTotalSize() {
			return totalSize.get();
		}
	}

	private class LocalSourceFileImpl extends TransferListenerImpl implements LocalSourceFile {

		private final String name;
		private final InputStream inputStream;

		LocalSourceFileImpl(final String name,
							final InputStream inputStream,
							final SizedStoppableCopyCallback copyCallback) {
			super(copyCallback);
			this.name = name;
			this.inputStream = inputStream;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return inputStream;
		}

		@Override
		public long getLength() {
			return -1;
		}

		@Override
		public int getPermissions() throws IOException {
			return 777;
		}

		@Override
		public boolean isFile() {
			return true;
		}

		@Override
		public boolean isDirectory() {
			return false;
		}

		@Override
		public Iterable<? extends LocalSourceFile> getChildren(final LocalFileFilter filter) throws IOException {
			return List.of();
		}

		@Override
		public boolean providesAtimeMtime() {
			return false;
		}

		@Override
		public long getLastAccessTime() throws IOException {
			return System.currentTimeMillis() / 1000L;
		}

		@Override
		public long getLastModifiedTime() throws IOException {
			return System.currentTimeMillis() / 1000L;
		}

	}

	private class LocalDestFileImpl extends TransferListenerImpl implements LocalDestFile {

		private static final String NOT_AVALIABLE = "Not avaliable";
		private final OutputStream outputStream;

		LocalDestFileImpl(final OutputStream outputStream,
						  final SizedStoppableCopyCallback copyCallback) {
			super(copyCallback);
			this.outputStream = outputStream;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return outputStream;
		}

		@Override
		public long getLength() {
			throw new UnsupportedOperationException(NOT_AVALIABLE);
		}

		@Override
		public OutputStream getOutputStream(final boolean append) throws IOException {
			if (append) {
				throw new UnsupportedOperationException("Append not avaliable");
			}
			return outputStream;
		}

		@Override
		public LocalDestFile getChild(final String name) {
			throw new UnsupportedOperationException(NOT_AVALIABLE);
		}

		@Override
		public LocalDestFile getTargetFile(final String filename) throws IOException {
			return this;
		}

		@Override
		public LocalDestFile getTargetDirectory(final String dirname) throws IOException {
			throw new UnsupportedOperationException(NOT_AVALIABLE);
		}

		@Override
		public void setPermissions(final int perms) throws IOException {
			/**
			 * Not managed
			 */
		}

		@Override
		public void setLastAccessedTime(final long t) throws IOException {
			/**
			 * Not managed
			 */
		}

		@Override
		public void setLastModifiedTime(final long t) throws IOException {
			/**
			 * Not managed
			 */
		}

	}

}
