package com.example.studentapp.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {
    // File categories
    enum class FileCategory {
        ASSIGNMENTS,
        LECTURE_NOTES,
        OTHERS
    }

    private const val DATE_FORMAT = "yyyyMMdd_HHmmss"

    /**
     * Get the app's private directory for storing files
     */
    fun getAppFilesDir(context: Context, category: FileCategory): File {
        val categoryDir = when (category) {
            FileCategory.ASSIGNMENTS -> "assignments"
            FileCategory.LECTURE_NOTES -> "lecture_notes"
            FileCategory.OTHERS -> "others"
        }
        
        val directory = File(context.getExternalFilesDir(null), categoryDir)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    /**
     * Generate a unique filename based on timestamp
     */
    fun generateUniqueFileName(originalName: String): String {
        val timeStamp = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
        val extension = getFileExtension(originalName)
        return "FILE_${timeStamp}.$extension"
    }

    /**
     * Get file extension from file name
     */
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast(".", "")
    }

    /**
     * Get MIME type from Uri
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        val contentResolver = context.contentResolver
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(contentResolver.getType(uri))
    }

    /**
     * Save file from Uri to app's private storage
     */
    @Throws(IOException::class)
    fun saveFile(context: Context, sourceUri: Uri, category: FileCategory, fileName: String): File {
        val destinationFile = File(getAppFilesDir(context, category), fileName)
        
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        
        return destinationFile
    }

    /**
     * Delete file from storage
     */
    fun deleteFile(file: File): Boolean {
        return file.exists() && file.delete()
    }

    /**
     * Get all files in a category
     */
    fun getCategoryFiles(context: Context, category: FileCategory): List<File> {
        val directory = getAppFilesDir(context, category)
        return directory.listFiles()?.toList() ?: emptyList()
    }

    /**
     * Get all files across all categories
     */
    fun getAllFiles(context: Context): List<File> {
        return FileCategory.values().flatMap { category ->
            getCategoryFiles(context, category)
        }
    }

    /**
     * Get human-readable file size
     */
    fun getReadableFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(
            "%.1f %s",
            size / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    /**
     * Check if file is a PDF
     */
    fun isPdf(fileName: String): Boolean {
        return getFileExtension(fileName).equals("pdf", ignoreCase = true)
    }

    /**
     * Check if file is an image
     */
    fun isImage(fileName: String): Boolean {
        val extension = getFileExtension(fileName).lowercase()
        return extension in arrayOf("jpg", "jpeg", "png", "gif", "bmp")
    }
}
