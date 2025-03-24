/*
 * @(#) JSONKtorClientResponseTest.kt
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
import kotlin.test.fail
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.set

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldThrow

import io.kjson.JSON.asInt
import io.kjson.JSON.asString
import io.kjson.JSONStringify.appendJSON
import io.kjson.stringifyJSON
import io.kjson.ktor.test.Dummy1

class JSONKtorClientResponseTest {

    @Test fun `should deserialise client response`() = runBlocking {
        val mockEngine = MockEngine {
            respond("""{"a":"first","b":1}""", HttpStatusCode.OK, contentTypeJSON(Charsets.UTF_8))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson()
            }
        }
        val response: Dummy1 = httpClient.get("/anything").body()
        response.a shouldBe "first"
        response.b shouldBe 1
    }

    @Test fun `should deserialise client response using custom`() = runBlocking {
        val mockEngine = MockEngine {
            respond("""{"aaa":"what?","bbb":99}""", HttpStatusCode.OK, contentTypeJSON(Charsets.UTF_8))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson {
                    fromJSONObject {
                        Dummy1(it["aaa"].asString, it["bbb"].asInt)
                    }
                }
            }
        }
        val response: Dummy1 = httpClient.get("/anything").body()
        response.a shouldBe "what?"
        response.b shouldBe 99
    }

    @Test fun `should deserialise client response as a Flow`() = runBlocking {
        val mockEngine = MockEngine {
            respond("""["one","two","three"]""", HttpStatusCode.OK, contentTypeJSON())
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson()
            }
        }
        val strings = mutableListOf<String>()
        val response: Flow<String> = httpClient.get("/flo").body()
        response.collect {
            strings.add(it)
        }
        strings.size shouldBe 3
        strings[0] shouldBe "one"
        strings[1] shouldBe "two"
        strings[2] shouldBe "three"
    }

    @Test fun `should deserialise client response as a Channel`() = runBlocking {
        val mockEngine = MockEngine {
            respond("""["one","two","three"]""", HttpStatusCode.OK, contentTypeJSON())
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson()
            }
        }
        val strings = mutableListOf<String>()
        val channelResponse: Channel<String> = httpClient.get("/ch").body()
        val iterator = channelResponse.iterator()
        while (iterator.hasNext())
            strings.add(iterator.next())
        strings.size shouldBe 3
        strings[0] shouldBe "one"
        strings[1] shouldBe "two"
        strings[2] shouldBe "three"
    }

    @Test fun `should deserialise complex client response as a Channel`() = runBlocking {
        val mockEngine = MockEngine {
            respond("""[["one","two","three"],["four","five","six"]]""", HttpStatusCode.OK, contentTypeJSON())
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson()
            }
        }
        val strings = mutableListOf<List<String>>()
        val channelResponse: Channel<List<String>> = httpClient.get("/ch").body()
        val iterator = channelResponse.iterator()
        while (iterator.hasNext())
            strings.add(iterator.next())
        strings.size shouldBe 2
        with(strings[0]) {
            size shouldBe 3
            this[0] shouldBe "one"
            this[1] shouldBe "two"
            this[2] shouldBe "three"
        }
        with(strings[1]) {
            size shouldBe 3
            this[0] shouldBe "four"
            this[1] shouldBe "five"
            this[2] shouldBe "six"
        }
    }

    @Test fun `should receive client data using streaming interface`() = runBlocking {
        val responseData = listOf(
            Dummy1("one", 1),
            Dummy1("two", 2),
            Dummy1("three", 3),
        )
        val mockEngine = MockEngine {
            respond(responseData.stringifyJSON(), headers = contentTypeJSON())
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson()
            }
        }
        var number = 0
        httpClient.receiveStreamJSON<Dummy1>("/any") {
            when (number++) {
                0 -> it shouldBe Dummy1("one", 1)
                1 -> it shouldBe Dummy1("two", 2)
                2 -> it shouldBe Dummy1("three", 3)
            }
        }
    }

    @Test fun `should receive client data using streaming interface and complex URL`() = runBlocking {
        val responseData = listOf(
            Dummy1("one", 1),
            Dummy1("two", 2),
            Dummy1("three", 3),
        )
        val mockEngine = MockEngine {
            respond(responseData.stringifyJSON(), headers = contentTypeJSON())
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson()
            }
        }
        var number = 0
        val url = URLBuilder().apply {
            set(path = "/any")
        }.build()
        httpClient.receiveStreamJSON<Dummy1>(url) {
            when (number++) {
                0 -> it shouldBe Dummy1("one", 1)
                1 -> it shouldBe Dummy1("two", 2)
                2 -> it shouldBe Dummy1("three", 3)
            }
        }
    }

    @Test fun `should throw JSONKtorClientException on error using streaming interface`() = runBlocking {
        val errorResponse = ErrorResponse("ERR1", "Error message")
        val mockEngine = MockEngine {
            respond(errorResponse.stringifyJSON(), HttpStatusCode.BadRequest, contentTypeJSON())
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson()
            }
        }
        shouldThrow<JSONKtorClientException> {
            httpClient.receiveStreamJSON<Dummy1>("http://example.com/any") {
                fail("Shouldn't get here")
            }
        }.let {
            it.message shouldBe "Unexpected response status (400 Bad Request) - http://example.com/any"
            it.urlString shouldBe "http://example.com/any"
            it.statusCode shouldBe HttpStatusCode.BadRequest
            val responseBody: ErrorResponse = it.body() ?: fail("Response body was null")
            responseBody.code shouldBe "ERR1"
            responseBody.message shouldBe "Error message"
            it.bodyAsString() shouldBe """{"code":"ERR1","message":"Error message"}"""
        }
    }

    @Test fun `should receive JSON Lines client data using streaming interface`() = runBlocking {
        val responseString = buildString {
            appendJSON(Dummy1("one", 1))
            append('\n')
            appendJSON(Dummy1("two", 2))
            append('\n')
            appendJSON(Dummy1("three", 3))
            append('\n')
        }
        val mockEngine = MockEngine {
            respond(responseString, headers = contentTypeJSONLines())
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson()
            }
        }
        var number = 0
        httpClient.receiveStreamJSONLines<Dummy1>("/any") {
            when (number++) {
                0 -> it shouldBe Dummy1("one", 1)
                1 -> it shouldBe Dummy1("two", 2)
                2 -> it shouldBe Dummy1("three", 3)
            }
        }
    }

    @Test fun `should receive JSON Lines client data with charset`() = runBlocking {
        val responseString = buildString {
            appendJSON(Dummy1("tahi", 1))
            append('\n')
            appendJSON(Dummy1("rua", 2))
            append('\n')
            appendJSON(Dummy1("toru", 3))
            append('\n')
        }
        val mockEngine = MockEngine {
            respond(responseString, headers = contentTypeJSONLines(Charsets.UTF_8))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson()
            }
        }
        var number = 0
        val url = URLBuilder().apply {
            set(path = "/any")
        }.build()
        httpClient.receiveStreamJSONLines<Dummy1>(url) {
            when (number++) {
                0 -> it shouldBe Dummy1("tahi", 1)
                1 -> it shouldBe Dummy1("rua", 2)
                2 -> it shouldBe Dummy1("toru", 3)
            }
        }
    }

    data class ErrorResponse(
        val code: String,
        val message: String,
    )

}
