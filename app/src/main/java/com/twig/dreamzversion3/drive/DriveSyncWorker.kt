package com.twig.dreamzversion3.drive

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.twig.dreamzversion3.data.AppDb
import com.twig.dreamzversion3.data.DreamRepo

class DriveSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing token"))

        val repo = DreamRepo(AppDb.get(applicationContext).dreamDao())

        return try {
            val entries = repo.getAll()
            setProgress(workDataOf(KEY_STATUS to "Preparing Google Drive…"))
            val folderId = ensureDreamZFolder(token)

            if (entries.isEmpty()) {
                setProgress(workDataOf(KEY_STATUS to "Nothing to sync"))
                Result.success(workDataOf(KEY_STATUS to "Nothing to sync"))
            } else {
                val total = entries.size
                entries.forEachIndexed { index, entry ->
                    setProgress(
                        workDataOf(
                            KEY_STATUS to "Uploading ${index + 1} of $total…",
                            KEY_STEP to index + 1,
                            KEY_TOTAL to total
                        )
                    )
                    upsertJsonFile(
                        token = token,
                        parentId = folderId,
                        name = "entries_${entry.id}.json",
                        jsonContent = entryToJson(entry)
                    )
                }
                setProgress(workDataOf(KEY_STATUS to "Sync complete"))
                Result.success(workDataOf(KEY_STATUS to "Sync complete"))
            }
        } catch (t: Throwable) {
            Result.failure(
                workDataOf(
                    KEY_ERROR to (t.message ?: t::class.java.simpleName ?: "Sync error")
                )
            )
        }
    }

    companion object {
        const val KEY_TOKEN = "token"
        const val KEY_STATUS = "status"
        const val KEY_ERROR = "error"
        const val KEY_STEP = "step"
        const val KEY_TOTAL = "total"
        const val WORK_NAME = "drive_sync"
        const val PERIODIC_WORK_NAME = "drive_sync_periodic"
    }
}

private fun entryToJson(e: com.twig.dreamzversion3.data.DreamEntry): String = """
{
  "id": ${esc(e.id)},
  "title": ${esc(e.title)},
  "body": ${esc(e.body)},
  "mood": ${esc(e.mood)},
  "lucid": ${e.lucid},
  "tags": ${listToJson(e.tags)},
  "intensityRating": ${e.intensityRating},
  "emotionRating": ${e.emotionRating},
  "lucidityRating": ${e.lucidityRating},
  "createdAt": ${e.createdAt},
  "editedAt": ${e.editedAt}
}
""".trimIndent()

private fun esc(s: String?): String =
    if (s == null) "null" else "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun listToJson(items: List<String>): String =
    items.joinToString(prefix = "[", postfix = "]") { esc(it) }
