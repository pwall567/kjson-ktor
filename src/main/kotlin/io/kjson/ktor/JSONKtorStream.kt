/*
 * @(#) JSONKtorStream.kt
 *
 * kjson-ktor  Reflection-based JSON serialization and deserialization for Ktor
 * Copyright (c) 2023 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.kjson.ktor

import kotlin.reflect.KType
import kotlin.reflect.typeOf

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.utils.EmptyContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.takeFrom
import io.ktor.utils.io.ByteReadChannel

import io.kstuff.pipeline.codec.CoDecoderFactory
import io.kstuff.pipeline.simpleCoAcceptor

import io.kjson.JSONCoPipeline
import io.kjson.JSONConfig
import io.kjson.JSONDeserializer
import io.kjson.JSONLinesCoPipeline

/**
 * Make a client call, receiving the response as a stream.  The response must be in the form of a JSON array, and each
 * array item will be deserialized and passed to the `consumer` function as it is received.
 *
 * @param   T               the type of the array item
 * @param   urlString       the URL as a [String]
 * @param   method          the HTTP method (default GET)
 * @param   body            the request body if required
 * @param   expectedStatus  the expected response status (default 200 OK)
 * @param   config          the [JSONConfig] to use when deserializing
 * @param   consumer        the consumer function (will be called with each array item)
 */
suspend inline fun <reified T : Any> HttpClient.receiveStreamJSON(
    urlString: String,
    method: HttpMethod = HttpMethod.Get,
    body: Any = EmptyContent,
    headers: Headers = Headers.Empty,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    config: JSONConfig = JSONConfig.defaultConfig,
    noinline consumer: suspend (T) -> Unit
) {
    receiveStreamJSON(typeOf<T>(), urlString, method, body, headers, expectedStatus, config, consumer)
}

/**
 * Make a client call, receiving the response as a stream.  The response must be in the form of a JSON array, and each
 * array item will be deserialized and passed to the `consumer` function as it is received.
 *
 * @param   T               the type of the array item
 * @param   type            the type of the array item as a [KType]
 * @param   urlString       the URL as a [String]
 * @param   method          the HTTP method (default GET)
 * @param   body            the request body if required
 * @param   expectedStatus  the expected response status (default 200 OK)
 * @param   config          the [JSONConfig] to use when deserializing
 * @param   consumer        the consumer function (will be called with each array item)
 */
suspend fun <T : Any> HttpClient.receiveStreamJSON(
    type: KType,
    urlString: String,
    method: HttpMethod = HttpMethod.Get,
    body: Any = EmptyContent,
    headers: Headers = Headers.Empty,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    config: JSONConfig = JSONConfig.defaultConfig,
    consumer: suspend (T) -> Unit
) {
    val requestBuilder = HttpRequestBuilder()
    requestBuilder.url.takeFrom(urlString)
    requestBuilder.method = method
    requestBuilder.headers.appendAll(headers)
    if (body !== EmptyContent) {
        requestBuilder.setBody(body)
        if (!requestBuilder.headers.contains(HttpHeaders.ContentType))
            requestBuilder.headers[HttpHeaders.ContentType] = applicationJSONString
    }
    executeStreamJSON(type, requestBuilder, expectedStatus, config, consumer)
}

/**
 * Make a client call, receiving the response as a stream.  The response must be in the form of a JSON array, and each
 * array item will be deserialized and passed to the `consumer` function as it is received.
 *
 * @param   T               the type of the array item
 * @param   url             the URL as a [Url]
 * @param   method          the HTTP method (default GET)
 * @param   body            the request body if required
 * @param   expectedStatus  the expected response status (default 200 OK)
 * @param   config          the [JSONConfig] to use when deserializing
 * @param   consumer        the consumer function (will be called with each array item)
 */
suspend inline fun <reified T : Any> HttpClient.receiveStreamJSON(
    url: Url,
    method: HttpMethod = HttpMethod.Get,
    body: Any = EmptyContent,
    headers: Headers = Headers.Empty,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    config: JSONConfig = JSONConfig.defaultConfig,
    noinline consumer: suspend (T) -> Unit
) {
    receiveStreamJSON(typeOf<T>(), url, method, body, headers, expectedStatus, config, consumer)
}

/**
 * Make a client call, receiving the response as a stream.  The response must be in the form of a JSON array, and each
 * array item will be deserialized and passed to the `consumer` function as it is received.
 *
 * @param   T               the type of the array item
 * @param   type            the type of the array item as a [KType]
 * @param   url             the URL as a [Url]
 * @param   method          the HTTP method (default GET)
 * @param   body            the request body if required
 * @param   expectedStatus  the expected response status (default 200 OK)
 * @param   config          the [JSONConfig] to use when deserializing
 * @param   consumer        the consumer function (will be called with each array item)
 */
