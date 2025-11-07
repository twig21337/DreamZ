package com.twig.dreamzversion3.data.dream

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.twig.dreamzversion3.model.dream.Dream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dreamRepositoryDataStore by preferencesDataStore(name = "dream_repository")

private val DREAMS_KEY = stringPreferencesKey("dreams")

class DataStoreDreamRepository(
    context: Context,
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : DreamRepository {

    private val dataStore = context.applicationContext.dreamRepositoryDataStore
    private val writeMutex = Mutex()
    private val listSerializer = ListSerializer(Dream.serializer())

    private val _dreams = MutableStateFlow<List<Dream>>(emptyList())
    override val dreams: StateFlow<List<Dream>> = _dreams.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .map { prefs -> prefs[DREAMS_KEY] }
                .map(::decodeDreams)
                .collect { storedDreams ->
                    _dreams.value = storedDreams
                }
        }
    }

    override fun addDream(dream: Dream) {
        scope.launch {
            persist { current ->
                (current + dream).sortedByDescending { it.createdAt }
            }
        }
    }

    override fun updateDream(dream: Dream) {
        scope.launch {
            persist { current ->
                val updated = current.map { existing ->
                    if (existing.id == dream.id) dream else existing
                }
                if (updated.any { it.id == dream.id }) {
                    updated
                } else {
                    (updated + dream).sortedByDescending { it.createdAt }
                }
            }
        }
    }

    override fun deleteDream(id: String) {
        scope.launch {
            persist { current -> current.filterNot { it.id == id } }
        }
    }

    override fun getDream(id: String): Dream? = dreams.value.firstOrNull { it.id == id }

    override fun getDreams(): List<Dream> = dreams.value

    private suspend fun persist(transform: (List<Dream>) -> List<Dream>) {
        writeMutex.withLock {
            val updated = transform(_dreams.value)
            dataStore.edit { prefs ->
                if (updated.isEmpty()) {
                    prefs.remove(DREAMS_KEY)
                } else {
                    prefs[DREAMS_KEY] = json.encodeToString(listSerializer, updated)
                }
            }
            _dreams.value = updated
        }
    }

    private fun decodeDreams(serialized: String?): List<Dream> {
        if (serialized.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(listSerializer, serialized)
        }.getOrDefault(emptyList())
    }
}
