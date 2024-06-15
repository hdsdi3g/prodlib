# ProdLib

Some utilities libs and tools, and parent (pom) projects for small java projects.

All these projects are consistent with each other. Some use Spring Boot when necessary.

Please use Maven and Java 21 for build, test and run.

All is tested on Windows 10, Linux and GitHub Actions (Ubuntu). Should be ok on macOS.

Relases are sent to [Maven central](https://mvnrepository.com/artifact/tv.hd3g).

[![Java CI with Maven](https://github.com/hdsdi3g/prodlib/actions/workflows/maven-package.yml/badge.svg)](https://github.com/hdsdi3g/prodlib/actions/workflows/maven-package.yml)

[![CodeQL](https://github.com/hdsdi3g/prodlib/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/hdsdi3g/prodlib/actions/workflows/codeql-analysis.yml)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=hdsdi3g_prodlib&metric=alert_status)](https://sonarcloud.io/dashboard?id=hdsdi3g_prodlib)

## Projects / modules

### Starter

Commons Maven starter project for bootstrap App, Web app and libs. Pom project, Spring Boot optional, stable.

### Parent

Base project for lib. [Pom project](https://github.com/hdsdi3g/prodlib/blob/master/parent/pom.xml), Spring Boot, stable.

### Parent-web

Base project for **web** lib. [Pom project](https://github.com/hdsdi3g/prodlib/blob/master/parent-web/pom.xml), Spring Boot, stable.

### App

Base project for standalone application. [Pom project](https://github.com/hdsdi3g/prodlib/blob/master/app/pom.xml), Spring Boot, stable.

### App-web

Base project for standalone **web** application. [Pom project](https://github.com/hdsdi3g/prodlib/blob/master/app-web/pom.xml), Spring Boot, stable.

### Interfaces

Java Interfaces and DTOs for connect projects without direct dependencies, for some shared cross-projects objects and definitions.

[Java lib](https://github.com/hdsdi3g/prodlib/blob/master/interfaces/pom.xml), Spring Boot, stable.

### Transfertfiles

Manipulate files across protocols: a Java library for upload and download files, with protocol abstraction (local File, FTP/FTPS/FTPES, SFTP), and transfer progression events.

[Java lib](https://github.com/hdsdi3g/prodlib/blob/master/transfertfiles/pom.xml), standalone, stable.

### Jobkit

Base project for all jobkit modules. [Pom project](https://github.com/hdsdi3g/prodlib/blob/master/jobkit/pom.xml), Spring Boot optional, stable.

### Jobkit/engine

Execute onetime and scheduled jobs with dynamic queues and events.

Engine embed a `Supervisable` tool, used in conjonction with JobKitEngine to track, and collect events during the task execution. 

[Java lib](https://github.com/hdsdi3g/prodlib/blob/master/jobkit/engine/pom.xml), standalone, stable.

### Jobkit/springboot-service

Execute as SpringBoot module the JobKit lib. [Java lib](https://github.com/hdsdi3g/prodlib/blob/master/jobkit/springboot-service/pom.xml), Spring Boot, stable.

### Jobkit/watchfolder

Regulary scan folders (local or distant) and throws events on activity.

It can found recently added files and directories in local filesystem, and start events (run Java code) on it.

[Java lib](https://github.com/hdsdi3g/prodlib/blob/master/jobkit/watchfolder/pom.xml), standalone, stable.

### Jobkit/watchfolder-jpa

Watchfolder lib with persistence. [Java lib](https://github.com/hdsdi3g/prodlib/blob/master/jobkit/watchfolder-jpa/pom.xml), Spring Boot, stable.

### Mailkit

Mail engine as notification abstraction.

MailKit can manage Supervisable produced by JobKit, and can transform it in mail: `Notification`.

[Java lib](https://github.com/hdsdi3g/prodlib/blob/master/mailkit/pom.xml), Spring Boot, stable.

### Env-version

Tools for provide project and deps version. [Java lib](https://github.com/hdsdi3g/prodlib/blob/master/env-version/pom.xml), Spring Boot, stable.

### Jsconfig

Let setup JS files as configuration and expose it as a Spring Boot service. [Java lib](https://github.com/hdsdi3g/prodlib/blob/master/jsconfig/pom.xml), Spring Boot, _Alpha_.

[Java lib](https://github.com/hdsdi3g/prodlib/blob/master/selfautorestdoc/pom.xml), Spring Boot, _Alpha_.

## Contributing / debugging

Versioning: just use [SemVer](https://semver.org/).

## Author and License

This project is writer by [hdsdi3g](https://github.com/hdsdi3g) and licensed under the LGPL License; see the LICENCE.TXT file for details.
