# All ProdLib projects upgrades

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
