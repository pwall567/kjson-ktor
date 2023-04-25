# kjson-ktor

[![Build Status](https://travis-ci.com/pwall567/kjson-ktor.svg?branch=main)](https://app.travis-ci.com/github/pwall567/kjson-ktor)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.7.21&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.7.21)
[![Maven Central](https://img.shields.io/maven-central/v/io.kjson/kjson-ktor?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.kjson%22%20AND%20a:%22kjson-ktor%22)

This library provides JSON serialization and deserialization for `ktor` using the
[`kjson`](https://github.com/pwall567/kjson) library.
In particular, it provides a streaming capability, so that items in a JSON array may be deserialised and processed as
they arrive, without waiting for the entire JSON document to be received.

## Quick Start

To use `kjson-ktor`, in the `Application` module, specify:
```kotlin
    install(ContentNegotiation) {
        kjson {
            // specify JSONConfig configuration options here
        }
    }
```
Then, any responses from a REST call such as `call.respond(customerObject)` will use `kjson` serialisation.

The same configuration will cause POST or PUT data to be deserialised using `kjson`.
For example:
```kotlin
        val customerObject = call.receive<Customer>()
```

Client REST calls may also make use of the same serialisation.
In the `HttpClient` configuration, specify:
```kotlin
    install(ContentNegotiation) {
        kjson {
            // specify JSONConfig configuration options here
        }
    }
```
(Observant readers will have noted that these lines are identical to those for server configuration.)

Client calls will use `kjson` to serialise outgoing data and deserialise responses:
```kotlin
        val response = client.get("https://my.remote.server/customers/12345")
        val customer: Customer = response.body()
```
or:
```kotlin
        val response = client.put("https://my.remote.server/customers/12345") {
            setBody(customer)
        }
```

## Streaming

More information to follow...

## Dependency Specification

The latest version of the library is 0.1, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>kjson-ktor</artifactId>
      <version>0.1</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'io.kjson:kjson-ktor:0.1'
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:kjson-ktor:0.1")
```

Peter Wall

2023-04-25
