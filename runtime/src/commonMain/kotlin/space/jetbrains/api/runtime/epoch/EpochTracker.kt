package space.jetbrains.api.runtime.epoch

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public object EpochTracker {
    private val mutex = Mutex()
    private val epochPerHost = mutableMapOf<String, Long>()
    private val syncEpochPerHost = mutableMapOf<String, Long>()

    public suspend fun getEpoch(serverUrl: String): Long? = mutex.withLock {
        epochPerHost[serverUrl]
    }

    public suspend fun updateEpoch(serverUrl: String, epochValue: String) {
        updateEpochImpl(serverUrl, epochValue, epochPerHost)
    }

    public suspend fun getSyncEpoch(serverUrl: String): Long? = mutex.withLock {
        syncEpochPerHost[serverUrl]
    }

    public suspend fun updateSyncEpoch(serverUrl: String, syncEpochValue: String) {
        updateEpochImpl(serverUrl, syncEpochValue, syncEpochPerHost)
    }

    private suspend fun updateEpochImpl(serverUrl: String, epochValue: String, epochMap: MutableMap<String, Long>) {
        val newEpochFromSpace = epochValue.toLongOrNull() ?: return
        mutex.withLock {
            val oldEpoch = epochMap[serverUrl] ?: Long.MIN_VALUE
            epochMap[serverUrl] = maxOf(oldEpoch, newEpochFromSpace)
        }
    }
}
