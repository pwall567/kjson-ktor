/*
 * @(#) JSONLinesFlowOutput.kt
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

import kotlinx.coroutines.flow.Flow

import io.kstuff.util.CoOutput
import io.kstuff.util.CoOutputFlushable
import io.kstuff.util.output

import io.kjson.JSONConfig
import io.kjson.JSONCoStringify.outputJSON

/**
 * A class to output JSON Lines format from a [Flow].
 *
 * @author  Peter Wall
 */
class JSONLinesFlowOutput(private val flow: Flow<Any?>) : JSONLinesOutput {

    override suspend fun coOutput(config: JSONConfig, coOutput: CoOutput) {
        flow.collect {
            coOutput.outputJSON(it, config)
            coOutput.output('\n')
            if (coOutput is CoOutputFlushable)
                coOutput.flush()
        }
    }

}
