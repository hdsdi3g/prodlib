# All ProdLib projects upgrades

## 22.0.0

Watchfolder queueManualScan should not use JobKit #177 (relative from #175).

Update Spring Boot version from 3.3.1 to 3.3.4

## 21.2.0

Watchfolders start scans just after startScans call #175

JobKit code clean.

Add a manual lock to wait when a Spool is empty.

## 21.1.0

Create testtools project #173

Set Spring Boot starter parent version from 3.3.0 to 3.3.1

Fix flacky SpoolExecutorTest (aggressive concurent calls)

## 21.0.0

Switch to Java 21 (LTS) and update to Spring Boot 3.3.0 #168

## 20.0.0

Big deps update: correct Snyk vulnerabilities, update Spring-boot-starter-parent, update all internal deps in Starter #170

Move Authkit, Selfautorestdoc and CSVKit to deprecated-attic #170

Correct code warns after updates (transfertfiles/test) #170

Remove non-used starter pom deps, update Code policy validation #170

## 19.0.0

Project maintenance: Update Spring Boot parent from 3.1.1 to 3.2.1, and correct deps warns from dependabot and snyk.

## 18.0.2

### Code and project maintenance

Fix spring-hateoas security dependency #150

Correct bad dependency shipped with httpcomponents: remove commons-logging #147

## 18.0.1

No code change: remove deprecated Setupdb maven plugin in Authkit for Liquibase updates #144

## 18.0.0

Create JobKitWatchdog in JobKit to warn if the queue (Spooler) as some toubles, like a Job who block the queue, or a very big queue size. Add Supervisable for it and add Spring Boot conf. Do some clean code on JobJit/test and Mailkit #140

## 17.1.0

Bug fix and evolution: add a timeout for DataExchange (TransfertFiles), set default timeout for AbstractFSURL (30 sec) #141

Set spring-boot-starter-parent version from 3.1.0 to 3.1.1

## 17.0.1

Bug fix: correct old Watchfolders notification messages #138

## 17.0.0

Remove all log4j calls and replace it by slf4j and logback, adapt code for this #135

Update Code Policy Validation to 3.0.0 (remove log4j calls).

Add lombok, slf4j and logback-classic for all project under Starter.

Explicit Hateoas and Thymeleaf uses only for needed projects.

Separate loggers for FTPFileSystem, FTPS and FTPES.

## 16.0.0

Implement parallel running Watchfolder logic #97

## 15.2.1

Bug fix: set SpoolExecutor order execution by add order (and keep priority) #132

## 15.2.0

Add new JobKit logic: create a gracefully stop for Jobkit with a shutdown hook, and an adaptative logic to wait or not the spools to ends #118. So, if you're using JobKit now, you need to add in JobKitEngine spool names on `spoolsNamesToKeepRunningToTheEnd`, so that everything run until all these pending jobs are executed, for *these* spools (the others are cleaned up). In all cases, it waits for the end of execution of the current jobs in progress.

## 15.1.0

Bug fix: stop sending an email after a Watchfolder scan error if the retry process is enabled: create a new FolderActivity "onStopRetryOnError" #125

Little code clean with FolderActivity

Bug fix: correct a bug with FlatBackgroundService who doesn't run disableTask on disable service

## 15.0.3

Correct NPE with NotificationRouterMail (MailKit) when endUserscontacts is null #126

## 15.0.2

Maintenance version: switch Spring Boot version from 3.0.6 to 3.1.0 #127

## 15.0.1

Maintenance version: update Commons Net from 3.8.0 to 3.9.0 and remove deprecated code, explicit versions deps in starter pom, ignore xercesImpl from AuthKit/ESAPI dep #122

## 15.0.0

Switch Spring Boot parent version to v3.0.6, setup Native Image, update codepolicyvalidation, update native-image jsons, fix Hibernate and J2E old calls #59.

Small bug fix: correct equals impl on UserPrivacyDto after Sonatype-lift review.

## 14.2.1

Bug fix: the FTP uploads don't manage subdirs path #119

## 14.2.0

Better error messages for TransfertFiles on I/O actions (bis) #116

## 14.1.0

Better error messages for TransfertFiles on I/O actions #113

