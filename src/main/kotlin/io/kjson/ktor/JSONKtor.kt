/*
 * @(#) JSONKtor.kt
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

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import io.ktor.http.ContentType
import io.ktor.http.withCharset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.OutgoingContent.WriteChannelContent
import io.ktor.http.content.TextContent
import io.ktor.serialization.ContentConverter
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset

import io.kstuff.pipeline.ChannelCoAcceptor
import io.kstuff.pipeline.codec.CoDecoderFactory
import io.kstuff.pipeline.simpleCoAcceptor

import io.kjson.JSONConfig
import io.kjson.JSONCoPipeline
import io.kjson.JSONCoStreamer
import io.kjson.JSONDeserializer
import io.kjson.stringifyJSON
import io.kjson.util.JSONDeserializerCoPipeline

/**
 * JSON [ContentConverter] for Ktor.  Converts to/from JSON using the [kjson](https://github.com/pwall567/kjson)
 * library.
 *
 * @author  Peter Wall
 */
class JSONKtor(
    private val contentType: ContentType,
    private val config: JSONConfig = JSONConfig.defaultConfig,
) : ContentConverter {

    /**
     * Serialize output object to an [OutgoingContent].  Creates a [WriteChannelContent] (streaming) if the
     * `streamOutput` flag is set in the [JSONConfig], otherwise creates a [TextContent] (non-streaming).
     */
    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?,
    ): OutgoingContent? = when {
        !contentType.match(this.contentType) -> null
        !config.streamOutput -> TextContent(value.stringifyJSON(config), contentType.withCharset(charset))
        value is JSONLinesOutput -> createStreamedJSONLinesContent(value, applicationJSONLines, charset, config)
        else -> createStreamedJSONContent(value, contentType, charset, config)
    }

    /**
     * Deserialize JSON input to a specified type.  If the top-level type is a [Channel] or a [Flow], the object is
     * returned immediately and the data is streamed asynchronously into it.
     */
    override suspend fun deserialize(
        charset: Charset,
        typeInfo: TypeInfo,
        content: ByteReadChannel,
    ): Any? = when (typeInfo.type) {
        Flow::class -> deserializeFlow(charset, typeInfo.getParamType(), content)
        Channel::class, ReceiveChannel::class -> deserializeChannel(charset, typeInfo.getParamType(), content)
        else -> deserializeOther(charset, typeInfo, content)
    }

    private suspend fun deserializeFlow(
        charset: Charset,
        type: KType, // Flow parameter type
        content: ByteReadChannel,
    ): Flow<Any?> = flow {
        val pipeline = CoDecoderFactory.getDecoder(
            charset = charset,
            downstream = JSONCoPipeline(
                downstream = simpleCoAcceptor {
                    emit(JSONDeserializer.deserialize(type, it, config))
                },
                parseOptions = config.parseOptions,
            ),
        )
        content.copyToPipeline(pipeline, config.readBufferSize)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun deserializeChannel(
        charset: Charset,
        type: KType, // Channel parameter type
        content: ByteReadChannel,
    ): ReceiveChannel<Any?> = CoroutineScope(coroutineContext).produce(JSONReceiveCoroutineContext(type)) {
        val pipeline = CoDecoderFactory.getDecoder(
            charset = charset,
            downstream = JSONCoPipeline(
                downstream = JSONDeserializerCoPipeline(
                    type = type,
                    downstream = ChannelCoAcceptor(this),
                    config = config,
                ),
                parseOptions = config.parseOptions,
            ),
        )
        content.copyToPipeline(pipeline, config.readBufferSize)
        close()
    }

    private suspend fun deserializeOther(
        charset: Charset,
        typeInfo: TypeInfo,
        content: ByteReadChannel,
    ): Any? {
        val pipeline = CoDecoderFactory.getDecoder(
            charset = charset,
            downstream = JSONCoStreamer(config.parseOptions),
        )
        content.copyToPipeline(pipeline, config.readBufferSize)
        pipeline.close()
        typeInfo.kotlinType?.let { return JSONDeserializer.deserialize(it, pipeline.result, config) }
        return  JSONDeserializer.deserialize(typeInfo.type, pipeline.result, config)
    }

    /**
     * A [CoroutineContext] for [Channel] deserialization coroutines.
     */
    data class JSONReceiveCoroutineContext(val type: KType) :
            AbstractCoroutineContextElement(JSONReceiveCoroutineContext) {

        companion object Key : CoroutineContext.Key<JSONReceiveCoroutineContext>

    }

}
