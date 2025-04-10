package com.example.studentapp.ui.files

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studentapp.R
import com.example.studentapp.data.models.FileItem
import com.example.studentapp.databinding.ItemFileBinding
import com.example.studentapp.utils.FileUtils

class FileAdapter(
    private val onFileClick: (FileItem) -> Unit,
    private val onFileDelete: (FileItem) -> Unit,
    private val onFileShare: (FileItem) -> Unit
) : ListAdapter<FileItem, FileAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FileViewHolder(
        private val binding: ItemFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFileClick(getItem(position))
                }
            }

            binding.moreButton.setOnClickListener { view ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showPopupMenu(view, getItem(position))
                }
            }
        }

        fun bind(fileItem: FileItem) {
            binding.apply {
                // Set file name
                fileName.text = fileItem.name

                // Set file details (size and last modified date)
                fileDetails.text = "${fileItem.size} • ${fileItem.lastModified}"

                // Set file icon based on type
                fileIcon.setImageResource(
                    when {
                        fileItem.isPdf -> android.R.drawable.ic_menu_agenda
                        fileItem.isImage -> android.R.drawable.ic_menu_gallery
                        else -> android.R.drawable.ic_menu_save
                    }
                )

                // Set category chip
                categoryChip.text = when (fileItem.category) {
                    FileUtils.FileCategory.ASSIGNMENTS -> "Assignments"
                    FileUtils.FileCategory.LECTURE_NOTES -> "Lecture Notes"
                    FileUtils.FileCategory.OTHERS -> "Others"
                }

                // Set chip color based on category
                categoryChip.setChipBackgroundColorResource(
                    when (fileItem.category) {
                        FileUtils.FileCategory.ASSIGNMENTS -> R.color.primary_light
                        FileUtils.FileCategory.LECTURE_NOTES -> R.color.accent
                        FileUtils.FileCategory.OTHERS -> R.color.gray_400
                    }
                )
            }
        }

        private fun showPopupMenu(view: android.view.View, fileItem: FileItem) {
            PopupMenu(view.context, view).apply {
                inflate(R.menu.file_item_menu)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_share -> {
                            onFileShare(fileItem)
                            true
                        }
                        R.id.action_delete -> {
                            onFileDelete(fileItem)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }
    }

    private class FileDiffCallback : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.file.absolutePath == newItem.file.absolutePath
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem == newItem
        }
    }
}
