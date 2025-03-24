# Change Log

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [1.4] - 2025-03-24
### Added
- `build.yml`, `deploy.yml`: converted project to GitHub Actions
### Changed
- `pom.xml`: updated dependency versions
- `pom.xml`: updated Kotlin version to 2.0.21, Ktor version to 3.0.3
- tests : switched to `should-test` library
### Removed
- `.travis.yml`

## [1.3] - 2023-10-15
### Changed
- `pom.xml`: updated dependency version
- `pom.xml`: updated Kotlin version to 1.8.22, Ktor version to 2.3.5

## [1.2] - 2023-07-19
### Added
- `JSONKtorStream`: functions extracted from `JSONKtorFunctions`
- `JSONLinesOutput`, `JSONLinesFlowOutput`, `JSONLinesChannelOutput`: JSON Lines support
### Changed
- `JSONKtor`, `JSONKtorFunctions`: added support for JSON Lines
- `pom.xml`: updated dependency version

## [1.1] - 2023-07-10
### Changed
- `pom.xml`: updated version of kjson

## [1.0] - 2023-06-05
### Changed
- `pom.xml`: promoted to version 1.0

## [0.3] - 2023-05-21
### Added
- `CoOutputChannel`
- `JSONKtorClientException`
### Changed
- `JSONKtorFunctions`: added `channelOutput()`, `createStreamedJSONContent()` and `ApplicationCall.respondStream()`
- `README.md`: more documentation

## [0.2] - 2023-05-14
### Added
- `KtorByteChannelOutput`
- `KtorOutgoingContent`
### Changed
- `JSONKtorFunctions`: added `channelOutput()` and `ApplicationCall.respondStream()`

## [0.1] - 2023-04-25
### Added
- all files: initial versions
