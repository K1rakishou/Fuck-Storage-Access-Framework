package com.github.k1rakishou.fsaf.manager

import android.content.Context
import android.util.Log
import com.github.k1rakishou.fsaf.BadPathSymbolResolutionStrategy
import com.github.k1rakishou.fsaf.FastFileSearchTree
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.document_file.SnapshotDocumentFile
import com.github.k1rakishou.fsaf.extensions.splitIntoSegments
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.ExternalFile
import com.github.k1rakishou.fsaf.file.Root
import com.github.k1rakishou.fsaf.manager.base_directory.DirectoryManager
import java.io.InputStream
import java.io.OutputStream

class SnapshotFileManager(
  appContext: Context,
  badPathSymbolResolutionStrategy: BadPathSymbolResolutionStrategy,
  directoryManager: DirectoryManager,
  private val fastFileSearchTree: FastFileSearchTree<SnapshotDocumentFile>
) : ExternalFileManager(appContext, badPathSymbolResolutionStrategy, directoryManager) {

  override fun exists(file: AbstractFile): Boolean {
    return fastFileSearchTree.findSegment(file.extractSegments())?.exists() ?: false
  }

  override fun isFile(file: AbstractFile): Boolean {
    return fastFileSearchTree.findSegment(file.extractSegments())?.isFile() ?: false
  }

  override fun isDirectory(file: AbstractFile): Boolean {
    return fastFileSearchTree.findSegment(file.extractSegments())?.isDirectory() ?: false
  }

  override fun canRead(file: AbstractFile): Boolean {
    return fastFileSearchTree.findSegment(file.extractSegments())?.canRead() ?: false
  }

  override fun canWrite(file: AbstractFile): Boolean {
    return fastFileSearchTree.findSegment(file.extractSegments())?.canWrite() ?: false
  }

  override fun getSegmentNames(file: AbstractFile): List<String> {
    return file.toString().splitIntoSegments()
  }

  override fun delete(file: AbstractFile): Boolean {
    return fastFileSearchTree.findSegment(file.extractSegments())?.delete() ?: false
  }

  override fun deleteContent(dir: AbstractFile): Boolean {
    val cachedDir = fastFileSearchTree.findSegment(dir.extractSegments())
    if (cachedDir == null) {
      // Already deleted
      return true
    }

    if (!cachedDir.isDirectory()) {
      Log.e(
        TAG,
        "deleteContent() Only directories are supported (files can't have contents anyway)"
      )
      return false
    }

    var allSuccess = false

    fastFileSearchTree.visitEverySegmentAfterPath(getSegmentNames(dir), true) { node ->
      val success = node.getNodeValue()?.delete() ?: true
      if (!success) {
        allSuccess = false
      }
    }

    // This may delete only some files and leave other but at least you will know that something
    // went wrong (just don't forget to check the result)
    return allSuccess
  }

  override fun getInputStream(file: AbstractFile): InputStream? {
    val cachedFile = fastFileSearchTree.findSegment(file.extractSegments())
    if (cachedFile == null) {
      Log.e(TAG, "getInputStream() fastFileSearchTree.findSegment() returned null")
      return null
    }

    if (!cachedFile.exists()) {
      Log.e(TAG, "getInputStream() cachedFile does not exist, uri = ${cachedFile.uri()}")
      return null
    }

    if (!cachedFile.isFile()) {
      Log.e(TAG, "getInputStream() cachedFile is not a file, uri = ${cachedFile.uri()}")
      return null
    }

    if (!cachedFile.canRead()) {
      Log.e(TAG, "getInputStream() cannot read from cachedFile, uri = ${cachedFile.uri()}")
      return null
    }

    val contentResolver = appContext.contentResolver
    return contentResolver.openInputStream(cachedFile.uri())
  }

  override fun getOutputStream(file: AbstractFile): OutputStream? {
    val cachedFile = fastFileSearchTree.findSegment(file.extractSegments())
    if (cachedFile == null) {
      Log.e(TAG, "getOutputStream() fastFileSearchTree.findSegment() returned null")
      return null
    }

    if (!cachedFile.exists()) {
      Log.e(TAG, "getOutputStream() cachedFile does not exist, uri = ${cachedFile.uri()}")
      return null
    }

    if (!cachedFile.isFile()) {
      Log.e(TAG, "getOutputStream() cachedFile is not a file, uri = ${cachedFile.uri()}")
      return null
    }

    if (!cachedFile.canRead()) {
      Log.e(TAG, "getOutputStream() cannot read from cachedFile, uri = ${cachedFile.uri()}")
      return null
    }

    val contentResolver = appContext.contentResolver
    return contentResolver.openOutputStream(cachedFile.uri())
  }

  override fun getName(file: AbstractFile): String? {
    return fastFileSearchTree.findSegment(file.extractSegments())?.name()
  }

  override fun findFile(dir: AbstractFile, fileName: String): ExternalFile? {
    val segments = dir.extractSegments() + fileName
    val cachedFile = fastFileSearchTree.findSegment(segments)
      ?: return null

    val innerRoot = if (cachedFile.isFile()) {
      Root.FileRoot(cachedFile as CachingDocumentFile, cachedFile.name()!!)
    } else {
      Root.DirRoot(cachedFile as CachingDocumentFile)
    }

    return ExternalFile(
      appContext,
      badPathSymbolResolutionStrategy,
      innerRoot
    )
  }

  override fun getLength(file: AbstractFile): Long {
    return fastFileSearchTree.findSegment(file.extractSegments())?.length() ?: 0L
  }

  override fun listFiles(dir: AbstractFile): List<ExternalFile> {
    val files = ArrayList<ExternalFile>(32)

    fastFileSearchTree.visitEverySegmentAfterPath(getSegmentNames(dir), false) { node ->
      node.getNodeValue()?.let { cachedFile ->
        val innerRoot = if (cachedFile.isFile()) {
          Root.FileRoot(cachedFile as CachingDocumentFile, cachedFile.name()!!)
        } else {
          Root.DirRoot(cachedFile as CachingDocumentFile)
        }

        files += ExternalFile(
          appContext,
          badPathSymbolResolutionStrategy,
          innerRoot
        )
      }
    }

    return files
  }

  override fun lastModified(file: AbstractFile): Long {
    return fastFileSearchTree.findSegment(file.extractSegments())?.lastModified() ?: 0L
  }

  private fun AbstractFile.extractSegments(): List<String> {
    return getSegmentNames(this)
  }

  companion object {
    private const val TAG = "SnapshotFileManager"
  }
}
