package com.twig.dreamzversion3.drive

import com.twig.dreamzversion3.data.dream.DreamRepository
import com.twig.dreamzversion3.model.dream.Dream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

class DriveSyncManager(
    private val repository: DreamRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun sync(token: String): DriveSyncResult = withContext(ioDispatcher) {
        val dreams = repository.getDreams()
        if (dreams.isEmpty()) {
            return@withContext DriveSyncResult.Empty
        }
        val folderId = ensureDreamZFolder(token)
        dreams.forEach { dream ->
            val fileName = buildFileName(dream)
            val bytes = createDocxForDream(dream)
            upsertBinaryFile(
                token = token,
                parentId = folderId,
                name = fileName,
                mimeType = DOCX_MIME,
                data = bytes
            )
        }
        DriveSyncResult.Success(dreams.size)
    }

    fun buildPreview(): List<DriveDocumentPreview> {
        return repository.getDreams().map { dream ->
            DriveDocumentPreview(
                fileName = buildFileName(dream),
                summary = dream.description.take(120)
            )
        }
    }

    private fun buildFileName(dream: Dream): String {
        val datePart = formatDate(dream.createdAt)
        val sanitizedTitle = sanitizeFileName(dream.title.ifBlank { "Untitled Dream" })
        return "$datePart-$sanitizedTitle.docx"
    }
}

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault()
}

private fun sanitizeFileName(value: String): String {
    val cleaned = value.replace(Regex("[\\\\/:*?\"<>|]"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
    return cleaned.ifEmpty { "Dream" }
}

private fun createDocxForDream(dream: Dream): ByteArray {
    val documentXml = buildDocumentXml(dream)
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        zip.write(CONTENT_TYPES_XML.toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("_rels/.rels"))
        zip.write(RELATIONSHIPS_XML.toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("word/document.xml"))
        zip.write(documentXml.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
    return out.toByteArray()
}

private fun buildDocumentXml(dream: Dream): String {
    val tagsLine = if (dream.tags.isEmpty()) "None" else dream.tags.joinToString(", ")
    val lucid = if (dream.isLucid) "Yes" else "No"
    val recurring = if (dream.isRecurring) "Yes" else "No"
    val createdText = formatDateTime(dream.createdAt)
    val updatedText = dream.updatedAt?.let { formatDateTime(it) }
    return buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">")
        append("<w:body>")
        append(paragraph(dream.title.ifBlank { "Untitled Dream" }, bold = true, size = 36))
        append(paragraph("Captured: $createdText"))
        if (updatedText != null) {
            append(paragraph("Last edited: $updatedText"))
        }
        append(paragraph("Mood: ${dream.mood.ifBlank { "Not captured" }}"))
        append(paragraph("Lucid dream: $lucid"))
        append(paragraph("Recurring dream: $recurring"))
        append(paragraph("Intensity: ${dream.intensity.toInt()} / 10"))
        append(paragraph("Emotion: ${dream.emotion.toInt()} / 10"))
        append(paragraph("Tags: $tagsLine"))
        append(paragraph("Description:", bold = true))
        if (dream.description.isBlank()) {
            append(paragraph("(No description provided)"))
        } else {
            dream.description.split("\n\n").forEach { block ->
                append(paragraph(block.trim()))
            }
        }
        append("</w:body></w:document>")
    }
}

private fun paragraph(text: String, bold: Boolean = false, size: Int = 24): String {
    val open = if (bold) "<w:r><w:rPr><w:b/><w:sz w:val=\"$size\"/></w:rPr><w:t>" else "<w:r><w:rPr><w:sz w:val=\"$size\"/></w:rPr><w:t>"
    val close = "</w:t></w:r>"
    return "<w:p>$open${escapeXml(text)}$close</w:p>"
}

private fun escapeXml(value: String): String {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

private val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
</Types>"""

private val RELATIONSHIPS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="R1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

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
