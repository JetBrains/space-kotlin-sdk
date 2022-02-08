package space.jetbrains.api.runtime

import io.ktor.http.*

public class Batch<out T>(public val next: String, public val totalCount: Int?, public val data: List<T>)

public abstract class RestResource(private val client: SpaceClient) {
    protected suspend fun callWithBody(
        functionName: String,
        path: String,
        method: HttpMethod,
        requestBody: JsonValue? = null,
        partial: PartialBuilder.Explicit? = null
    ): DeserializationContext = callSpaceApi(
        ktorClient = client.ktorClient,
        functionName = functionName,
        appInstance = client.appInstance,
        auth = client.auth,
        callMethod = method,
        path = path,
        partial = partial,
        requestBody = requestBody,
    )

    protected suspend fun callWithParameters(
        functionName: String,
        path: String,
        method: HttpMethod,
        parameters: Parameters = Parameters.Empty,
        partial: PartialBuilder.Explicit? = null
    ): DeserializationContext = callSpaceApi(
        ktorClient = client.ktorClient,
        functionName = functionName,
        appInstance = client.appInstance,
        auth = client.auth,
        callMethod = method,
        path = path,
        partial = partial,
        parameters = parameters,
    )

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
