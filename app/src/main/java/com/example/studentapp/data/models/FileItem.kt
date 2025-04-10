package com.example.studentapp.data.models

import com.example.studentapp.utils.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val file: File,
    val category: FileUtils.FileCategory
) {
    val name: String get() = file.name
    val size: String get() = FileUtils.getReadableFileSize(file.length())
    val lastModified: String get() = formatDate(file.lastModified())
    val isPdf: Boolean get() = FileUtils.isPdf(file.name)
    val isImage: Boolean get() = FileUtils.isImage(file.name)
    
    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }
}
