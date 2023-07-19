/*
 * @(#) JSONKtorRespondLinesTest.kt
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
import kotlin.test.expect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.flow

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication

import io.kjson.ktor.test.Dummy1

class JSONKtorRespondLinesTest {

    @Test fun `should output using respondLines`() = testApplication {
        application {
            install(ContentNegotiation) {
                kjson {
                    streamOutput = true
                }
            }
            routing {
                get("/x") {
                    val flow = flow<Dummy1> {
                        emit(Dummy1("one", 111))
                        emit(Dummy1("two", 222))
                        emit(Dummy1("three", 333))
                    }
                    call.respondLines(flow)
                }
            }
        }
        val response = client.get("/x")
        expect(HttpStatusCode.OK) { response.status }
        expect("{\"a\":\"one\",\"b\":111}\n{\"a\":\"two\",\"b\":222}\n{\"a\":\"three\",\"b\":333}\n") {
            response.bodyAsText()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun `should output using respondLines with Channel`() = testApplication {
        application {
            install(ContentNegotiation) {
                kjson {
                    streamOutput = true
                }
            }
            routing {
                get("/x") {
                    val channel = CoroutineScope(coroutineContext).produce {
                        send(Dummy1("one", 1111))
                        send(Dummy1("two", 2222))
                        send(Dummy1("three", 3333))
                        close()
                    }
                    call.respondLines(channel)
                }
            }
        }
        val response = client.get("/x")
        expect(HttpStatusCode.OK) { response.status }
        expect("{\"a\":\"one\",\"b\":1111}\n{\"a\":\"two\",\"b\":2222}\n{\"a\":\"three\",\"b\":3333}\n") {
            response.bodyAsText()
        }
    }

}
