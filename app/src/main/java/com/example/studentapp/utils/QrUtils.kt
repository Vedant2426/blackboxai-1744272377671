package com.example.studentapp.utils

import android.graphics.Bitmap
import android.util.Base64
import com.example.studentapp.data.models.FileItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object QrUtils {
    private const val QR_SIZE = 512
    
    data class FileQrData(
        val fileName: String,
        val fileSize: Long,
        val checksum: String,
        val fileContent: String,
        val category: FileUtils.FileCategory
    )

    fun generateQrCodeForFile(file: File, category: FileUtils.FileCategory): Bitmap? {
        return try {
            val fileData = FileQrData(
                fileName = file.name,
                fileSize = file.length(),
                checksum = calculateChecksum(file),
                fileContent = encodeFileToBase64(file),
                category = category
            )
            
            val jsonData = JSONObject().apply {
                put("fileName", fileData.fileName)
                put("fileSize", fileData.fileSize)
                put("checksum", fileData.checksum)
                put("fileContent", fileData.fileContent)
                put("category", fileData.category.name)
            }.toString()

            generateQrCode(jsonData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decodeQrCodeData(qrContent: String): FileQrData? {
        return try {
            val json = JSONObject(qrContent)
            FileQrData(
                fileName = json.getString("fileName"),
                fileSize = json.getLong("fileSize"),
                checksum = json.getString("checksum"),
                fileContent = json.getString("fileContent"),
                category = FileUtils.FileCategory.valueOf(json.getString("category"))
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun encodeFileToBase64(file: File): String {
        return Base64.encodeToString(file.readBytes(), Base64.DEFAULT)
    }

    private fun generateQrCode(content: String): Bitmap {
        val bitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            QR_SIZE,
            QR_SIZE
        )
        return bitMatrixToBitmap(bitMatrix)
    }

    private fun bitMatrixToBitmap(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        
        return bitmap
    }
}
