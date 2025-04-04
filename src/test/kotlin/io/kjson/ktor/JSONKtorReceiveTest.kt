/*
 * @(#) JSONKtorReceiveTest.kt
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

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication

import io.kstuff.test.shouldBe

import io.kjson.JSON.asInt
import io.kjson.JSON.asString
import io.kjson.ktor.test.Dummy1

class JSONKtorReceiveTest {

    @Test fun `should receive JSON POST object`() = testApplication {
        application {
            install(ContentNegotiation) {
                kjson {
                    readBufferSize = 1280
                }
            }
            routing {
                post("/xx") {
                    val content = call.receive<Dummy1>()
                    call.respond(Dummy1("${content.a} 1", content.b + 1))
                }
            }
        }
        val response = client.post("/xx") {
            contentType(ContentType.Application.Json)
            setBody("""{"a":"Fred","b":1}""")
        }
        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText() shouldBe """{"a":"Fred 1","b":2}"""
    }

    @Test fun `should receive JSON POST object using custom`() = testApplication {
        application {
            install(ContentNegotiation) {
                kjson {
                    readBufferSize = 1280
                    fromJSONObject {
                        Dummy1(it["aaa"].asString, it["bbb"].asInt)
                    }
                }
            }
            routing {
                post("/xx") {
                    val content = call.receive<Dummy1>()
                    call.respond(Dummy1("${content.a} 1", content.b + 1))
                }
            }
        }
        val response = client.post("/xx") {
            contentType(ContentType.Application.Json)
            setBody("""{"aaa":"Fred","bbb":1}""")
        }
        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText() shouldBe """{"a":"Fred 1","b":2}"""
    }

    @Test fun `should receive generic POST object`() = testApplication {
        application {
            install(ContentNegotiation) {
                kjson {
                    readBufferSize = 1280
                }
            }
            routing {
                post("/xx") {
                    val content = call.receive<List<String>>()
                    call.respond(content.joinToString(":"))
                }
            }
        }
        val response = client.post("/xx") {
            contentType(ContentType.Application.Json)
            setBody("""["alpha","beta","gamma"]""")
        }
        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText() shouldBe "alpha:beta:gamma"
    }

}
