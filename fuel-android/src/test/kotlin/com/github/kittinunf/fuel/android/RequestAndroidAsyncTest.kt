package com.github.kittinunf.fuel.android

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.core.Json
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Handler
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.ResponseDeserializable
import org.hamcrest.CoreMatchers.*
import org.json.JSONObject
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import org.hamcrest.CoreMatchers.`is` as isEqualTo

class RequestAndroidAsyncTest : BaseTestCase() {

    init {
        FuelManager.instance.apply {
            basePath = "http://httpbin.org"
            baseHeaders = mapOf("foo" to "bar")
            baseParams = listOf("key" to "value")
            callbackExecutor = Executor(Runnable::run)
        }
    }

    //Model
    data class HttpBinHeadersModel(var headers: Map<String, String> = mutableMapOf())

    //Deserializer
    class HttpBinHeadersDeserializer : ResponseDeserializable<HttpBinHeadersModel> {

        override fun deserialize(content: String): HttpBinHeadersModel {
            val json = JSONObject(content)
            val headers = json.getJSONObject("headers")
            val results = headers.keys().asSequence().associate { Pair(it, headers.getString(it)) }
            val model = HttpBinHeadersModel()
            model.headers = results
            return model
        }

    }

    @Before
    fun setUp() {
        lock = CountDownLatch(1)
    }

    @Test
    fun httpGetRequestString() {
        var request: Request? = null
        var response: Response? = null
        var data: Any? = null
        var error: FuelError? = null

        Fuel.get("/user-agent").responseString { req, res, result ->
            val (d, e) = result
            data = d
            error = e

            request = req
            response = res

            lock.countDown()
        }

        await()

        assertThat(request, notNullValue())
        assertThat(response, notNullValue())
        assertThat(error, nullValue())
        assertThat(data, notNullValue())
        assertThat(data as String, isA(String::class.java))

        val statusCode = HttpURLConnection.HTTP_OK
        assertThat(response?.statusCode, isEqualTo(statusCode))
    }

    @Test
    fun httpGetRequestJsonValid() {
        var request: Request? = null
        var response: Response? = null
        var data: Any? = null
        var error: FuelError? = null

        Fuel.get("/user-agent").responseJson { req, res, result ->
            val (d, e) = result
            data = d
            error = e

            request = req
            response = res

            lock.countDown()
        }

        await()

        assertThat(request, notNullValue())
        assertThat(response, notNullValue())
        assertThat(error, nullValue())
        assertThat(data, notNullValue())
        assertThat(data as Json, isA(Json::class.java))
        assertThat((data as Json).obj(), isA(JSONObject::class.java))

        val statusCode = HttpURLConnection.HTTP_OK
        assertThat(response?.statusCode, isEqualTo(statusCode))
    }

    @Test
    fun httpGetRequestJsonHandlerValid() {
        var req: Request? = null
        var res: Response? = null
        var data: Any? = null
        var err: FuelError? = null

        Fuel.get("/user-agent").responseJson(object : Handler<Json> {
            override fun success(request: Request, response: Response, value: Json) {
                req = request
                res = response
                data = value

                lock.countDown()
            }

            override fun failure(request: Request, response: Response, error: FuelError) {
                err = error
            }
        })

        await()

        assertThat(req, notNullValue())
        assertThat(res, notNullValue())
        assertThat(err, nullValue())
        assertThat(data, notNullValue())
        assertThat(data as Json, isA(Json::class.java))
        assertThat((data as Json).obj(), isA(JSONObject::class.java))

        val statusCode = HttpURLConnection.HTTP_OK
        assertThat(res?.statusCode, isEqualTo(statusCode))
    }

    @Test
    fun httpGetRequestJsonInvalid() {
        var request: Request? = null
        var response: Response? = null
        var data: Any? = null
        var error: FuelError? = null

        Fuel.get("/404").responseJson { req, res, result ->
            val (d, e) = result
            data = d
            error = e

            request = req
            response = res

            lock.countDown()
        }

        await()

        assertThat(request, notNullValue())
        assertThat(response, notNullValue())
        assertThat(error, notNullValue())
        assertThat(data, nullValue())

        val statusCode = HttpURLConnection.HTTP_NOT_FOUND
        assertThat(response?.statusCode, isEqualTo(statusCode))
    }

    @Test
    fun httpGetRequestJsonHandlerInvalid() {
        var req: Request? = null
        var res: Response? = null
        var data: Any? = null
        var err: FuelError? = null

        Fuel.get("/404").responseJson(object : Handler<Json> {
            override fun success(request: Request, response: Response, value: Json) {
                data = value
            }

            override fun failure(request: Request, response: Response, error: FuelError) {
                req = request
                res = response
                err = error

                lock.countDown()
            }
        })

        await()

        assertThat(req, notNullValue())
        assertThat(res, notNullValue())
        assertThat(err, notNullValue())
        assertThat(data, nullValue())

        val statusCode = HttpURLConnection.HTTP_NOT_FOUND
        assertThat(res?.statusCode, isEqualTo(statusCode))
    }

    @Test
    fun httpGetRequestObject() {
        var request: Request? = null
        var response: Response? = null
        var data: Any? = null
        var error: FuelError? = null

        Fuel.get("/headers").responseObject(HttpBinHeadersDeserializer()) { req, res, result ->
            val (d, e) = result
            request = req
            response = res
            data = d
            error = e

            lock.countDown()
        }

        await()

        assertThat(request, notNullValue())
        assertThat(response, notNullValue())
        assertThat(error, nullValue())
        assertThat(data, notNullValue())
        assertThat(data as HttpBinHeadersModel, isA(HttpBinHeadersModel::class.java))
        assertThat((data as HttpBinHeadersModel).headers.isNotEmpty(), isEqualTo(true))

        val statusCode = HttpURLConnection.HTTP_OK
        assertThat(response?.statusCode, isEqualTo(statusCode))
    }

    @Test
    fun httpGetRequestHandlerObject() {
        var req: Request? = null
        var res: Response? = null
        var data: Any? = null
        var err: FuelError? = null

        Fuel.get("/headers").responseObject(HttpBinHeadersDeserializer(), object : Handler<HttpBinHeadersModel> {

            override fun success(request: Request, response: Response, value: HttpBinHeadersModel) {
                req = request
                res = response
                data = value

                lock.countDown()
            }

            override fun failure(request: Request, response: Response, error: FuelError) {
                err = error
            }

        })

        await()

        assertThat(req, notNullValue())
        assertThat(res, notNullValue())
        assertThat(err, nullValue())
        assertThat(data, notNullValue())
        assertThat(data as HttpBinHeadersModel, isA(HttpBinHeadersModel::class.java))
        assertThat((data as HttpBinHeadersModel).headers.isNotEmpty(), isEqualTo(true))

        val statusCode = HttpURLConnection.HTTP_OK
        assertThat(res?.statusCode, isEqualTo(statusCode))
    }

}