package com.phonecluster.app.utils
import com.phonecluster.app.core.SERVER_BASE_URL
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.code

class FileRepository {

    private val client = OkHttpClient()


    suspend fun downloadFile(fileId: Long) = withContext(Dispatchers.IO) {

        val request = Request.Builder()
            .url("$SERVER_BASE_URL/files/$fileId/download")
            .get()
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Download failed: ${response.code}")
        }

        val bytes = response.body?.bytes()
            ?: throw RuntimeException("Empty response")

        saveFileLocally(fileId, bytes)
    }

    private fun saveFileLocally(fileId: Long, data: ByteArray) {
        val file = File("/storage/emulated/0/Download/file_$fileId.bin")
        file.writeBytes(data)
    }
}