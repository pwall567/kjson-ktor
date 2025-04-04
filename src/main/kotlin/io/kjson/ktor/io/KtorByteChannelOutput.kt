/*
 * @(#) KtorByteChannelOutput.kt
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

package io.kjson.ktor.io

import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets

import io.kstuff.pipeline.codec.CoEncoderFactory
import io.kstuff.util.CoOutputFlushable

/**
 * A [CoOutputFlushable] implementation that writes to a Ktor [ByteWriteChannel].
 *
 * @author  Peter Wall
 */
class KtorByteChannelOutput(
    channel: ByteWriteChannel,
    charset: Charset = Charsets.UTF_8,
) : CoOutputFlushable() {

    private val pipeline = CoEncoderFactory.getEncoder(charset, KtorByteChannelCoAcceptor(channel))

    override suspend fun invoke(ch: Char) {
        pipeline.accept(ch.code)
    }

    override suspend fun flush() {
        pipeline.flush()
    }

}
