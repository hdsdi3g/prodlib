# All ProdLib projects upgrades needs

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
