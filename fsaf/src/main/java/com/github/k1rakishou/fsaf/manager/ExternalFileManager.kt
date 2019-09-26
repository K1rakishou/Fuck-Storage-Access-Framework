package com.github.k1rakishou.fsaf.manager

import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.github.k1rakishou.fsaf.FastFileSearchTree
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.document_file.SnapshotDocumentFile
import com.github.k1rakishou.fsaf.extensions.getMimeFromFilename
import com.github.k1rakishou.fsaf.extensions.splitIntoSegments
import com.github.k1rakishou.fsaf.file.*
import com.github.k1rakishou.fsaf.util.SAFHelper
import java.io.FileDescriptor
import java.io.InputStream
import java.io.OutputStream

class ExternalFileManager(
  private val appContext: Context,
  private val searchMode: SearchMode
) : BaseFileManager {
  private val mimeTypeMap = MimeTypeMap.getSingleton()
  private val fastFileSearchTree: FastFileSearchTree<CachingDocumentFile> = FastFileSearchTree()

  fun getFastFileSearchTree() = fastFileSearchTree

  fun cacheFiles(files: List<Pair<ExternalFile, SnapshotDocumentFile>>) {
    for ((externalFile, snapshotDocFile) in files) {
      val segments = externalFile.getFullPath().splitIntoSegments()
      if (segments.isEmpty()) {
        Log.e(TAG, "cacheFile.splitIntoSegments() returned empty list")
        continue
      }

      check(fastFileSearchTree.insertSegments(segments, snapshotDocFile))
    }
  }

  fun uncacheFile(file: AbstractFile) {
    val segments = file.getFullPath().splitIntoSegments()
    if (segments.isEmpty()) {
      Log.e(TAG, "uncacheFile.splitIntoSegments() returned empty list")
      return
    }

    fastFileSearchTree.removeSegments(segments)
  }

  @Suppress("UNCHECKED_CAST")
  override fun create(baseDir: AbstractFile, segments: List<Segment>): ExternalFile? {
    val root = baseDir.getFileRoot<CachingDocumentFile>()
    check(root !is Root.FileRoot) {
      "root is already FileRoot, cannot append anything anymore"
    }

    check(segments.isNotEmpty()) { "root has already been created" }
    var newFile: CachingDocumentFile? = null

    for (segment in segments) {
      val innerFile = newFile ?: root.holder

      val mimeType = if (segment.isFileName) {
        mimeTypeMap.getMimeFromFilename(segment.name)
      } else {
        DocumentsContract.Document.MIME_TYPE_DIR
      }

      val newUri = DocumentsContract.createDocument(
        appContext.contentResolver,
        innerFile.uri,
        mimeType,
        segment.name
      )

      if (newUri == null) {
        Log.e(
          TAG, "create() DocumentsContract.createDocument returned null, " +
            "file.uri = ${innerFile.uri}, segment.name = ${segment.name}"
        )
        return null
      }

      val createdFile = if (segment.isFileName) {
        DocumentFile.fromSingleUri(appContext, newUri)
      } else {
        DocumentFile.fromTreeUri(appContext, newUri)
      }

      if (createdFile == null) {
        Log.e(
          TAG, "create() DocumentFile.fromSingleUri returned null, directoryUri = ${newUri}"
        )
        return null
      }

      if (segment.isFileName) {
        // Ignore any left segments (which we shouldn't have) after encountering fileName
        // segment
        return ExternalFile(
          appContext, Root.FileRoot(
            CachingDocumentFile(appContext, createdFile),
            segment.name
          )
        )
      } else {
        newFile = CachingDocumentFile(appContext, createdFile)
      }
    }

    if (newFile == null) {
      Log.e(TAG, "result file is null")
      return null
    }

    val lastSegment = segments.last()
    val isLastSegmentFilename = lastSegment.isFileName

    val newRoot = if (isLastSegmentFilename) {
      Root.FileRoot(newFile, lastSegment.name)
    } else {
      Root.DirRoot(newFile)
    }

    return ExternalFile(appContext, newRoot)
  }

  override fun exists(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.exists ?: false
  override fun isFile(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.isFile ?: false
  override fun isDirectory(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.isDirectory ?: false
  override fun canRead(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.canRead ?: false
  override fun canWrite(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.canWrite ?: false

  override fun getSegmentNames(file: AbstractFile): List<String> {
    return file.getFullPath().splitIntoSegments()
  }

  override fun delete(file: AbstractFile): Boolean {
    val documentFile = toDocumentFile(file.clone())
      ?: return true

    if (searchMode == SearchMode.Fast) {
      val segments = file
        .toString()
        .splitIntoSegments()
      check(segments.isNotEmpty())

      if (!fastFileSearchTree.removeSegments(segments)) {
        Log.e(TAG, "delete(), Couldn't remove segments $segments from fastFileSearchTree")
        return false
      }
    }

    return documentFile.delete()
  }

  override fun deleteContent(dir: AbstractFile) {
    val documentFile = toDocumentFile(dir.clone())
      ?: return

    if (!documentFile.isDirectory) {
      Log.e(TAG, "Only directories are supported (files can't have contents anyway)")
      return
    }

    val filesInDirectory = documentFile.listFiles()

    if (searchMode == SearchMode.Fast) {
      for (fileInDirectory in filesInDirectory) {
        val segments = fileInDirectory.uri.toString().splitIntoSegments()

        if (!fastFileSearchTree.removeSegments(segments)) {
          Log.e(TAG, "deleteContent() Couldn't remove segments $segments from fastFileSearchTree")
          return
        }
      }
    }

    filesInDirectory.forEach { it.delete() }
  }

  override fun getInputStream(file: AbstractFile): InputStream? {
    val contentResolver = appContext.contentResolver
    val documentFile = toDocumentFile(file.clone())

    if (documentFile == null) {
      Log.e(TAG, "getInputStream() toDocumentFile() returned null")
      return null
    }

    if (!documentFile.exists) {
      Log.e(TAG, "getInputStream() documentFile does not exist, uri = ${documentFile.uri}")
      return null
    }

    if (!documentFile.isFile) {
      Log.e(TAG, "getInputStream() documentFile is not a file, uri = ${documentFile.uri}")
      return null
    }

    if (!documentFile.canRead) {
      Log.e(TAG, "getInputStream() cannot read from documentFile, uri = ${documentFile.uri}")
      return null
    }

    return contentResolver.openInputStream(documentFile.uri)
  }

  override fun getOutputStream(file: AbstractFile): OutputStream? {
    val contentResolver = appContext.contentResolver
    val documentFile = toDocumentFile(file.clone())

    if (documentFile == null) {
      Log.e(TAG, "getOutputStream() toDocumentFile() returned null")
      return null
    }

    if (!documentFile.exists) {
      Log.e(TAG, "getOutputStream() documentFile does not exist, uri = ${documentFile.uri}")
      return null
    }

    if (!documentFile.isFile) {
      Log.e(TAG, "getOutputStream() documentFile is not a file, uri = ${documentFile.uri}")
      return null
    }

    if (!documentFile.canWrite) {
      Log.e(TAG, "getOutputStream() cannot write to documentFile, uri = ${documentFile.uri}")
      return null
    }

    return contentResolver.openOutputStream(documentFile.uri)
  }

  override fun getName(file: AbstractFile): String {
    val segments = file.getFileSegments()
    if (segments.isNotEmpty() && segments.last().isFileName) {
      return segments.last().name
    }

    val documentFile = toDocumentFile(file.clone())
      ?: throw IllegalStateException("getName() toDocumentFile() returned null")

    return documentFile.name
      ?: throw IllegalStateException("Could not extract file name from document file")
  }

  override fun findFile(dir: AbstractFile, fileName: String): ExternalFile? {
    val root = dir.getFileRoot<CachingDocumentFile>()
    val segments = dir.getFileSegments()

    check(root !is Root.FileRoot) { "Cannot use FileRoot as directory" }
    check(!segments.last().isFileName) { "Cannot do search when last segment is file" }

    val parentDocFile = if (segments.isNotEmpty()) {
      SAFHelper.findDeepFile(appContext, root.holder.uri, segments)
    } else {
      val docFile = DocumentFile.fromSingleUri(appContext, root.holder.uri)
      if (docFile != null) {
        CachingDocumentFile(appContext, docFile)
      } else {
        null
      }
    }

    if (parentDocFile == null) {
      return null
    }

    val cachingDocFile = SAFHelper.findCachingFile(appContext, parentDocFile.uri, fileName)
    if (cachingDocFile == null || !cachingDocFile.exists) {
      return null
    }

    val innerRoot = if (cachingDocFile.isFile) {
      Root.FileRoot(cachingDocFile, cachingDocFile.name!!)
    } else {
      Root.DirRoot(cachingDocFile)
    }

    return ExternalFile(
      appContext,
      innerRoot
    )
  }

  override fun getLength(file: AbstractFile): Long = toDocumentFile(file.clone())?.length ?: -1L

  override fun listFiles(dir: AbstractFile): List<ExternalFile> {
    val root = dir.getFileRoot<CachingDocumentFile>()
    check(root !is Root.FileRoot) { "Cannot use listFiles with FileRoot" }

    return toDocumentFile(dir.clone())
      ?.listFiles()
      ?.map { documentFile -> ExternalFile(appContext, Root.DirRoot(documentFile)) }
      ?: emptyList()
  }

  override fun lastModified(file: AbstractFile): Long {
    return toDocumentFile(file.clone())?.lastModified ?: 0L
  }

  private fun toDocumentFile(file: AbstractFile): CachingDocumentFile? {
    val segments = file.getFileSegments()
    if (segments.isEmpty()) {
      return file.getFileRoot<CachingDocumentFile>().holder
    }

    val parentUri = file.getFileRoot<CachingDocumentFile>().holder.uri
    return SAFHelper.findDeepFile(appContext, parentUri, segments)
  }

  override fun <T> withFileDescriptor(
    file: AbstractFile,
    fileDescriptorMode: FileDescriptorMode,
    func: (FileDescriptor) -> T?
  ): T? {
    return getParcelFileDescriptor(file, fileDescriptorMode)
      ?.use { pfd -> func(pfd.fileDescriptor) }
      ?: throw IllegalStateException(
        "Could not get ParcelFileDescriptor " +
          "from root with uri = ${file.getFileRoot<DocumentFile>().holder.uri}"
      )
  }

  private fun getParcelFileDescriptor(
    file: AbstractFile,
    fileDescriptorMode: FileDescriptorMode
  ): ParcelFileDescriptor? {
    return appContext.contentResolver.openFileDescriptor(
      file.getFileRoot<DocumentFile>().holder.uri,
      fileDescriptorMode.mode
    )
  }

  /**
   * For tests only!
   * */
  enum class SearchMode {
    /**
     * Fast search mode uses FastFileSearchTree
     * */
    Fast,
    /**
     * Slow search mode doesn't use FastFileSearchTree
     * */
    Slow
  }

  companion object {
    private const val TAG = "ExternalFileManager"
  }
}