package com.twig.dreamzversion3.drive

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.driveSyncStateDataStore by preferencesDataStore(name = "drive_sync_state")

private val KEY_DRIVE_SYNC_STATES = stringPreferencesKey("drive_sync_states")

@Serializable
data class DriveSyncState(
    val dreamId: String,
    val driveFileId: String? = null,
    val lastSyncedAt: Long
)

class DriveSyncStateRepository(
    context: Context,
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
) {
    private val dataStore = context.applicationContext.driveSyncStateDataStore
    private val listSerializer = ListSerializer(DriveSyncState.serializer())
    private val writeMutex = Mutex()

    suspend fun getStates(): Map<String, DriveSyncState> {
        val prefs = dataStore.data.first()
        return decodeToMutableMap(prefs[KEY_DRIVE_SYNC_STATES]).toMap()
    }

    suspend fun upsert(state: DriveSyncState) {
        writeMutex.withLock {
            dataStore.edit { prefs ->
                val current = decodeToMutableMap(prefs[KEY_DRIVE_SYNC_STATES])
                current[state.dreamId] = state
                if (current.isEmpty()) {
                    prefs.remove(KEY_DRIVE_SYNC_STATES)
                } else {
                    prefs[KEY_DRIVE_SYNC_STATES] =
                        json.encodeToString(listSerializer, current.values.toList())
                }
            }
        }
    }

    suspend fun removeAllExcept(validDreamIds: Set<String>) {
        writeMutex.withLock {
            dataStore.edit { prefs ->
                val current = decodeToMutableMap(prefs[KEY_DRIVE_SYNC_STATES])
                val filtered = current.filterKeys { it in validDreamIds }
                if (filtered.isEmpty()) {
                    prefs.remove(KEY_DRIVE_SYNC_STATES)
                } else {
                    prefs[KEY_DRIVE_SYNC_STATES] =
                        json.encodeToString(listSerializer, filtered.values.toList())
                }
            }
        }
    }

    private fun decodeToMutableMap(serialized: String?): MutableMap<String, DriveSyncState> {
        if (serialized.isNullOrBlank()) return mutableMapOf()
        return runCatching {
            json.decodeFromString(listSerializer, serialized)
                .associateByTo(mutableMapOf()) { it.dreamId }
        }.getOrDefault(mutableMapOf())
    }
}
