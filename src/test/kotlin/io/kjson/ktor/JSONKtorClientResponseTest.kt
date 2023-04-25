/*
 * @(#) JSONKtorClientResponseTest.kt
 *
 * kjson-ktor  Reflection-based JSON serialization and deserialization for ktor
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

import io.kjson.ktor.test.Dummy1
import io.kjson.stringifyJSON

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
        expect("first") { response.a }
        expect(1) { response.b }
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
        expect(3) { strings.size }
        expect("one") { strings[0] }
        expect("two") { strings[1] }
        expect("three") { strings[2] }
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
        expect(3) { strings.size }
        expect("one") { strings[0] }
        expect("two") { strings[1] }
        expect("three") { strings[2] }
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
        expect(2) { strings.size }
        with(strings[0]) {
            expect(3) { size }
            expect("one") { this[0] }
            expect("two") { this[1] }
            expect("three") { this[2] }
        }
        with(strings[1]) {
            expect(3) { size }
            expect("four") { this[0] }
            expect("five") { this[1] }
            expect("six") { this[2] }
        }
    }

    @Test fun `should receive client data using streaming interface`() = runBlocking {
        val responseData = listOf(
            Dummy1("one", 1),
            Dummy1("two", 2),
            Dummy1("three", 3),
        )
        val mockEngine = MockEngine {
            respond(responseData.stringifyJSON())
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson()
            }
        }
        var number = 0
        httpClient.receiveStreamJSON<Dummy1>("/any") {
            when (number++) {
                0 -> expect(Dummy1("one", 1)) { it }
                1 -> expect(Dummy1("two", 2)) { it }
                2 -> expect(Dummy1("three", 3)) { it }
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
            respond(responseData.stringifyJSON())
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
                0 -> expect(Dummy1("one", 1)) { it }
                1 -> expect(Dummy1("two", 2)) { it }
                2 -> expect(Dummy1("three", 3)) { it }
            }
        }
    }

}
