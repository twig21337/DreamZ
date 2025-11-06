package com.twig.dreamzversion3.drive

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.twig.dreamzversion3.data.AppDb
import com.twig.dreamzversion3.data.DreamEntry
import com.twig.dreamzversion3.data.DreamRepo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

                    val dateStr = formatDate(entry.createdAt)
                    val title = (entry.title ?: "").ifBlank { "Untitled Dream" }
                    val body = buildDriveDocBody(entry)

                    upsertDreamAsGoogleDoc(
                        token = token,
                        folderId = folderId,
                        date = dateStr,
                        title = title,
                        body = body
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

// ---------------- helpers ----------------

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault()
}

private fun formatDate(millis: Long): String =
    dateFormatter.format(Date(millis))

/**
 * Turn your DB dream into readable Google Doc text, but treat every String as nullable.
 */
private fun buildDriveDocBody(e: DreamEntry): String {
    val title = (e.title ?: "").ifBlank { "Untitled Dream" }
    val mood = (e.mood ?: "").ifBlank { "Not captured" }
    val lucid = e.lucid?.toString() ?: "false"
    val intensity = e.intensityRating?.toString() ?: "-"
    val emotion = e.emotionRating?.toString() ?: "-"
    val lucidity = e.lucidityRating?.toString() ?: "-"
    val tagsList = e.tags ?: emptyList()
    val tagsLine = if (tagsList.isEmpty()) "None" else tagsList.joinToString(", ")
    val bodyText = (e.body ?: "").ifBlank { "(No description provided)" }

    return buildString {
        appendLine(formatDate(e.createdAt))
        appendLine(title)
        appendLine()
        appendLine("Mood: $mood")
        appendLine("Lucid: $lucid")
        appendLine("Intensity: $intensity")
        appendLine("Emotion: $emotion")
        appendLine("Lucidity: $lucidity")
        appendLine("Tags: $tagsLine")
        appendLine()
        appendLine("Description:")
        appendLine(bodyText)
        if ((e.editedAt ?: 0L) != 0L) {
            appendLine()
            appendLine("Last edited (local): ${Date(e.editedAt!!)}")
        }
        appendLine()
        appendLine("Entry id: ${e.id}")
    }
}
