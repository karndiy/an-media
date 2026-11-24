package com.anmedia.player.domain.youtube

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class NewPipeDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) : Downloader() {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

        @Volatile
        private var instance: NewPipeDownloader? = null

        fun getInstance(): NewPipeDownloader =
            instance ?: synchronized(this) {
                instance ?: NewPipeDownloader().also { instance = it }
            }
    }

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = OkRequest.Builder().url(url)

        val body = if (dataToSend != null) {
            val contentType = headers["Content-Type"]?.firstOrNull()?.toMediaTypeOrNull()
            dataToSend.toRequestBody(contentType)
        } else null

        requestBuilder.method(httpMethod, body)
        requestBuilder.header("User-Agent", USER_AGENT)

        for ((key, values) in headers) {
            if (values.size > 1) {
                requestBuilder.removeHeader(key)
                for (value in values) {
                    requestBuilder.addHeader(key, value)
                }
            } else if (values.size == 1) {
                requestBuilder.header(key, values[0])
            }
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 429) {
                throw IOException("NewPipe: rate limited (HTTP 429) for $url")
            }

            val bodyString = response.body?.string() ?: ""
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                bodyString,
                response.request.url.toString()
            )
        }
    }
}