Mailkit notifications bug correction (#114):

 - Don't display empty steps zone on Mailkit notifications
 - Correct raw html presence in mail
 - Correct display semicolon
 - Correct exception displays

## 14.0.0

Create project watchfolder-jpa #107

Protect agains missing labels in watchfolders conf (just warn), protect against missing pickUp file type in watchfolder (set Files only by default - in the case of mocks)

Remove unnecessary Repository annotations on AuthKit (code clean)

## 13.4.3 (inc. 13.4.2)

Maintenance:

 - Update ESAPI deps version (https://snyk.io/vuln/SNYK-JAVA-COMMONSFILEUPLOAD-3326457)
 - Update spring-boot-starter-parent from 2.7.9 to 2.7.11

## 13.4.1

Bug correction: correct behavior with missing directory in a Watchfolder target configuration, and refactor error behaviors with transfertfile protocols load missing setup #93

Refactor JSConfig and split responsibilities (tests will be more stable)

## 13.4.0

Create project jsconfig: let load dynamically (hot reloaded) JS files and run internal JS functions as Java functions #104

## 13.3.3

Correct implementation bugs with Watchfolder/ObservedFolder: let overload postConfiguration and correct Lombok setups (EqualsAndHashCode) in ObservedFolder (only label and disabled with be used to compute non duplicated entries).

## 13.3.2

Bug correction: add PBSZ, PROT and OPTS UTF8 commands for FTPS/ES Clients (transfertfiles) #101

## 13.3.1

Code maintenance update (#99). No changes.

## 13.3.0

Create project env-version, and add EnvironmentVersion + process pid in (admin/dev) mail footer to MailKit Notifications #91. No setup required.

## 13.2.0

Implements #95 add allowed/blocked Files/Dirs based on simple willcards.

## 13.1.0

Add SendToEndUserThisContext utility in MailKit Notification #88. It must be enabled explicitly (by default, it set to false, and it don't break actual code/behaviors)

## 13.0.0

Update Spring Boot parent version from 2.6.11 to 2.7.9, correct h2 test setup for AuthKit, change AuthKit MySQL dep in pom, remove unnecessary annotations in Authkit, remove explicit commons compress version in pom transfertfiles #89

## 12.4.0

Add a "disable" entry in Watchfolder configuration (ObservedFolder) #84

## 12.3.1

Maintenance version: explicit add of common-compress dep v1.22 #85

## 12.3.0

Bug fix: correct a problem with serialization and Supervisable.

Also bug fix: Protect against hashCode requests to CachedFileAttribute in a disconnected state, and re-implements hashCode computing for FileSystems (transfertfiles), to avoid host ip change relative bugs with watchfolders #81

## 12.2.0

Add explicit hamcrest dependency in starter pom #74

Add commons-collections4 dependency and property version #75

Create a mkdirs function on AbstractFile/Transfertfiles project #76

Add explicit configuration metadatas for MailKit

## 12.1.0

Implements WithSuperviable annotation on Spring AOP #51

Remove LifeCycle from SupervisableManager

Explicit maven-compiler-plugin and maven-surefire-plugin versions, remove maven-deploy-plugin

## 12.0.1

### Bug fixes

Correct messages for Watchfolder/MailKit

Correct JobKit log messages during shutdown

Fix a bad log message during BackgroundService closing

Remove callbacks for empty supervisables

## 12.0.0

Close Supervisable/Notifications mails are not sended during the App shutdown (#64), now JobKit Service will be `shutdown()` on Spring Boot shutdown, with blocking jobs: all running/waiting job will be waited before the app shutdown, even the Supervisable/Notification jobs.

All services (JobKit) must declare an "on close" (`disableTask`) ready-to-run lambda, and runned during the stop of the service. WatchFolder is all read updated.

Always on JobKit, remove `waitToClose` and merge it with shutdown.

### Bug fixes

NotificationRouterMail end mail log message should display the reason to send a mail (group / enduser), and the used template #68

Correct a flaky test and Mailkit code clean

Correct Translate (MailKit) with uppercase keys #67

Correct buggy templates on debug/dev Notification mails #65

Add missing translate keys for Watchfolder on MailKit #66

## 11.2.0

### Bug fixes, no break changes

Update messages for Notification (FR+EN) #58

Correct MailKit Notification templates #61

Correct race condition with Supervisable/Notification and external code, add logs for Notification #63

## 11.1.0

### Maintenance version, no break changes

Display full file path on logs during missing message file writing failback operation on MailKit/Translate

Better error for Supervisable error messages with "{}"

Add isFolderActivityEvent on Watchfolder for simplier checks with AppNotificationService.isStateChangeEvent implementations.

Add SpringBoot ObjectMapper Supervisable via JobKit service (it embbed a jackson-datatype-jsr310): correct #54

## 11.0.0

__Strong evolution on JobKit and MailKit: add Supervisable and Notification tools.__

Update Datafaker from 1.3.0 to 1.6.0, update Code policy validation from 1.0.4 to 1.1.0, add j2html 1.6.0 to MailKit, add Lombok, add Mockito-inline.

Add ignoreRule DefaultCharset to `lift.toml`

### JobKit

Implements Supervisable on JobKit.

Add Exceptions for JobKit Runnables : replace all `Runnable` by `RunnableWithException`.

Remove old JobKit REST API (status), and `BackgroundServiceId`.

Move `FlatJobKitEngine` in root package.

Remove `thymeleaf` from JobKit service.

WatchedFiles is now a Record: don't use getX() with it, just X().

### MailKit

Implements mail `Notification` logic (linked with `JobKit Supervisable` events).

Update Configuration tree, with `Notification`.

Remove `AppLifeCycleMailService` API, deprecated for `Supervisable`/`Notification` API. App now must implements `AppNotificationService`. Remove templates for it.

Remove `SendMailToFileService`, replaced by `FlatJavaMailSender`.

### Transfertsfiles

Add Jackson annotations and serializers for `AbstractFile`, `AbstractFileSystem` and `CachedFileAttributes`.

### AuthKit

Correct Security warning https://github.com/hdsdi3g/prodlib/security/code-scanning/5

## 10.0.1

Update Apache Commons Text to 1.10.0 (security) #49

## 10.0.0

Switch from Java 11 (LTS) to Java 17 (LTS) #39

## 9.3.0

No break changes in actual configurations.

Manage updated files for Watchfolder #32

## 9.1.2

No break changes in actual configurations.

Run all waited JobKit BackgroundServices after Spring Boot application is ready to run #28

Update Spring Boot and Spoon.

## 9.1.0

Manage extended extensions for Watchfolder #15. No break changes in actual configurations.

Add CSVKit (non-spring project) `tv.hd3g.csvkit`, in Alpha/non-stable.

## 9.0.0

First merged version. No API signatures or code behavior changes from the original projects.
