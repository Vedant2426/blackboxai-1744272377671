package com.example.studentapp.ui.qrscan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.studentapp.databinding.ActivityQrScanBinding
import com.example.studentapp.utils.FileUtils
import com.example.studentapp.utils.QrUtils
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.util.Base64

class QrScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrScanBinding
    private lateinit var barcodeView: DecoratedBarcodeView
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupScanner()
        checkCameraPermission()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupScanner() {
        barcodeView = binding.barcodeScanner
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                handleScannedQrCode(result.text)
            }
        })
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        } else {
            startScanning()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startScanning()
                } else {
                    Toast.makeText(
                        this,
                        "Camera permission is required for QR scanning",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun startScanning() {
        barcodeView.resume()
    }

    private fun handleScannedQrCode(qrContent: String) {
        lifecycleScope.launch {
            try {
                // Pause scanning while processing
                barcodeView.pause()
                
                // Decode QR data
                val fileData = QrUtils.decodeQrCodeData(qrContent)
                if (fileData == null) {
                    Toast.makeText(this@QrScanActivity, "Invalid QR code format", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create directory if it doesn't exist
                val directory = FileUtils.getAppFilesDir(this@QrScanActivity, fileData.category)
                val file = File(directory, fileData.fileName)

                // Save file
                withContext(Dispatchers.IO) {
                    val fileBytes = Base64.decode(fileData.fileContent, Base64.DEFAULT)
                    FileOutputStream(file).use { fos ->
                        fos.write(fileBytes)
                    }
                }

                // Verify checksum
                val savedFileChecksum = withContext(Dispatchers.IO) {
                    QrUtils.calculateChecksum(file)
                }

                if (savedFileChecksum == fileData.checksum) {
                    Toast.makeText(this@QrScanActivity, "File received successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // Delete corrupted file
                    file.delete()
                    Toast.makeText(this@QrScanActivity, "File verification failed", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@QrScanActivity, "Error processing file: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // Resume scanning
                barcodeView.resume()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}