suspend fun <T : Any> HttpClient.receiveStreamJSON(
    type: KType,
    url: Url,
    method: HttpMethod = HttpMethod.Get,
    body: Any = EmptyContent,
    headers: Headers = Headers.Empty,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    config: JSONConfig = JSONConfig.defaultConfig,
    consumer: suspend (T) -> Unit
) {
    val requestBuilder = HttpRequestBuilder()
    requestBuilder.url.takeFrom(url)
    requestBuilder.method = method
    requestBuilder.headers.appendAll(headers)
    if (body !== EmptyContent) {
        requestBuilder.setBody(body)
        if (!requestBuilder.headers.contains(HttpHeaders.ContentType))
            requestBuilder.headers[HttpHeaders.ContentType] = applicationJSONString
    }
    executeStreamJSON(type, requestBuilder, expectedStatus, config, consumer)
}

/**
 * Make a client call with the parameters supplied in a [HttpRequestBuilder], receiving the response as a stream.  The
 * response must be in the form of a JSON array, and each array item will be deserialized and passed to the `consumer`
 * function as it is received.
 *
 * @param   T               the type of the array item
 * @param   type            the type of the array item as a [KType]
 * @param   expectedStatus  the expected response status (default 200 OK)
 * @param   config          the [JSONConfig] to use when deserializing
 * @param   consumer        the consumer function (will be called with each array item)
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T : Any> HttpClient.executeStreamJSON(
    type: KType,
    requestBuilder: HttpRequestBuilder,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    config: JSONConfig = JSONConfig.defaultConfig,
    consumer: suspend (T) -> Unit
) {
    prepareRequest(requestBuilder).execute { response ->
        if (response.status == expectedStatus) {
            val parsedContentTypeHeader = response.headers.parsedContentTypeHeader()
            if (parsedContentTypeHeader?.value != applicationJSONString)
                throw JSONKtorException("Content-Type not $applicationJSONString - ${requestBuilder.url}")
            val charsetName = parsedContentTypeHeader.getParam("charset") ?: config.charset.name()
            val pipeline = CoDecoderFactory.getDecoder(
                charsetName = charsetName,
                downstream = JSONCoPipeline(simpleCoAcceptor {
                    val item = JSONDeserializer.deserialize(type, it, config) as T? ?:
                    throw JSONKtorException("Streaming array item was null - ${requestBuilder.url}")
                    consumer(item)
                }),
            )
            response.body<ByteReadChannel>().copyToPipeline(pipeline, config.readBufferSize)
        }
        else
            throw JSONKtorClientException(
                urlString = requestBuilder.url.toString(),
                statusCode = response.status,
                responseHeaders = response.headers,
                responseBody = response.body(),
                config = config,
            )
    }
}

/**
 * Make a client call, receiving the response in JSON Lines form, with a complete JSON value on each line.  Each value
 * will be deserialized and passed to the `consumer` function as it is received.
 *
 * @param   T               the type of the array item
 * @param   urlString       the URL as a [String]
 * @param   method          the HTTP method (default GET)
 * @param   body            the request body if required
 * @param   expectedStatus  the expected response status (default 200 OK)
 * @param   config          the [JSONConfig] to use when deserializing
 * @param   consumer        the consumer function (will be called with each array item)
 */
suspend inline fun <reified T : Any> HttpClient.receiveStreamJSONLines(
    urlString: String,
    method: HttpMethod = HttpMethod.Get,
    body: Any = EmptyContent,
    headers: Headers = Headers.Empty,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    config: JSONConfig = JSONConfig.defaultConfig,
    noinline consumer: suspend (T) -> Unit
) {
    receiveStreamJSONLines(typeOf<T>(), urlString, method, body, headers, expectedStatus, config, consumer)
}

/**
 * Make a client call, receiving the response in JSON Lines form, with a complete JSON value on each line.  Each value
 * will be deserialized and passed to the `consumer` function as it is received.
 *
 * @param   T               the type of the array item
 * @param   type            the type of the array item as a [KType]
 * @param   urlString       the URL as a [String]
 * @param   method          the HTTP method (default GET)
 * @param   body            the request body if required
 * @param   expectedStatus  the expected response status (default 200 OK)
 * @param   config          the [JSONConfig] to use when deserializing
 * @param   consumer        the consumer function (will be called with each array item)
 */
suspend fun <T : Any> HttpClient.receiveStreamJSONLines(
    type: KType,
    urlString: String,
    method: HttpMethod = HttpMethod.Get,
    body: Any = EmptyContent,
    headers: Headers = Headers.Empty,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    config: JSONConfig = JSONConfig.defaultConfig,
    consumer: suspend (T) -> Unit
) {
    val requestBuilder = HttpRequestBuilder()
    requestBuilder.url.takeFrom(urlString)
    requestBuilder.method = method
    requestBuilder.headers.appendAll(headers)
    if (body !== EmptyContent) {
        requestBuilder.setBody(body)
        if (!requestBuilder.headers.contains(HttpHeaders.ContentType))
            requestBuilder.headers[HttpHeaders.ContentType] = applicationJSONString
    }
    executeStreamJSONLines(type, requestBuilder, expectedStatus, config, consumer)
}

