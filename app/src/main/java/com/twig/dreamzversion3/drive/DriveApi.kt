package com.twig.dreamzversion3.drive

import okhttp3.Headers
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.net.URLEncoder

private val client = OkHttpClient()
private const val DRIVE_BASE = "https://www.googleapis.com/drive/v3"
private const val UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3/files"

private fun Request.Builder.auth(token: String) =
    header("Authorization", "Bearer $token")

/** Find or create a folder named "DreamZ" and return its fileId. */
fun ensureDreamZFolder(token: String): String {
    val q = "mimeType='application/vnd.google-apps.folder' and name='DreamZ' and trashed=false"
    val listReq = Request.Builder()
        .url("$DRIVE_BASE/files?q=${URLEncoder.encode(q, "UTF-8")}&fields=files(id,name)")
        .auth(token)
        .build()
    client.newCall(listReq).execute().use { resp ->
        if (!resp.isSuccessful) error("Drive list failed: ${resp.code}")
        val body = resp.body?.string().orEmpty()
        Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)?.let { return it }
    }

    val meta = """{"name":"DreamZ","mimeType":"application/vnd.google-apps.folder"}"""
    val createReq = Request.Builder()
        .url("$DRIVE_BASE/files?fields=id")
        .post(meta.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .auth(token)
        .build()
    client.newCall(createReq).execute().use { resp ->
        if (!resp.isSuccessful) error("Drive create folder failed: ${resp.code}")
        val body = resp.body?.string().orEmpty()
        val id = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
        require(!id.isNullOrBlank()) { "No folder id" }
        return id!!
    }
}

/** Create/update a JSON file by name inside the parent folder. */
fun upsertJsonFile(token: String, parentId: String, name: String, jsonContent: String) {
    val q = "'$parentId' in parents and name='${name.replace("'", "\\'")}' and trashed=false"
    val listReq = Request.Builder()
        .url("$DRIVE_BASE/files?q=${URLEncoder.encode(q, "UTF-8")}&fields=files(id)")
        .auth(token)
        .build()
    val existingId = client.newCall(listReq).execute().use { resp ->
        if (!resp.isSuccessful) error("Drive list (upsert) failed: ${resp.code}")
        val body = resp.body?.string().orEmpty()
        Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
    }

    val meta = """{"name":"$name","parents":["$parentId"]}"""
    val boundary = "boundary${System.currentTimeMillis()}"
    val multipart = buildString {
        append("--$boundary\r\n")
        append("Content-Type: application/json; charset=utf-8\r\n\r\n")
        append(meta).append("\r\n")
        append("--$boundary\r\n")
        append("Content-Type: application/json; charset=utf-8\r\n\r\n")
        append(jsonContent).append("\r\n")
        append("--$boundary--\r\n")
    }.toRequestBody("multipart/related; boundary=$boundary".toMediaType())

    val url = if (existingId != null)
        "$UPLOAD_BASE/$existingId?uploadType=multipart"
    else
        "$UPLOAD_BASE?uploadType=multipart"

    val req = Request.Builder()
        .url(url)
        .post(multipart)
        .auth(token)
        .build()

    client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) error("Drive upsert failed: ${resp.code} ${resp.message}")
    }
}

fun upsertBinaryFile(
    token: String,
    parentId: String,
    name: String,
    mimeType: String,
    data: ByteArray
) {
    val q = "'$parentId' in parents and name='${name.replace("'", "\\'")}' and trashed=false"
    val listReq = Request.Builder()
        .url("$DRIVE_BASE/files?q=${URLEncoder.encode(q, "UTF-8")}&fields=files(id)")
        .auth(token)
        .build()
    val existingId = client.newCall(listReq).execute().use { resp ->
        if (!resp.isSuccessful) error("Drive list (binary upsert) failed: ${resp.code}")
        val body = resp.body?.string().orEmpty()
        Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
    }

    val meta = """{"name":"$name","parents":["$parentId"]}"""
    val boundary = "boundary${System.currentTimeMillis()}"
    val multipart = MultipartBody.Builder(boundary)
        .setType("multipart/related".toMediaType())
        .addPart(
            Headers.headersOf("Content-Type", "application/json; charset=utf-8"),
            meta.toRequestBody("application/json; charset=utf-8".toMediaType())
        )
        .addPart(
            Headers.headersOf("Content-Type", mimeType),
            data.toRequestBody(mimeType.toMediaType())
        )
        .build()

    val url = if (existingId != null)
        "$UPLOAD_BASE/$existingId?uploadType=multipart"
    else
        "$UPLOAD_BASE?uploadType=multipart"

    val req = Request.Builder()
        .url(url)
        .post(multipart)
        .auth(token)
        .build()

    client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) error("Drive upsert failed: ${resp.code} ${resp.message}")
    }
}
