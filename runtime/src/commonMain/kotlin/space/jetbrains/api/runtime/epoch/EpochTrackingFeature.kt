package space.jetbrains.api.runtime.epoch

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val EPOCH_HEADER_NAME = "epoch"

public class EpochTrackingFeature {
    public class Configuration

    private val mutex = Mutex()
    private val epochPerHost = mutableMapOf<String, Long>()

    public companion object Feature : HttpClientFeature<Configuration, EpochTrackingFeature> {
        override val key: AttributeKey<EpochTrackingFeature> = AttributeKey("EpochTrackingFeature")

        override fun install(feature: EpochTrackingFeature, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
                val maxKnownEpoch = feature.mutex.withLock {
                    feature.epochPerHost[context.url.host]
                }
                if (maxKnownEpoch != null) {
                    context.header(EPOCH_HEADER_NAME, maxKnownEpoch)
                }
            }

            scope.receivePipeline.intercept(HttpReceivePipeline.After) {
                context.response.headers[EPOCH_HEADER_NAME]?.toLongOrNull()?.let {
                    feature.mutex.withLock {
                        feature.epochPerHost[context.request.url.host] = maxOf(
                            feature.epochPerHost[context.request.url.host] ?: Long.MIN_VALUE,
                            it
                        )
                    }
                }
            }
        }

        override fun prepare(block: Configuration.() -> Unit): EpochTrackingFeature {
            return EpochTrackingFeature()
        }
    }
}