/*
 * This file is part of env-version.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package tv.hd3g.commons.version.mod;

import static javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA;
import static javax.xml.xpath.XPathConstants.STRING;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import tv.hd3g.commons.version.EnvironmentVersion;

@Configuration
public class EnvVersionConfigurer {
	private static final Logger log = LogManager.getLogger();

	@Bean
	EnvironmentVersion getEnvVersion(final ApplicationContext context) {
		/**
		 * Inspired by https://blog.jdriven.com/2018/10/get-your-application-appVersion-with-spring-boot/
		 */
		final var appVersion = context.getBeansWithAnnotation(SpringBootApplication.class)
				.entrySet()
				.stream()
				.findFirst()
				.map(Entry::getValue)
				.map(Object::getClass)
				.map(Class::getPackage)
				.map(Package::getImplementationVersion)
				.flatMap(Optional::ofNullable)
				.or(EnvVersionConfigurer::getPomVersion)
				.or(EnvVersionConfigurer::getMavenVersion)
				.orElse(null);
		log.debug("appVersion: \"{}\"", appVersion);

		return EnvironmentVersion.makeEnvironmentVersion(
				appVersion,
				getProdlibVersion(),
				"Spring Boot v" + SpringBootVersion.getVersion());
	}

	private static Optional<String> getPomVersion() {
		final var xmlFile = new File("pom.xml");
		if (xmlFile.exists() == false) {
			return Optional.empty();
		}

		try {
			log.debug("Load {} to extract <version />", xmlFile.getAbsolutePath());

			final var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setAttribute(ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(ACCESS_EXTERNAL_SCHEMA, "");
			factory.setExpandEntityReferences(false);
			final var doc = factory.newDocumentBuilder().parse(xmlFile);

			final var xPath = XPathFactory.newInstance().newXPath();
			final var expression = "/project/version";
			final var result = (String) xPath
					.compile(expression)
					.evaluate(doc, STRING);
			log.debug("Result: \"{}\"", result);

			return Optional.ofNullable(result)
					.stream()
					.map(String::trim)
					.filter(Predicate.not(String::isEmpty))
					.findFirst();
		} catch (final ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
			log.warn("Error during pom.xml version extraction", e);
		}
		return Optional.empty();
	}

	private static Optional<String> getMavenVersion() {
		final var mvnCmdLine = "mvn help:evaluate -Dexpression=project.version -q -DforceStdout";
		try {
			log.debug("Run {}", mvnCmdLine);
			Process r;
			if (System.getProperty("os.name", "").toLowerCase().contains("windows")) {
				r = Runtime.getRuntime().exec("cmd /c " + mvnCmdLine);
			} else {
				r = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", mvnCmdLine });
			}
			while (r.isAlive()) {
				Thread.onSpinWait();
			}
			if (r.exitValue() != 0) {
				log.warn("Can't run maven: {}",
						() -> r.errorReader().lines().collect(Collectors.joining("; ")));
				return Optional.empty();
			}
			final var buffer = new byte[256];
			final var size = r.getInputStream().read(buffer);
			if (size < 1) {
				log.debug("Result: (no stdout)");
				return Optional.empty();
			}

			final var result = new String(buffer, 0, size).trim();
			log.debug("Result: \"{}\"", result);

			return Stream.of(result)
					.filter(Predicate.not(String::isEmpty))
					.findFirst();
		} catch (final IOException e) {
			log.warn("Error during mvn run version extraction", e);
		}
		return Optional.empty();
	}

	private static String getProdlibVersion() {
		final var fileName = "prodlib-version.txt";
		try {
			final var is = EnvVersionConfigurer.class.getClassLoader().getResourceAsStream(fileName);
			if (is == null) {
				log.warn("Can't found \"{}\" from resources", fileName);
				return null;
			}
			final var buffer = new byte[256];
			final var size = is.read(buffer);
			if (size < 1) {
				log.debug("{} resource file is empty!", fileName);
				return null;
			}
			final var result = new String(buffer, 0, size).trim();
			if (result.isEmpty()) {
				log.debug("{} resource file is full of spaces!", fileName);
				return null;
			}

			log.debug("Prodlib version: \"{}\"", result);
			return result;
		} catch (final IOException e) {
			log.warn("Can't open \"{}\" from resources", fileName);
		}
		return null;
	}

}
