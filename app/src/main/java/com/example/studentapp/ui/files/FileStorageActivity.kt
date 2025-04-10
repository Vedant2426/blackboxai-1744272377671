package com.example.studentapp.ui.files

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentapp.R
import com.example.studentapp.data.models.FileItem
import com.example.studentapp.databinding.ActivityFileStorageBinding
import com.example.studentapp.databinding.DialogQrCodeBinding
import com.example.studentapp.utils.FileUtils
import com.example.studentapp.utils.QrUtils
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileStorageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFileStorageBinding
    private lateinit var fileAdapter: FileAdapter
    private var currentCategory = FileUtils.FileCategory.ASSIGNMENTS

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupTabLayout()
        setupFab()
        loadFiles()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(
            onFileClick = { fileItem ->
                openFile(fileItem)
            },
            onFileDelete = { fileItem ->
                showDeleteConfirmationDialog(fileItem)
            },
            onFileShare = { fileItem ->
                showQrCodeDialog(fileItem)
            }
        )

        binding.filesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FileStorageActivity)
            adapter = fileAdapter
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentCategory = when (tab?.position) {
                    0 -> FileUtils.FileCategory.ASSIGNMENTS
                    1 -> FileUtils.FileCategory.LECTURE_NOTES
                    2 -> FileUtils.FileCategory.OTHERS
                    else -> FileUtils.FileCategory.ASSIGNMENTS
                }
                loadFiles()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupFab() {
        binding.addFileFab.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    private fun handleSelectedFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                showLoading(true)
                
                val fileName = getFileName(uri)
                val savedFile = withContext(Dispatchers.IO) {
                    FileUtils.saveFile(
                        context = this@FileStorageActivity,
                        sourceUri = uri,
                        category = currentCategory,
                        fileName = FileUtils.generateUniqueFileName(fileName)
                    )
                }
                
                // Refresh the file list
                loadFiles()
                Toast.makeText(this@FileStorageActivity, "File saved successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(this@FileStorageActivity, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        } ?: "unknown_file"
    }

    private fun loadFiles() {
        lifecycleScope.launch {
            try {
                showLoading(true)
                
                val files = withContext(Dispatchers.IO) {
                    FileUtils.getCategoryFiles(this@FileStorageActivity, currentCategory)
                }
                
                val fileItems = files.map { FileItem(it, currentCategory) }
                
                // Update UI
                if (fileItems.isEmpty()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.filesRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateText.visibility = View.GONE
                    binding.filesRecyclerView.visibility = View.VISIBLE
                    fileAdapter.submitList(fileItems)
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@FileStorageActivity, "Failed to load files: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showDeleteConfirmationDialog(fileItem: FileItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete ${fileItem.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteFile(fileItem)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFile(fileItem: FileItem) {
        lifecycleScope.launch {
            try {
                showLoading(true)
                
                val success = withContext(Dispatchers.IO) {
                    FileUtils.deleteFile(fileItem.file)
                }
                
                if (success) {
                    Toast.makeText(this@FileStorageActivity, "File deleted successfully", Toast.LENGTH_SHORT).show()
                    loadFiles()
                } else {
                    Toast.makeText(this@FileStorageActivity, "Failed to delete file", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@FileStorageActivity, "Error deleting file: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.addFileFab.isEnabled = !show
    }

    private fun openFile(fileItem: FileItem) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                fileItem.file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                data = uri

                when {
                    fileItem.isPdf -> {
                        type = "application/pdf"
                    }
                    fileItem.isImage -> {
                        type = "image/*"
                    }
                    else -> {
                        type = "*/*"
                    }
                }
            }

            startActivity(Intent.createChooser(intent, "Open ${fileItem.name} with..."))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error opening file: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showQrCodeDialog(fileItem: FileItem) {
        val dialog = Dialog(this)
        val binding = DialogQrCodeBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        // Set dialog width to match parent with margins
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.apply {
            titleText.text = "Share: ${fileItem.name}"
            fileInfoText.text = "Size: ${fileItem.size}\nLast modified: ${fileItem.lastModified}"

            // Generate QR code
            lifecycleScope.launch(Dispatchers.IO) {
                val qrBitmap = QrUtils.generateQrCodeForFile(fileItem.file, fileItem.category)
                
                withContext(Dispatchers.Main) {
                    if (qrBitmap != null) {
                        qrCodeImage.setImageBitmap(qrBitmap)
                    } else {
                        fileInfoText.text = "Failed to generate QR code"
                        Toast.makeText(
                            this@FileStorageActivity,
                            "Failed to generate QR code",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            closeButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
