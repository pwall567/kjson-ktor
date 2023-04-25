/*
 * @(#) JSONKtorClientReceiveTest.kt
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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

import io.kjson.ktor.test.Dummy1

class JSONKtorClientReceiveTest {

    @Test fun `should serialise client request`() = runBlocking {
        val mockEngine = MockEngine {
            expect("""{"a":"alpha","b":1}""") { String(it.body.toByteArray()) }
            respond("OK", HttpStatusCode.OK, contentTypeJSON(Charsets.UTF_8))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                kjson()
            }
        }
        val response: String = httpClient.put("/anything") {
            contentType(ContentType.Application.Json)
            setBody(Dummy1("alpha", 1))
        }.body()
        expect("OK") { response }
    }

}
