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

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

import tv.hd3g.transfertfiles.filters.DataExchangeFilter;
import tv.hd3g.transfertfiles.local.LocalFileSystem;

/**
 * Expected to be stateless (store statuses in corresponding AbstractFileSystem) and thread safe.
 * Don't forget to implements hashCode, equals and toString.
 */
@JsonIgnoreType
public interface AbstractFile {

	AbstractFileSystem<?> getFileSystem();// NOSONAR S1452

	void copyAbstractToLocal(File localFile, TransfertObserver observer);

	void sendLocalToAbstract(File localFile, TransfertObserver observer);

	/**
	 * Relative path only
	 * @return always with '/' as directory separators
	 */
	String getPath();

	String getName();

	/**
	 * @return never null: if this is root dir, return this.
	 */
	AbstractFile getParent();

	long length();

	boolean exists();

	/**
	 * Not recursive.
	 */
	void delete();

	boolean isDirectory();

	boolean isFile();

	boolean isLink();

	boolean isSpecial();

	/**
	 * @return marked "hidden" or is a dotfile
	 */
	boolean isHidden();

	long lastModified();

	Stream<AbstractFile> list();

	void mkdir();

	/**
	 * Don't throws error if dirs exists. But throw an error if a dir was already a file.
	 */
	void mkdirs();

	/**
	 * @return moved file
	 */
	AbstractFile renameTo(String path);

	/**
	 * @return moved file
	 */
	default AbstractFile renameTo(final String path0, final String... pathN) {
		if (pathN != null && pathN.length > 0) {
			return renameTo(path0 + "/" + Stream.of(pathN)
					.filter(not(Objects::isNull))
					.collect(joining("/")));
		} else {
			return renameTo(path0);
		}
	}

	/**
	 * @return a read-only cached data version of this AbstractFile
	 */
	default CachedFileAttributes toCache() {
		return new CachedFileAttributes(this);
	}

	/**
	 * @return a read-only cached list data version of AbstractFile.list
	 */
	default Stream<CachedFileAttributes> toCachedList() {
		return list().map(CachedFileAttributes::new);
	}

	static String normalizePath(final String path) {
		Objects.requireNonNull(path, "path can't be null");
		var p = path;
		if (p.equals("") || p.equals("/")) {
			return "/";
		}
		while (p.contains("//")) {
			p = p.replace("//", "/");
		}
		if (p.contains("../")
			|| p.contains("./")
			|| p.contains("/~/")
			|| p.startsWith("~/")
			|| p.equals("..")
			|| p.equals(".")
			|| p.equals("~")) {
			throw new IllegalArgumentException("Invalid path: \"" + path + "\"");
		}

		if (p.startsWith("/") == false) {
			p = "/" + p;
		}
		if (p.endsWith("/")) {
			return p.substring(0, p.length() - 1);
		} else {
			return p;
		}
	}

	/**
	 * Only use with a regular file. Type will not be checked before copy action.
	 * Never forget to call outputStream.close after download.
	 * @param bufferSize can be used on internal stream transfert, but it's not mandated.
	 * @return data size readed from this
	 */
	long downloadAbstract(final OutputStream outputStream,
						  int bufferSize,
						  final SizedStoppableCopyCallback copyCallback);

	/**
	 * Only use with a regular file. Type will not be checked before copy action.
	 * @param bufferSize can be used on internal stream transfert, but it's not mandated.
	 * @return data size writer to this
	 */
	long uploadAbstract(final InputStream inputStream,
						int bufferSize,
						final SizedStoppableCopyCallback copyCallback);

	static void checkIsSameFileSystem(final AbstractFile from,
									  final AbstractFile destination) {
		final var fromFs = from.getFileSystem();
		final var toFs = destination.getFileSystem();
		if (fromFs.equals(toFs) == false
			|| fromFs instanceof LocalFileSystem
			|| toFs instanceof LocalFileSystem) {
			return;
		}
		if (fromFs.reusableHashCode() == toFs.reusableHashCode()) {
			throw new UncheckedIOException(
					new IOException(
							"Can't use same FileSystem instances between to AbstractFiles. Please start a new FS instance for one of the two AbstractFile"));
		}
	}

	default Duration getDataExchangeTimeout() {
		return Duration.ofSeconds(30);
	}

	default DataExchangeInOutStream copyAbstractToAbstract(final AbstractFile destination,
														   final DataExchangeObserver dataExchangeObserver,
														   final DataExchangeFilter... filters) {
		final var bufferSize = Math.max(8192,
				Math.max(destination.getFileSystem().getIOBufferSize(),
						getFileSystem().getIOBufferSize()));
		final var exchange = new DataExchangeInOutStream(getDataExchangeTimeout());
		Stream.of(filters).forEach(exchange::addFilter);
		copyAbstractToAbstract(destination, bufferSize, dataExchangeObserver, exchange);
		return exchange;
	}

	default void copyAbstractToAbstract(final AbstractFile destination,
										final int bufferSize,
										final DataExchangeObserver dataExchangeObserver,
										final DataExchangeInOutStream exchange) {
		checkIsSameFileSystem(this, destination);

		final var sourceStream = exchange.getSourceOriginStream();
		final var destStream = exchange.getDestTargetStream();
		final var startDate = System.currentTimeMillis();

		final SizedStoppableCopyCallback readCallback = copied -> dataExchangeObserver
				.onTransfertProgressFromSource(this, startDate, copied);
		final SizedStoppableCopyCallback writeCallback = copied -> dataExchangeObserver
				.onTransfertProgressToDestination(destination, startDate, copied);

		dataExchangeObserver.beforeTransfert(this, destination);
		final var downloader = CompletableFuture.supplyAsync(() -> downloadAbstract(destStream, bufferSize,
				readCallback));

		final var writed = destination.uploadAbstract(sourceStream, bufferSize, writeCallback);

		long readed;
		try {
			readed = downloader.get();
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
		dataExchangeObserver.afterTransfert(this, destination, readed, writed,
				Duration.ofMillis(System.currentTimeMillis() - startDate));
	}
}
