package space.jetbrains.api.runtime

import io.ktor.http.*
import space.jetbrains.api.runtime.epoch.EpochTracker
import space.jetbrains.api.runtime.epoch.SYNC_EPOCH_HEADER_NAME

public class Batch<out T>(public val next: String, public val totalCount: Int?, public val data: List<T>)
public class SyncBatch<out T>(public val etag: String, public val data: List<T>, public val hasMore: Boolean)

public abstract class RestResource(private val client: SpaceClient) {
    protected suspend fun callWithBody(
        functionName: String,
        path: String,
        method: HttpMethod,
        requestBody: JsonValue? = null,
        requestHeaders: List<Pair<String, String>>? = null,
        partial: PartialBuilder.Explicit? = null,
    ): DeserializationContext = callSpaceApi(
        ktorClient = client.ktorClient,
        functionName = functionName,
        appInstance = client.appInstance,
        auth = client.auth,
        callMethod = method,
        path = path,
        partial = partial,
        requestBody = requestBody,
        requestHeaders = requestHeaders,
    )

    protected suspend fun callWithParameters(
        functionName: String,
        path: String,
        method: HttpMethod,
        parameters: Parameters = Parameters.Empty,
        requestHeaders: List<Pair<String, String>>? = null,
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
        requestHeaders = requestHeaders,
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

    protected suspend fun getSyncEpochHeader(): Pair<String, String>? {
        val syncEpoch = EpochTracker.getSyncEpoch(client.server.serverUrl) ?: return null
        return SYNC_EPOCH_HEADER_NAME to syncEpoch.toString()
    }
}
