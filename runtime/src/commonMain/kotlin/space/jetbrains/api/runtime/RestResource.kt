package space.jetbrains.api.runtime

import io.ktor.http.*

class Batch<out T>(val next: String, val totalCount: Int?, val data: List<T>)

abstract class RestResource(private val client: SpaceHttpClientWithCallContext) {
    protected suspend fun callWithBody(
        functionName: String,
        path: String,
        method: HttpMethod,
        requestBody: JsonValue? = null,
        partial: Partial<*>? = null
    ): DeserializationContext<*> {
        return client.client.call(functionName, client.callContext, method, path, partial, requestBody = requestBody)
    }

    protected suspend fun callWithParameters(
        functionName: String,
        path: String,
        method: HttpMethod,
        parameters: List<Pair<String, String>> = emptyList(),
        partial: Partial<*>? = null
    ): DeserializationContext<*> {
        return client.client.call(functionName, client.callContext, method, path, partial, parameters)
    }

    protected fun BatchInfo?.toParams(): List<Pair<String, String>> = listOfNotNull(
        this?.let { "\$top" to it.batchSize.toString() },
        this?.offset?.let { "\$skip" to it }
    )

    protected fun pathParam(value: Any): String = value.toString().encodeURLParameter()
}
