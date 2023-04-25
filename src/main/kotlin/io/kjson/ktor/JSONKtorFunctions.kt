/*
 * @(#) JSONKtorFunctions.kt
 *
 * kjson-ktor  Reflection-based JSON serialization and deserialization for ktor
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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.http.takeFrom
import io.ktor.http.withCharset
import io.ktor.serialization.Configuration
import io.ktor.server.plugins.NotFoundException
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset

import io.kjson.JSONCoPipeline
import io.kjson.JSONConfig
import io.kjson.JSONDeserializer
import io.kjson.ktor.JSONKtor.Companion.copyToPipeline
import net.pwall.pipeline.codec.CoDecoderFactory
import net.pwall.pipeline.simpleCoAcceptor

/**
 * Register the `kjson` content converter, configuring the `JSONConfig` with a lambda.
 */
fun Configuration.kjson(
    contentType: ContentType = ContentType.Application.Json,
    block: JSONConfig.() -> Unit = {},
) {
    register(contentType, JSONKtor(contentType, JSONConfig(block)))
}

/**
 * Register the `kjson` content converter, supplying a `JSONConfig`.
 */
fun Configuration.kjson(
    config: JSONConfig,
    contentType: ContentType = ContentType.Application.Json,
) {
    register(contentType, JSONKtor(contentType, config))
}

/**
 * Register the `kjson` client content converter, configuring the `JSONConfig` with a lambda.
 */
fun ContentNegotiation.Config.kjson(
    contentType: ContentType = ContentType.Application.Json,
    block: JSONConfig.() -> Unit = {},
) {
    register(contentType, JSONKtor(contentType, JSONConfig(block)))
}

/**
 * Register the `kjson` client content converter, supplying a `JSONConfig`.
 */
fun ContentNegotiation.Config.kjson(
    config: JSONConfig,
    contentType: ContentType = ContentType.Application.Json,
) {
    register(contentType, JSONKtor(contentType, config))
}

/** The `application/json` content type */
val applicationJSON = ContentType.Application.Json

/** The `application/json` content type as a string */
val applicationJSONString = ContentType.Application.Json.toString()

/**
 * Get a [Headers] containing a `Content-Type` of `application/json`.
 */
fun contentTypeJSON(): Headers = headersOf(HttpHeaders.ContentType, applicationJSONString)

/**
 * Get a [Headers] containing a `Content-Type` of `application/json` with the specified [Charset].
 */
fun contentTypeJSON(charset: Charset): Headers =
        headersOf(HttpHeaders.ContentType, applicationJSON.withCharset(charset).toString())

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
    crossinline consumer: suspend (T) -> Unit
) {
    val requestBuilder = HttpRequestBuilder()
    requestBuilder.url.takeFrom(urlString)
    requestBuilder.method = method
    requestBuilder.headers.appendAll(headers)
    if (body !== EmptyContent)
        requestBuilder.setBody(body)
    executeStreamJSON<T>(requestBuilder, expectedStatus, config, consumer)
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
    crossinline consumer: suspend (T) -> Unit
) {
    val requestBuilder = HttpRequestBuilder()
    requestBuilder.url.takeFrom(url)
    requestBuilder.method = method
    requestBuilder.headers.appendAll(headers)
    if (body !== EmptyContent)
        requestBuilder.setBody(body)
    executeStreamJSON<T>(requestBuilder, expectedStatus, config, consumer)
}

/**
 * Make a client call with the parameters supplied in a [HttpRequestBuilder], receiving the response as a stream.  The
 * response must be in the form of a JSON array, and each array item will be deserialized and passed to the `consumer`
 * function as it is received.
 *
 * @param   T               the type of the array item
 * @param   expectedStatus  the expected response status (default 200 OK)
 * @param   config          the [JSONConfig] to use when deserializing
 * @param   consumer        the consumer function (will be called with each array item)
 */
suspend inline fun <reified T : Any> HttpClient.executeStreamJSON(
    requestBuilder: HttpRequestBuilder,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    config: JSONConfig = JSONConfig.defaultConfig,
    crossinline consumer: suspend (T) -> Unit
) {
    prepareRequest(requestBuilder).execute { response ->
        when (response.status) {
            expectedStatus -> {
                val pipeline = CoDecoderFactory.getDecoder(config.charset, JSONCoPipeline(simpleCoAcceptor {
                    val item = JSONDeserializer.deserialize<T>(it, config) ?:
                    throw JSONKtorException("Streaming array item was null - ${requestBuilder.url}")
                    consumer(item)
                }))
                response.body<ByteReadChannel>().copyToPipeline(pipeline, config.readBufferSize)
            }
            HttpStatusCode.NotFound -> throw NotFoundException("Not found - ${requestBuilder.url}")
            else -> throw JSONKtorException("Unexpected status code - ${response.status} - ${requestBuilder.url}")
        }
    }
}
