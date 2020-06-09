package space.jetbrains.api.runtime

import io.ktor.http.*

class Batch<out T>(val next: String, val totalCount: Int?, val data: List<T>)

abstract class RestResource(private val client: SpaceHttpClientWithCallContext) {
    protected suspend fun callWithBody(
        functionName: String,
        path: String,
        method: HttpMethod,
        requestBody: JsonValue? = null,
        partial: PartialBuilder? = null
    ): DeserializationContext {
        return client.client.call(functionName, client.callContext, method, path, partial, requestBody = requestBody)
    }

    protected suspend fun callWithParameters(
        functionName: String,
        path: String,
        method: HttpMethod,
        parameters: Parameters = Parameters.Empty,
        partial: PartialBuilder? = null
    ): DeserializationContext {
        return client.client.call(functionName, client.callContext, method, path, partial, parameters)
    }

    protected fun ParametersBuilder.appendBatchInfo(batchInfo: BatchInfo?) {
        batchInfo?.let {
            append("\$top", it.batchSize.toString())
        }
        batchInfo?.offset?.let {
            append("\$skip", it)
        }
    }

    protected fun pathParam(value: Any): String = value.toString().encodeURLParameter()
}
