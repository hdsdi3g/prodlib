/*
 * This file is part of csvkit.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2022
 *
 */
package tv.hd3g.csvkit;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Reusable
 */
public class CSVKit {
	private static Logger log = LogManager.getLogger();

	protected final CSVParserBuilder csvParserBuilder;
	protected final int skipLines;

	public CSVKit(final CSVParserBuilder csvParserBuilder, final int skipLines) {
		this.csvParserBuilder = csvParserBuilder;
		this.skipLines = skipLines;
	}

	public static CSVKit createFrenchFlavor(final int skipLines) {
		final var parser = new CSVParserBuilder()
		        .withSeparator(';')
		        .withQuoteChar('"');
		return new CSVKit(parser, skipLines);
	}

	protected CSVReaderBuilder createCSVReaderBuilder(final Reader reader) {
		return new CSVReaderBuilder(reader)
		        .withCSVParser(csvParserBuilder.build())
		        .withSkipLines(skipLines);
	}

	public <T> void importCSV(final byte[] rawSource,
	                          final Charset sourceCharset,
	                          final Predicate<String[]> rowValidator,
	                          final Function<String[], T> transform,
	                          final LinkedBlockingQueue<T> result) {
		try (final Reader reader = new StringReader(new String(rawSource, sourceCharset))) {
			try (var csvReader = createCSVReaderBuilder(reader).build()) {
				String[] nextLine;
				while ((nextLine = csvReader.readNext()) != null) {
					if (rowValidator.test(nextLine)) {
						result.add(transform.apply(nextLine));
					}
				}
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't read CSV", e);
		} catch (final CsvValidationException e) {
			throw new UncheckedIOException(new IOException("Can't parse CSV", e));
		}
	}

	public <T> void importAllCSV(final Map<String, Supplier<byte[]>> rawSourcesBySourceNames,
	                             final Charset sourceCharset,
	                             final Predicate<String[]> rowValidator,
	                             final Function<String[], T> transform,
	                             final LinkedBlockingQueue<T> result) {
		rawSourcesBySourceNames.forEach((name, source) -> {
			log.info("Start load CSV from \"{}\"...", name);
			importCSV(source.get(), sourceCharset, rowValidator, transform, result);
		});
	}

	public static <T, R> Map<String, R> groupingById(final Stream<T> values,
	                                                 final Function<? super T, String> idClassifier,
	                                                 final Function<List<T>, R> transform) {
		return values.collect(groupingBy(idClassifier, collectingAndThen(toUnmodifiableList(), transform)));
	}

	/**
	 * @return Only the keys bettween left and right. The other will be sended on lostLeft/lostRight.
	 */
	public static <L, R, C> Map<String, C> aggregateById(final Map<String, L> left,
	                                                     final Map<String, R> right,
	                                                     final BiFunction<L, R, C> transform,
	                                                     final BiConsumer<String, R> lostLeft,
	                                                     final BiConsumer<String, L> lostRight) {
		left.keySet().stream()
		        .filter(not(right::containsKey))
		        .forEach(id -> lostRight.accept(id, left.get(id)));
		right.keySet().stream()
		        .filter(not(left::containsKey))
		        .forEach(id -> lostLeft.accept(id, right.get(id)));
		return left.keySet().stream()
		        .filter(right::containsKey)
		        .collect(toUnmodifiableMap(id -> id,
		                id -> transform.apply(left.get(id), right.get(id))));
	}

}
