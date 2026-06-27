package interceptor

import model.KraftHttpClientEvent
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import util.PulseContextHolder
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.util.UUID

class KraftHttpClientInterceptor(
    private val captor: PulseTelemetryCaptor
) : ClientHttpRequestInterceptor {

    @Throws(IOException::class)
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val startTime = System.currentTimeMillis()
        val currentTraceId = PulseContextHolder.get()?.traceId ?: "outbound-standalone"

        var statusCode = 0
        var errorMessage: String? = null
//
//        var statusCode = 0
//        var errorMessage: String? = null
        var responseSize = 0L
        val timeoutAttr = request.headers.getFirst("X-Kraft-Connect-Timeout")?.toLongOrNull()

        try {
            val response = execution.execute(request, body)
            val wrappedResponse = ResizedClientHttpResponse(response)

            statusCode = wrappedResponse.statusCode.value()
            // We return the wrapper so the caller can consume the body
            return wrappedResponse
        } catch (ex: Throwable) {
            // Determine if it is a true network timeout
            if (ex is SocketTimeoutException || ex.cause is SocketTimeoutException) {
                statusCode = 408 // Request Timeout
                errorMessage = "Connection or Read Timeout: ${ex.message}"
            } else {
                statusCode = 500
                errorMessage = ex.message
            }
            throw ex
        } finally {
            val duration = System.currentTimeMillis() - startTime

            // Prevent recursive loops
            if (!request.uri.path.contains("/api/telemetry/ingest")) {
                captor.recordOutboundHttp(
                    KraftHttpClientEvent(
                        id = UUID.randomUUID().toString(),
                        traceId = currentTraceId,
                        host = request.uri.host ?: "unknown-host",
                        url = request.uri.toString(),
                        method = request.method.name(),
                        statusCode = statusCode,
                        durationMs = duration,
                        responseBodySize = responseSize, // Requires body stream wrapping for real byte count
                        connectionTimeoutMs = timeoutAttr,
                        errorMessage = errorMessage,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}

class CountingInputStream(inputStream: InputStream) : FilterInputStream(inputStream) {
    var bytesRead: Long = 0
        private set

    override fun read(): Int {
        val b = super.read()
        if (b != -1) bytesRead++
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = super.read(b, off, len)
        if (read != -1) bytesRead += read
        return read
    }
}

class ResizedClientHttpResponse(private val delegate: ClientHttpResponse) : ClientHttpResponse by delegate {
    private var countingStream: CountingInputStream? = null

    override fun getBody(): InputStream {
        if (countingStream == null) {
            countingStream = CountingInputStream(delegate.body)
        }
        return countingStream!!
    }

    val bytesRead: Long get() = countingStream?.bytesRead ?: 0L
}