# kjson-ktor

[![Build Status](https://github.com/pwall567/kjson-ktor/actions/workflows/build.yml/badge.svg)](https://github.com/pwall567/kjson-ktor/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v2.0.21&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v2.0.21)
[![Maven Central](https://img.shields.io/maven-central/v/io.kjson/kjson-ktor?label=Maven%20Central)](https://central.sonatype.com/artifact/io.kjson/kjson-ktor)

This library provides JSON serialization and deserialization for Ktor using the
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

Client calls will use `kjson` to serialise outgoing data and deserialise incoming client responses:
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

## Streaming Output

Output from a Ktor REST call may be streamed automatically.
If the line:
```kotlin
        streamOutput = true
```
is added to the `JSONConfig` used by the application, output will be performed by an asynchronous process, serialising
the response object to JSON and outputting it on the fly.
This has significant benefits in both storage (no need to hold a long string of JSON in memory) and response time (the
first bytes start appearing on the line much more quickly), but even greater benefits can be realised by the use of
`Channel` or `Flow` collections.

Consider the following example:
```kotlin
    routing {
        get("/customers") {
            val flow = flow {
                // a process to read the customer database and emit each object as it becomes available
            }
            call.respond(flow)
        }
    }
```

In this case, the first bytes of the response may be sent **before the entire response list has been assembled**.
A `Flow` will be serialised as a JSON array, and each object in the `Flow` collection will be serialised and sent in the
response as it is added (by `emit()` to the `Flow`).

This asynchronous serialisation applies even if the `Flow` is a nested data property of a container object.

Alternatively, a `Channel` may be used:
```kotlin
    routing {
    get("/customers") {
        val channel = call.application.produce {
            // a process to read the customer database and send each object as it becomes available
        }
        call.respond(channel)
    }
}
```

Using this technique, completely asynchronous output can be achieved with very little effort.

## Streaming Input

It's all very well for a REST service to send data asynchronously as it is produced, but that may not be particularly
helpful if the calling function is not able to process the objects as they arrive.
If the function simply accumulates the incoming JSON into a buffer and processes it only when the whole list has arrived
little will have been gained.

The `kjson-ktor` library includes functions to read a client response, deserialise it on the fly and hand each item to
the calling function as it arrives.

Streaming input is not quite as seamless as output; it requires a new function (an extension function on `HttpClient`).
The following:
```kotlin
        httpClient.receiveStreamJSON<Customer>("$host/customers") { customer ->
            // the code here is called with each Customer object as it is received
        }
```
will call the streaming output function from the previous section, with the `suspend` lambda being invoked with each
`Customer` object in turn as it arrives.

For streaming input to work in this manner, the highest-level JSON element in the received data must be an array.
As each array item is completed it is deserialised using `kjson` and the resulting object is passed to the `suspend`
lambda.

The full parameter list for the `receiveStreamJSON` function is (note that most parameters have appropriate defaults):

| Name             | Type                  | Default                    | Notes                                                 |
|------------------|-----------------------|----------------------------|-------------------------------------------------------|
| `urlString`      | `String`              |                            | The target URL                                        |
| `method`         | `HttpMethod`          | `HttpMethod.Get`           |                                                       |
| `body`           | `Any`                 | `EmptyContent`             | The **request** body (Usually only for POST ot PUT)   |
| `headers`        | `Headers`             | `Headers.Empty`            |                                                       |
| `expectedStatus` | `HttpStatusCode`      | `HttpStatusCode.OK`        | (see [Exception Handling](#exception-handling) below) |
| `config`         | `JSONConfig`          | `JSONConfig.defaultConfig` | The config to be used by `kjson` deserialisation      |
| `consumer`       | `suspend (T) -> Unit` |                            | Called with each array item                           |

A second form is available with identical parameters except that the URL is specified as a `Url` rather than a `String`.

**IMPORTANT:** Ktor does not allow access to the `JSONConfig` specified in the client configuration, so if `kjson`
configuration options are required, the `JSONConfig` must be specified on `receiveStreamJSON()` call.

If a POST or PUT request body is provided, the function will add a `Content-Type: application/json` header, if no
`Content-Type` header has been supplied using the `headers` parameter.

### Exception Handling

If the response status code is not the one expected, a `JSONKtorClientException` will be thrown.
The exception will include information from the `HttpResponse` object, so that the full details of unexpected response
may be examined.

The properties of `JSONKtorClientException` are:

| Name              | Type             |
|-------------------|------------------|
| `urlString`       | `String`         |
| `statusCode`      | `HttpStatusCode` |
| `responseHeaders` | `Headers`        |
| `responseBody`    | `ByteArray?`     |
| `config`          | `JSONConfig`     |

The `JSONKtorClientException` also has functions to allow access to the response body:
- `bodyAsString()`: converts to `String` (using the character set in the response `Content-type` header if specified)
- `body<T>()`: deserialises the body as a JSON object (also using the character set as above)

For example, if the called endpoint returns an `ErrorResponse` object in the case of errors:
```kotlin
        try {
            httpClient.receiveStreamJSON<Customer>("$host/customers") { customer ->
                // process each customer
            }
        } catch (e: JSONKtorReceiveException) {
            val errorResponse = e.body<ErrorResponse>()
            // make use of ErrorResponse object, for example log the content
            // (note that errorResponse may be null if the response body was "null")
        }
```
The above is an over-simplified example; in the real world the code to deserialise the `ErrorResponse` object would most
likely be made conditional on the value of the status code (available at `e.statusCode`).
It would probably also be wrapped in its own `try ... catch` block.

## JSON Lines

The [JSON Lines](https://jsonlines.org/) specification allows multiple JSON values to be specified in a single stream of
data, separated by newline (`\u000a`) characters.
For example, events may be logged to a file as a sequence of objects on separate lines; the alternative would be to
output a JSON array, but this would require a "`]`" terminator, complicating the shutdown of the process (particularly
abnormal shutdown).

```json lines
{"time":"2023-06-24T12:24:10.321+10:00","eventType":"ACCOUNT_OPEN","accountNumber": "123456789"}
{"time":"2023-06-24T12:24:10.321+10:00","eventType":"DEPOSIT","accountNumber": "123456789","amount":"1000.00"}
```

### Output JSON Lines

The JSON Lines format is particularly suitable for streaming data, and the `kjson-ktor` library includes functions to
output JSON Lines data from a `Channel` or a `Flow`:
```kotlin
        call.respondLines(channel)
```
or:
```kotlin
        call.respondLines(flow)
```

### Receiving JSON Lines

The `receiveStreamJSON` functions have equivalent functions named `receiveStreamJSONLines`; these functions operate in
an identical manner to the original functions except that the input stream is expected to be in JSON Lines form.

## Dependency Specification

The latest version of the library is 1.4, and it may be obtained from the Maven Central repository.
This version has been built using version 3.0.3 of Ktor.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>kjson-ktor</artifactId>
      <version>1.4</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'io.kjson:kjson-ktor:1.4'
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:kjson-ktor:1.4")
```

Peter Wall

2025-03-24
