package com.phoneai.localmodel

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

object ModelFileManager {

    // Default recommended models
    const val DEFAULT_SMOLLM_360M = "smollm2-360m-instruct-q4_k_m.gguf"     // ~258 MB
    const val DEFAULT_GEMMA_2_2B_GGUF = "gemma-2-2b-it-q4_k_m.gguf"          // ~1.7 GB
    const val DEFAULT_QWEN_05B = "qwen2.5-0.5b-instruct-q4_k_m.gguf"        // ~320 MB
    const val DEFAULT_SMOLLM_135M = "smollm2-135m-instruct-q4_k_m.gguf"     // ~90 MB
    const val DEFAULT_WIKI_ZIM = "wikipedia_en_physics_nopic_2026-07.zim"    // ~303 MB

    val RECOMMENDED_MODELS = listOf(
        DEFAULT_SMOLLM_360M,
        DEFAULT_GEMMA_2_2B_GGUF,
        DEFAULT_QWEN_05B,
        DEFAULT_SMOLLM_135M
    )

    // Return list of all installed models in the app files directory
    fun getInstalledModels(context: Context): List<File> {
        val list = mutableListOf<File>()
        context.filesDir.listFiles()?.forEach { file ->
            val n = file.name.lowercase()
            if ((n.endsWith(".gguf") || n.endsWith(".bin") || n.endsWith(".task")) && file.length() > 50_000_000L) {
                list.add(file)
            }
        }
        // Sort: .gguf files first, then smaller to larger
        return list.sortedWith(compareBy({ !it.name.endsWith(".gguf") }, { it.length() }))
    }

    // Look for existing model file in app internal files directory
    fun findExistingModel(context: Context): File? {
        val installed = getInstalledModels(context)
        // First preference: SmolLM2-360M (fastest, most reliable)
        installed.firstOrNull { it.name.equals(DEFAULT_SMOLLM_360M, ignoreCase = true) }?.let { return it }
        // Second preference: Gemma-2-2B GGUF
        installed.firstOrNull { it.name.equals(DEFAULT_GEMMA_2_2B_GGUF, ignoreCase = true) }?.let { return it }
        // Third preference: Any other GGUF
        installed.firstOrNull { it.name.endsWith(".gguf", ignoreCase = true) }?.let { return it }
        // Fallback: any other model file
        return installed.firstOrNull()
    }

    // Check if offline Wikipedia physics ZIM file is available
    fun findZimFile(context: Context): File? {
        // Check filesDir
        val internalFile = File(context.filesDir, DEFAULT_WIKI_ZIM)
        if (internalFile.exists() && internalFile.length() > 10_000_000L) return internalFile

        // Check /data/local/tmp
        val tmpFile = File("/data/local/tmp", DEFAULT_WIKI_ZIM)
        if (tmpFile.exists() && tmpFile.canRead() && tmpFile.length() > 10_000_000L) return tmpFile

        // Check any .zim in filesDir
        context.filesDir.listFiles()?.firstOrNull { it.name.lowercase().endsWith(".zim") }?.let { return it }
        return null
    }

    // Check if model file exists in /data/local/tmp/ and copy it to filesDir
    suspend fun copyFromLocalTmp(
        context: Context,
        onProgress: (percent: Int, copiedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<File>()
        for (name in RECOMMENDED_MODELS) {
            candidates.add(File("/data/local/tmp", name))
        }
        candidates.add(File("/data/local/tmp", DEFAULT_WIKI_ZIM))

        // Also scan /data/local/tmp for any GGUF or bin or zim files
        try {
            File("/data/local/tmp").listFiles()?.forEach { f ->
                val n = f.name.lowercase()
                if ((n.endsWith(".gguf") || n.endsWith(".bin") || n.endsWith(".task") || n.endsWith(".zim")) && !candidates.contains(f)) {
                    candidates.add(f)
                }
            }
        } catch (_: Exception) {}

        var sourceFile: File? = null
        for (f in candidates) {
            if (f.exists() && f.canRead() && f.length() > 50_000_000L) {
                sourceFile = f
                break
            }
        }

        if (sourceFile == null) {
            return@withContext Result.failure(
                Exception("Model not found in /data/local/tmp/. Push a model file (e.g. $DEFAULT_SMOLLM_360M) using 'adb push <file> /data/local/tmp/'.")
            )
        }

        val destFile = File(context.filesDir, sourceFile.name)
        return@withContext copyStreamWithProgress(
            sourceStream = FileInputStream(sourceFile),
            totalSize = sourceFile.length(),
            destFile = destFile,
            onProgress = onProgress
        )
    }

    // Copy model file from a Storage Access Framework Uri (e.g. Downloads folder)
    suspend fun copyFromUri(
        context: Context,
        uri: Uri,
        destFileName: String? = null,
        onProgress: (percent: Int, copiedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
            ?: return@withContext Result.failure(Exception("Unable to open selected file stream."))

        var totalSize = 0L
        var resolvedFileName = destFileName

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    totalSize = cursor.getLong(sizeIndex)
                }
                if (resolvedFileName == null) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        resolvedFileName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        val finalName = resolvedFileName ?: DEFAULT_SMOLLM_360M
        val destFile = File(context.filesDir, finalName)

        return@withContext copyStreamWithProgress(
            sourceStream = inputStream,
            totalSize = totalSize,
            destFile = destFile,
            onProgress = onProgress
        )
    }

    private fun copyStreamWithProgress(
        sourceStream: InputStream,
        totalSize: Long,
        destFile: File,
        onProgress: (percent: Int, copiedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> {
        val tempDest = File(destFile.parentFile, "${destFile.name}.tmp")
        try {
            sourceStream.use { input ->
                FileOutputStream(tempDest).use { output ->
                    val buffer = ByteArray(1024 * 1024) // 1MB buffer
                    var bytesRead: Int
                    var totalCopied = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalCopied += bytesRead

                        val percent = if (totalSize > 0) {
                            ((totalCopied * 100) / totalSize).toInt()
                        } else {
                            -1
                        }
                        onProgress(percent, totalCopied, totalSize)
                    }
                    output.flush()
                }
            }

            if (destFile.exists()) {
                destFile.delete()
            }
            if (tempDest.renameTo(destFile)) {
                return Result.success(destFile)
            } else {
                return Result.failure(Exception("Failed to finalize model file name."))
            }
        } catch (e: Exception) {
            if (tempDest.exists()) tempDest.delete()
            return Result.failure(e)
        }
    }
}
