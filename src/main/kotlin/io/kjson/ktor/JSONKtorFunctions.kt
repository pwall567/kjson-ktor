/*
 * @(#) JSONKtorFunctions.kt
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
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow

import io.ktor.http.ContentType
import io.ktor.http.HeaderValue
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.parseHeaderValue
import io.ktor.http.withCharset
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.Configuration
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.reifiedType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.charsets.Charset

import io.kstuff.pipeline.IntCoAcceptor
import io.kstuff.pipeline.codec.CoEncoderFactory
import io.kstuff.util.CoOutput

import io.kjson.JSONConfig
import io.kjson.coStringifyJSON
import io.kjson.toKType
import io.kjson.ktor.io.KtorByteChannelCoAcceptor
import io.kjson.ktor.io.KtorByteChannelOutput
import io.kjson.ktor.io.KtorOutgoingContent
import io.kjson.util.CoOutputChannel

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

/** The `application/json` content type */
val applicationJSON = ContentType.Application.Json

/** The `application/json` content type as a string */
val applicationJSONString = applicationJSON.toString()

/** The `application/jsonl` content type */
val applicationJSONLines = ContentType("application", "jsonl")

/** The `application/json` content type as a string */
val applicationJSONLinesString = applicationJSONLines.toString()

/**
 * Get a [Headers] containing a `Content-Type` of `application/json`.
 */
fun contentTypeJSON(): Headers = headersOf(HttpHeaders.ContentType, applicationJSONString)

/**
 * Get a [Headers] containing a `Content-Type` of `application/jsonl`.
 */
fun contentTypeJSONLines(): Headers = headersOf(HttpHeaders.ContentType, applicationJSONLinesString)

/**
 * Get a [Headers] containing a `Content-Type` of `application/json` with the specified [Charset].
 */
fun contentTypeJSON(charset: Charset): Headers =
        headersOf(HttpHeaders.ContentType, applicationJSON.withCharset(charset).toString())

/**
 * Get a [Headers] containing a `Content-Type` of `application/jsonl` with the specified [Charset].
 */
fun contentTypeJSONLines(charset: Charset): Headers =
        headersOf(HttpHeaders.ContentType, applicationJSONLines.withCharset(charset).toString())

/**
 * Create a [KtorByteChannelOutput] (convenience function).
 */
fun channelOutput(
    channel: ByteWriteChannel,
    charset: Charset = Charsets.UTF_8,
) = KtorByteChannelOutput(channel, charset)

/**
 * Create an [OutgoingContent] to stream JSON output.
 */
fun createStreamedJSONContent(
    value: Any?,
    contentType: ContentType = ContentType.Application.Json,
    charset: Charset = Charsets.UTF_8,
    config: JSONConfig = JSONConfig.defaultConfig,
): OutgoingContent = KtorOutgoingContent(contentType.withCharset(charset)) {
    val output = CoOutputChannel(
        downstream = CoEncoderFactory.getEncoder(
            charset = charset,
            downstream = KtorByteChannelCoAcceptor(this),
        )
    )
    value.coStringifyJSON(config, output)
    output.close()
}

/**
 * Create an [OutgoingContent] to stream JSON Lines output.
 */
fun createStreamedJSONLinesContent(
    value: JSONLinesOutput,
    contentType: ContentType = ContentType.Application.Json,
    charset: Charset = Charsets.UTF_8,
    config: JSONConfig = JSONConfig.defaultConfig,
): OutgoingContent = KtorOutgoingContent(contentType.withCharset(charset)) {
    val output = CoOutputChannel(
        downstream = CoEncoderFactory.getEncoder(
            charset = charset,
            downstream = KtorByteChannelCoAcceptor(this),
        )
    )
    value.coOutput(config, output)
    output.close()
}

/**
 * Respond to a call using streamed data.
 *
 * @param   contentType     the [ContentType]
 * @param   status          the [HttpStatusCode] being returned
 * @param   contentLength   the content length (if known)
 * @param   charset         the [Charset]
 * @param   producer        the producer function which will write the data to a [CoOutput] (supplied as receiver)
 */
suspend fun ApplicationCall.respondStream(
    contentType: ContentType? = null,
    status: HttpStatusCode? = null,
    contentLength: Long? = null,
    charset: Charset? = null,
    producer: suspend CoOutput.() -> Unit
) {
    respond(KtorOutgoingContent(contentType, status, contentLength) {
        val pipeline = CoEncoderFactory.getEncoder(charset ?: Charsets.UTF_8, KtorByteChannelCoAcceptor(this))
        CoOutputChannel(pipeline).producer()
    })
}

/**
 * Respond to a call using JSON Lines form, using a [Flow].
 *
 * @param   flow        a [Flow] supplying the data.
 */
suspend fun ApplicationCall.respondLines(flow: Flow<Any?>) {
    respond(JSONLinesFlowOutput(flow))
}

/**
 * Respond to a call using JSON Lines form, using a [ReceiveChannel].
 *
 * @param   channel     a [ReceiveChannel] supplying the data.
 */
suspend fun ApplicationCall.respondLines(channel: ReceiveChannel<Any?>) {
    respond(JSONLinesChannelOutput(channel))
}

/**
 * Get the `Content-Type` header, parsed into a [HeaderValue].
 */
fun Headers.parsedContentTypeHeader(): HeaderValue? = parseHeaderValue(this[HttpHeaders.ContentType]).firstOrNull()

/**
 * Get a named parameter value from a header (_e.g._ the `charset` value in `application/json;charset=UTF-8`).
 */
fun HeaderValue.getParam(name: String): String? = params.find { it.name.equals(name, ignoreCase = true) }?.value

/**
 * Copy data from a [ByteReadChannel] to an [IntCoAcceptor].
 */
suspend fun ByteReadChannel.copyToPipeline(
    acceptor: IntCoAcceptor<*>,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
) {
    val buffer = ByteArray(bufferSize)
    while (!isClosedForRead) {
        val bytesRead = readAvailable(buffer, 0, buffer.size)
        if (bytesRead < 0)
            break
        for (i in 0 until bytesRead)
            acceptor.accept(buffer[i].toInt() and 0xFF)
    }
    acceptor.close()
}

/**
 * Get a selected generic class type parameter from a Ktor [TypeInfo].
 */
fun TypeInfo.getParamType(index: Int = 0): KType {
    val kType: KType = kotlinType ?: reifiedType.toKType()
    return kType.arguments.getOrNull(index)?.type ?:
            throw JSONKtorException("Insufficient type information to deserialize generic class $type")
}
