/*
 * @(#) JSONLinesChannelOutput.kt
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

import kotlinx.coroutines.channels.ReceiveChannel

import io.kstuff.util.CoOutput
import io.kstuff.util.CoOutputFlushable
import io.kstuff.util.output

import io.kjson.JSONCoStringify.outputJSON
import io.kjson.JSONConfig

/**
 * A class to output JSON Lines format from a [ReceiveChannel].
 *
 * @author  Peter Wall
 */
class JSONLinesChannelOutput(private val channel: ReceiveChannel<Any?>) : JSONLinesOutput {

    override suspend fun coOutput(config: JSONConfig, coOutput: CoOutput) {
        val iterator = channel.iterator()
        while (iterator.hasNext()) {
            coOutput.outputJSON(iterator.next(), config)
            coOutput.output('\n')
            if (coOutput is CoOutputFlushable)
                coOutput.flush()
        }
    }

}
