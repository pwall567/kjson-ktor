/*
 * @(#) JSONKtorClientException.kt
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
import io.kjson.JSONConfig
import io.kjson.JSONDeserializer
import io.kjson.JSONStreamer
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode

import net.pwall.pipeline.StringAcceptor
import net.pwall.pipeline.codec.DecoderFactory

/**
 * Exception class for errors in `receiveStreamJSON` _etc._
 *
 * @author  Peter Wall
 */
class JSONKtorClientException(
    val urlString: String,
    val statusCode: HttpStatusCode,
    val responseHeaders: Headers,
    val responseBody: ByteArray?,
    val config: JSONConfig,
) : JSONKtorException("Unexpected response status ($statusCode) - $urlString") {

    /**
     * Get the response body as an object of the implied type.
     */
    inline fun <reified T: Any> body(): T? = body(typeOf<T>())

    /**
     * Get the response body as an object of the specified [KType].
     */
    fun <T: Any> body(type: KType): T? {
        if (responseBody == null)
            return null
        val parsedContentTypeHeader = responseHeaders.parsedContentTypeHeader()
        if (parsedContentTypeHeader?.value != applicationJSONString)
            throw JSONKtorException("Content-Type not $applicationJSONString - $urlString")
        val charsetName = parsedContentTypeHeader.getParam("charset") ?: config.charset.name()
        val pipeline = DecoderFactory.getDecoder(charsetName, JSONStreamer(config.parseOptions))
        pipeline.accept(responseBody)
        @Suppress("UNCHECKED_CAST")
        return JSONDeserializer.deserialize(type, pipeline.result, config) as T?
    }

    /**
     * Get the response body as a string.
     */
    fun bodyAsString(): String? {
        if (responseBody == null)
            return null
        val parsedContentTypeHeader = responseHeaders.parsedContentTypeHeader()
        val charsetName = parsedContentTypeHeader?.getParam("charset") ?: config.charset.name()
        val pipeline = DecoderFactory.getDecoder(charsetName, StringAcceptor(responseBody.size))
        pipeline.accept(responseBody)
        return pipeline.result
    }

}
