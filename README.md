# ProdLib: all libs and parent projects for production

Please use Maven and Java 17 for build and test.

All is tested on Windows 10 and Linux. Should be ok on macOS.

[![Java CI with Maven](https://github.com/hdsdi3g/prodlib/actions/workflows/maven-package.yml/badge.svg)](https://github.com/hdsdi3g/prodlib/actions/workflows/maven-package.yml)

[![CodeQL](https://github.com/hdsdi3g/prodlib/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/hdsdi3g/prodlib/actions/workflows/codeql-analysis.yml)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=hdsdi3g_prodlib&metric=alert_status)](https://sonarcloud.io/dashboard?id=hdsdi3g_prodlib)

## Commons

Maven SpringBoot projects for bootstrap App, Web app and libs.

<details>
<summary>See more</summary>

`tv.hd3g.commons.starter` for base projects SpringBoot reference.

`tv.hd3g.commons.parent` for complete SpringBoot projects lib.

`tv.hd3g.commons.parent-web` for complete SpringBoot projects lib with a web server (based on parent).

`tv.hd3g.commons.app` for complete a SpringBoot application projects (based on parent).

`tv.hd3g.commons.app-web` for complete a SpringBoot application projects (based on parent-web).

`tv.hd3g.commons.interfaces` for some shared cross-projects objects and definitions.

</details>

## JobKit

Execute onetime and scheduled Java jobs with dynamic queues and events.

<details>
<summary>See more</summary>

It's a standalone lib splitted in 3 related Maven projects

- `jobkit-starter` the base project
- `engine`: the internal engine
- `service`: a Spring Boot module for enable `@async` with JobKit.

It's stable.

Engine embed a `Supervisable` tool, used in conjonction with JobKitEngine to track, and collect events during the task execution. 

</details>

## Watchfolder

It can found recently added files and directories in local filesystem, and start events (run Java code) on it.

Related to `JobKit` project and `TransfertFiles`.

## MailKit

Mail engine as notification abstraction for Spring Boot

<details>
<summary>See more</summary>

MailKit can manage Supervisable produced by JobKit, and can transform it in mail: `Notification`.

It's stable.

</details>

## Transfertfiles

A Java library for upload and download files, with protocol abstraction, and transfer progression events.

<details>
<summary>See more</summary>

It currently manage some protocols:

- Local File
- FTP/FTPS/FTPES
- SFTP

It's stable.
</details>

## AuthKit

Authentication and RBAC module for Spring Boot 2 (Java 11).

<details>
<summary>See more</summary>

It's still in alpha.

AuthKit provide a backend API & logon front with:

- User/Group/Role/Right (access to a controller)/Right Context (access context limitation for a controller) as RBAC objects
- Cookie-less and persistence-less session based on JSON web tokens via an HTTP bearer.
- Cookie stateless/session less (same JWT from bearer) for non-REST purpose.
- Optional 2 factors auth (TOTP)
- Optional login by Active Directory via an LDAP auth
- A Role can required a specific IP address before it enable during a request
- Data persistance with the help of an Hibernate database compatible, like MySQL and H2.
- Strongly tested by integration tests (see SonarQube reports)
- An internal and systematic audit system on database, for trace any security/logon actions.
- A ciphered and isolated table for storing personal information (privacy). An user is only referenced by its auto-generated UUID. The username (login name) is only manipulated during logon operations.

AuthKit don't use Spring Security functions: it can be setup in addition for Spring Security.

It's use Liquibase for setup/upgrade MySQL database via [setupdb](https://github.com/hdsdi3g/setupdb-maven-plugin), only if you don't want to use Hibernate to do it.

See more on `authkit` dir.

</details>

## SelfAutoRestDoc

Create an automatic SpringBoot REST documentation to markdown with [Spoon](http://spoon.gforge.inria.fr/).

<details>
<summary>See more</summary>

It's still in alpha.

For setup, add in your pom file:

```
<dependency>
    <groupId>tv.hd3g.commons</groupId>
    <artifactId>selfautorestdoc</artifactId>
    <version>(last current version)</version>
</dependency>
```

And start (or copy and start) from your Spring Boot project `scripts/make-rest-doc.sh`. You will needs maven to run it.

</details>

## Contributing / debugging

Versioning: just use [SemVer](https://semver.org/).

## Author and License

This project is writer by [hdsdi3g](https://github.com/hdsdi3g) and licensed under the LGPL License; see the LICENCE.TXT file for details.
