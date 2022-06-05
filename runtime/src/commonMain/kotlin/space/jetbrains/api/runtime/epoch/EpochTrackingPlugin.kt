package space.jetbrains.api.runtime.epoch

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*

public const val EPOCH_HEADER_NAME: String = "epoch"
public const val SYNC_EPOCH_HEADER_NAME: String = "X-Space-Sync-Epoch"

public class EpochTrackingPlugin {
    public class Configuration

    public companion object Plugin : HttpClientPlugin<Configuration, EpochTrackingPlugin> {
        override val key: AttributeKey<EpochTrackingPlugin> = AttributeKey("EpochTrackingPlugin")

        override fun install(plugin: EpochTrackingPlugin, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
                val host = context.url.host
                EpochTracker.getEpoch(host)?.let { epoch ->
                    context.header(EPOCH_HEADER_NAME, epoch)
                }
            }

            scope.receivePipeline.intercept(HttpReceivePipeline.After) { response ->
                response.headers[EPOCH_HEADER_NAME]?.let { newEpochFromSpace ->
                    EpochTracker.updateEpoch(response.call.request.url.host, newEpochFromSpace)
                }
            }
        }

        override fun prepare(block: Configuration.() -> Unit): EpochTrackingPlugin {
            return EpochTrackingPlugin()
        }
    }
}
