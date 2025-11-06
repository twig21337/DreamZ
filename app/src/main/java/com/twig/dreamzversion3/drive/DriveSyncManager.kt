package com.twig.dreamzversion3.drive

import com.twig.dreamzversion3.data.dream.DreamRepository
import com.twig.dreamzversion3.model.dream.Dream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DriveSyncManager(
    private val repository: DreamRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Push all local dreams to Drive as Google Docs.
     * Each doc will be named: yyyy-MM-dd - Dream title
     */
    suspend fun sync(token: String): DriveSyncResult = withContext(ioDispatcher) {
        val dreams = repository.getDreams()
        if (dreams.isEmpty()) {
            return@withContext DriveSyncResult.Empty
        }

        // make sure the folder exists
        val folderId = ensureDreamZFolder(token)

        dreams.forEach { dream ->
            val datePart = formatDate(dream.createdAt)
            val titlePart = if (dream.title.isBlank()) "Untitled Dream" else dream.title
            val body = buildDocBody(dream)

            // this creates or updates the Google Doc
            upsertDreamAsGoogleDoc(
                token = token,
                folderId = folderId,
                date = datePart,
                title = titlePart,
                body = body
            )
        }

        DriveSyncResult.Success(dreams.size)
    }

    /**
     * Used by settings to show "what will be synced".
     */
    fun buildPreview(): List<DriveDocumentPreview> {
        return repository.getDreams().map { dream ->
            val datePart = formatDate(dream.createdAt)
            val titlePart = if (dream.title.isBlank()) "Untitled Dream" else dream.title
            DriveDocumentPreview(
                fileName = "$datePart - $titlePart",
                summary = dream.description.take(120)
            )
        }
    }

    private fun buildDocBody(dream: Dream): String {
        val tagsLine = if (dream.tags.isEmpty()) "None" else dream.tags.joinToString(", ")
        val lucid = if (dream.isLucid) "Yes" else "No"
        val recurring = if (dream.isRecurring) "Yes" else "No"
        val createdText = formatDateTime(dream.createdAt)
        val updatedText = dream.updatedAt?.let { formatDateTime(it) }
        return buildString {
            appendLine(createdText)
            appendLine(if (dream.title.isBlank()) "Untitled Dream" else dream.title)
            appendLine()
            appendLine("Mood: ${dream.mood.ifBlank { "Not captured" }}")
            appendLine("Lucid dream: $lucid")
            appendLine("Recurring dream: $recurring")
            appendLine("Intensity: ${dream.intensity.toInt()} / 10")
            appendLine("Emotion: ${dream.emotion.toInt()} / 10")
            appendLine("Tags: $tagsLine")
            appendLine()
            appendLine("Description:")
            appendLine(if (dream.description.isBlank()) "(No description provided)" else dream.description)
            if (updatedText != null) {
                appendLine()
                appendLine("Last edited: $updatedText")
            }
        }
    }
}

/** same formatting helpers you already had */
private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault()
}

private val dateTimeFormatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault()
}

private fun formatDate(millis: Long): String = synchronized(dateFormatter) {
    dateFormatter.format(Date(millis))
}

private fun formatDateTime(millis: Long): String = synchronized(dateTimeFormatter) {
    dateTimeFormatter.format(Date(millis))
}

sealed class DriveSyncResult {
    data class Success(val count: Int) : DriveSyncResult()
    object Empty : DriveSyncResult()
}

data class DriveDocumentPreview(
    val fileName: String,
    val summary: String
)
