# ProdLib: All libs and parent projects for production

Please use Maven and Java 11 for build and test.

[![Java CI with Maven](https://github.com/hdsdi3g/prodlib/actions/workflows/maven-package.yml/badge.svg)](https://github.com/hdsdi3g/prodlib/actions/workflows/maven-package.yml)

[![CodeQL](https://github.com/hdsdi3g/prodlib/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/hdsdi3g/prodlib/actions/workflows/codeql-analysis.yml)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=hdsdi3g_prodlib&metric=alert_status)](https://sonarcloud.io/dashboard?id=hdsdi3g_prodlib)

## Commons

`tv.hd3g.commons.starter` for base projects SpringBoot reference.

`tv.hd3g.commons.parent` for complete SpringBoot projects lib.

`tv.hd3g.commons.parent-web` for complete SpringBoot projects lib with a web server (based on parent).

`tv.hd3g.commons.app` for complete a SpringBoot application projects (based on parent).

`tv.hd3g.commons.app-web` for complete a SpringBoot application projects (based on parent-web).

`tv.hd3g.commons.interfaces` for some shared cross-projects objects and definitions.

## MailKit

Mail engine as notification abstraction for Spring Boot

## Transfertfiles

A Java library for upload and download files, with protocol abstraction, and transfer progression events.

## SelfAutoRestDoc

Create an automatic SpringBoot REST documentation to markdown with [Spoon](http://spoon.gforge.inria.fr/).

It's still in alpha.

### Setup

Add in your pom file:

```
<dependency>
    <groupId>tv.hd3g.commons</groupId>
    <artifactId>selfautorestdoc</artifactId>
    <version>(last current version)</version>
</dependency>
```

And start (or copy and start) from your Spring Boot project `scripts/make-rest-doc.sh`. You will needs maven to run it.

## Contributing / debugging

Versioning: just use [SemVer](https://semver.org/).

## Author and License

This project is writer by [hdsdi3g](https://github.com/hdsdi3g) and licensed under the LGPL License; see the LICENCE.TXT file for details.
