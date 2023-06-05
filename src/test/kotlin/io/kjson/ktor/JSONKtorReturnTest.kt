/*
 * @(#) JSONKtorReturnTest.kt
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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.expect

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication

import io.kjson.JSONConfig
import io.kjson.ktor.test.Dummy1

class JSONKtorReturnTest {

    @Test fun `should return JSON object`() = testApplication {
        var conf: JSONConfig = JSONConfig.defaultConfig
        application {
            install(ContentNegotiation) {
                kjson {
                    conf = this
                    readBufferSize = 1280
                }
            }
            routing {
                get("/x") {
                    call.respond(Dummy1("Hello", 876))
                }
            }
        }
        val response = client.get("/x")
        expect(HttpStatusCode.OK) { response.status }
        expect("""{"a":"Hello","b":876}""") { response.bodyAsText() }
        expect(1280) { conf.readBufferSize }
        assertFalse(conf.streamOutput)
        assertNotSame(conf, JSONConfig.defaultConfig)
    }

    @Test fun `should return JSON object using streaming`() = testApplication {
        application {
            install(ContentNegotiation) {
                kjson {
                    streamOutput = true
                    readBufferSize = 1280
                }
            }
            routing {
                get("/x") {
                    call.respond(Dummy1("Hello", 876))
                }
            }
        }
        val response = client.get("/x")
        expect(HttpStatusCode.OK) { response.status }
        expect("""{"a":"Hello","b":876}""") { response.bodyAsText() }
    }

}