/**
 * Make a client call, receiving the response in JSON Lines form, with a complete JSON value on each line.  Each value
 * will be deserialized and passed to the `consumer` function as it is received.
 *
 * @param   T               the type of the array item
 * @param   url             the URL as a [Url]
 * @param   method          the HTTP method (default GET)
 * @param   body            the request body if required
 * @param   expectedStatus  the expected response status (default 200 OK)
 * @param   config          the [JSONConfig] to use when deserializing
 * @param   consumer        the consumer function (will be called with each array item)
 */
suspend inline fun <reified T : Any> HttpClient.receiveStreamJSONLines(
    url: Url,
    method: HttpMethod = HttpMethod.Get,
    body: Any = EmptyContent,
    headers: Headers = Headers.Empty,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    config: JSONConfig = JSONConfig.defaultConfig,
    noinline consumer: suspend (T) -> Unit
) {
    receiveStreamJSONLines(typeOf<T>(), url, method, body, headers, expectedStatus, config, consumer)
}

/**
 * Make a client call, receiving the response in JSON Lines form, with a complete JSON value on each line.  Each value
 * will be deserialized and passed to the `consumer` function as it is received.
 *
 * @param   T               the type of the array item
 * @param   type            the type of the array item as a [KType]
 * @param   url             the URL as a [Url]
 * @param   method          the HTTP method (default GET)
 * @param   body            the request body if required
 * @param   expectedStatus  the expected response status (default 200 OK)
 * @param   config          the [JSONConfig] to use when deserializing
 * @param   consumer        the consumer function (will be called with each array item)
 */
suspend fun <T : Any> HttpClient.receiveStreamJSONLines(
    type: KType,
    url: Url,
    method: HttpMethod = HttpMethod.Get,
    body: Any = EmptyContent,
    headers: Headers = Headers.Empty,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    config: JSONConfig = JSONConfig.defaultConfig,
    consumer: suspend (T) -> Unit
) {
    val requestBuilder = HttpRequestBuilder()
    requestBuilder.url.takeFrom(url)
    requestBuilder.method = method
    requestBuilder.headers.appendAll(headers)
    if (body !== EmptyContent) {
        requestBuilder.setBody(body)
        if (!requestBuilder.headers.contains(HttpHeaders.ContentType))
            requestBuilder.headers[HttpHeaders.ContentType] = applicationJSONString
    }
    executeStreamJSONLines(type, requestBuilder, expectedStatus, config, consumer)
}

/**
 * Make a client call with the parameters supplied in a [HttpRequestBuilder], receiving the response in JSON Lines form,
 * with a complete JSON value on each line.  Each value will be deserialized and passed to the `consumer` function as it
 * is received.
 *
 * @param   T               the type of the array item
 * @param   type            the type of the array item as a [KType]
 * @param   expectedStatus  the expected response status (default 200 OK)
 * @param   config          the [JSONConfig] to use when deserializing
 * @param   consumer        the consumer function (will be called with each array item)
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T : Any> HttpClient.executeStreamJSONLines(
    type: KType,
    requestBuilder: HttpRequestBuilder,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    config: JSONConfig = JSONConfig.defaultConfig,
    consumer: suspend (T) -> Unit
) {
    prepareRequest(requestBuilder).execute { response ->
        if (response.status == expectedStatus) {
            val parsedContentTypeHeader = response.headers.parsedContentTypeHeader()
            if (parsedContentTypeHeader?.value != applicationJSONLinesString)
                throw JSONKtorException("Content-Type not $applicationJSONLinesString - ${requestBuilder.url}")
            val charsetName = parsedContentTypeHeader.getParam("charset") ?: config.charset.name()
            val pipeline = CoDecoderFactory.getDecoder(
                charsetName = charsetName,
                downstream = JSONLinesCoPipeline(simpleCoAcceptor {
                    val item = JSONDeserializer.deserialize(type, it, config) as T? ?:
                    throw JSONKtorException("JSON Lines value was null - ${requestBuilder.url}")
                    consumer(item)
                }),
            )
            response.body<ByteReadChannel>().copyToPipeline(pipeline, config.readBufferSize)
        }
        else
            throw JSONKtorClientException(
                urlString = requestBuilder.url.toString(),
                statusCode = response.status,
                responseHeaders = response.headers,
                responseBody = response.body(),
                config = config,
            )
    }
}
