package com.twig.dreamzversion3.drive

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

private val client = OkHttpClient()

private const val DRIVE_BASE = "https://www.googleapis.com/drive/v3"
private const val DOCS_BASE = "https://docs.googleapis.com/v1"
private const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3/files"

private fun Request.Builder.auth(token: String) =
    header("Authorization", "Bearer $token")

/**
 * Ensures there is a folder called "DreamZ" and returns its file id.
 */
fun ensureDreamZFolder(token: String): String {
    val q = "mimeType='application/vnd.google-apps.folder' and name='DreamZ' and trashed=false"
    val listReq = Request.Builder()
        .url("$DRIVE_BASE/files?q=${URLEncoder.encode(q, "UTF-8")}&fields=files(id,name)")
        .auth(token)
        .build()

    client.newCall(listReq).execute().use { resp ->
        if (!resp.isSuccessful) error("Drive list failed: ${resp.code}")
        val body = resp.body?.string().orEmpty()
        // find existing
        val match = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)
        if (match != null) return match.groupValues[1]
    }

    // create it
    val json = """{"name":"DreamZ","mimeType":"application/vnd.google-apps.folder"}"""
    val createReq = Request.Builder()
        .url("$DRIVE_BASE/files?fields=id")
        .auth(token)
        .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    client.newCall(createReq).execute().use { resp ->
        if (!resp.isSuccessful) error("Create folder failed: ${resp.code}")
        val body = resp.body?.string().orEmpty()
        val idMatch = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)
        return idMatch?.groupValues?.get(1) ?: error("No id from create folder")
    }
}

/**
 * Build the exact doc name we want:
 *   2025-11-05 - Flying over the city
 */
fun dreamDocName(dreamDate: String, dreamTitle: String): String {
    return "$dreamDate - $dreamTitle"
}

/**
 * Create an empty Google Doc with the given name in the given parent folder.
 */
private fun createGoogleDoc(token: String, parentId: String, name: String): String {
    val json = """
        {
          "name": "$name",
          "mimeType": "application/vnd.google-apps.document",
          "parents": ["$parentId"]
        }
    """.trimIndent()

    val req = Request.Builder()
        .url("$DRIVE_BASE/files?fields=id")
        .auth(token)
        .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) error("Create doc failed: ${resp.code}")
        val body = resp.body?.string().orEmpty()
        val idMatch = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)
        return idMatch?.groupValues?.get(1) ?: error("No doc id from create")
    }
}

/**
 * Find an existing Google Doc in the folder with this name.
 */
private fun findGoogleDoc(
    token: String,
    parentId: String,
    name: String
): String? {
    // name + parent + mimeType
    val q =
        "name='${name.replace("'", "\\'")}' and '${parentId}' in parents and mimeType='application/vnd.google-apps.document' and trashed=false"
    val req = Request.Builder()
        .url("$DRIVE_BASE/files?q=${URLEncoder.encode(q, "UTF-8")}&fields=files(id,name,modifiedTime)")
        .auth(token)
        .build()

    client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) error("Find doc failed: ${resp.code}")
        val body = resp.body?.string().orEmpty()
        val m = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)
        return m?.groupValues?.get(1)
    }
}

/**
 * Replace the FULL content of a Google Doc with our dream text using Docs API.
 *
 * Docs API wants:
 *  POST https://docs.googleapis.com/v1/documents/{documentId}:batchUpdate
 */
private fun replaceDocContent(
    token: String,
    documentId: String,
    text: String
) {
    // 1) delete all existing content (except the structural end)
    // 2) insert new text
    val body = """
        {
          "requests": [
            {
              "deleteContentRange": {
                "range": {
                  "startIndex": 1,
                  "endIndex": 999999
                }
              }
            },
            {
              "insertText": {
                "location": {
                  "index": 1
                },
                "text": ${text.toJsonString()}
              }
            }
          ]
        }
    """.trimIndent()

    val req = Request.Builder()
        .url("$DOCS_BASE/documents/$documentId:batchUpdate")
        .auth(token)
        .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) error("Docs batchUpdate failed: ${resp.code} ${resp.message}")
    }
}

/**
 * Make a nice body for the doc: date, title, then dream text.
 */
fun buildDreamDocText(date: String, title: String, body: String): String {
    return buildString {
        appendLine(date)
        appendLine(title)
        appendLine()
        appendLine(body)
    }
}

/**
 * Upsert ONE dream as a Google Doc named
 *   {date} - {title}
 */
fun upsertDreamAsGoogleDoc(
    token: String,
    folderId: String,
    date: String,
    title: String,
    body: String
): String {
    val docName = dreamDocName(date, title)
    val existingId = findGoogleDoc(token, folderId, docName)
    val docId = existingId ?: createGoogleDoc(token, folderId, docName)
    val content = buildDreamDocText(date, title, body)
    replaceDocContent(token, docId, content)
    return docId
}

/**
 * Very simple sync:
 * - for each local dream, compute its Drive name
 * - look up a doc with that name
 * - (optional) compare timestamps
 * - update Drive if local is newer or doc missing
 */
fun syncDreamsToDrive(
    token: String,
    dreams: List<DreamEntry>
) {
    val folderId = ensureDreamZFolder(token)

    // If you have remote modifiedTime stored, you can compare here.
    dreams.forEach { dream ->
        val name = dreamDocName(dream.createdAt, dream.title)
        val existingId = findGoogleDoc(token, folderId, name)

        // Simple strategy: always overwrite Drive with local
        val docId = existingId ?: createGoogleDoc(token, folderId, name)
        val text = buildDreamDocText(dream.createdAt, dream.title, dream.body)
        replaceDocContent(token, docId, text)
    }
}

// helper to JSON-escape arbitrary text
private fun String.toJsonString(): String {
    return "\"" + this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n") + "\""
}

// your local model
data class DreamEntry(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val updatedAt: Long
)
