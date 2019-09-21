package com.github.k1rakishou.fsaf.manager

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.github.k1rakishou.fsaf.FastFileSearchTree
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.ExternalFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class ExternalFileManager(
  private val appContext: Context
) : BaseFileManager {
  // TODO: supa search speed
  private val fastFileSearchTree: FastFileSearchTree<DocumentFile> = FastFileSearchTree()

  override fun exists(file: AbstractFile): Boolean = toDocumentFile(file.clone())?.exists() ?: false
  override fun isFile(file: AbstractFile): Boolean = toDocumentFile(file.clone())?.isFile ?: false
  override fun isDirectory(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.isDirectory ?: false

  override fun canRead(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.canRead() ?: false

  override fun canWrite(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.canWrite() ?: false

  override fun getSegmentNames(file: AbstractFile): List<String> {
    return file.getFullPath()
      .split(File.separatorChar)
      .flatMap { names -> names.split(AbstractFile.ENCODED_SEPARATOR) }
  }

  override fun delete(file: AbstractFile): Boolean {
    return toDocumentFile(file.clone())?.delete() ?: false
  }

  override fun getInputStream(file: AbstractFile): InputStream? {
    val contentResolver = appContext.contentResolver
    val documentFile = toDocumentFile(file.clone())

    if (documentFile == null) {
      Log.e(TAG, "getInputStream() toDocumentFile() returned null")
      return null
    }

    if (!documentFile.exists()) {
      Log.e(TAG, "getInputStream() documentFile does not exist, uri = ${documentFile.uri}")
      return null
    }

    if (!documentFile.isFile) {
      Log.e(TAG, "getInputStream() documentFile is not a file, uri = ${documentFile.uri}")
      return null
    }

    if (!documentFile.canRead()) {
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

    if (!documentFile.exists()) {
      Log.e(TAG, "getOutputStream() documentFile does not exist, uri = ${documentFile.uri}")
      return null
    }

    if (!documentFile.isFile) {
      Log.e(TAG, "getOutputStream() documentFile is not a file, uri = ${documentFile.uri}")
      return null
    }

    if (!documentFile.canWrite()) {
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
    val root = dir.getFileRoot<DocumentFile>()
    val segments = dir.getFileSegments()
    check(root !is AbstractFile.Root.FileRoot) { "Cannot use FileRoot as directory" }

    val filteredSegments = segments
      .map { it.name }

    var dirTree = root.holder
    for (segment in filteredSegments) {
      // FIXME: SLOW!!!
      for (documentFile in dirTree.listFiles()) {
        if (documentFile.name != null && documentFile.name == segment) {
          dirTree = documentFile
          break
        }
      }
    }

    // FIXME: SLOW!!!
    for (documentFile in dirTree.listFiles()) {
      if (documentFile.name != null && documentFile.name == fileName) {
        val innerRoot = if (documentFile.isFile) {
          AbstractFile.Root.FileRoot(documentFile, documentFile.name!!)
        } else {
          AbstractFile.Root.DirRoot(documentFile)
        }

        return ExternalFile(
          appContext,
          innerRoot
        )
      }
    }

    if (dirTree.name == fileName) {
      val innerRoot = if (dirTree.isFile) {
        AbstractFile.Root.FileRoot(dirTree, dirTree.name!!)
      } else {
        AbstractFile.Root.DirRoot(dirTree)
      }

      return ExternalFile(
        appContext,
        innerRoot
      )
    }

    // Not found
    return null
  }

  override fun getLength(file: AbstractFile): Long = toDocumentFile(file.clone())?.length() ?: -1L

  override fun listFiles(dir: AbstractFile): List<ExternalFile> {
    val root = dir.getFileRoot<DocumentFile>()

    check(root !is AbstractFile.Root.FileRoot) { "Cannot use listFiles with FileRoot" }

    return toDocumentFile(dir.clone())
      ?.listFiles()
      ?.map { documentFile -> ExternalFile(appContext, AbstractFile.Root.DirRoot(documentFile)) }
      ?: emptyList()
  }

  override fun lastModified(file: AbstractFile): Long {
    return toDocumentFile(file.clone())?.lastModified() ?: 0L
  }

  private fun toDocumentFile(file: AbstractFile): DocumentFile? {
    if (file.getFileSegments().isEmpty()) {
      return file.getFileRoot<DocumentFile>().holder
    }

    val root = file.getFileRoot<DocumentFile>()
    val segments = file.getFileSegments()

    var documentFile: DocumentFile = root.holder
    var index = 0

    for (element in segments) {
      val foundFile = fastFindFile(documentFile, element)
        ?: break

      documentFile = foundFile
      ++index
    }

    if (index != segments.size) {
      return createDocumentFileFromUri(file, documentFile.uri, index)
    }

    return documentFile
  }

  private fun fastFindFile(root: DocumentFile, segment: AbstractFile.Segment): DocumentFile? {
    val name = 0
    val documentId = 1
    val selection = "${DocumentsContract.Document.COLUMN_DISPLAY_NAME} = ?"
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
      root.uri,
      DocumentsContract.getDocumentId(root.uri)
    )
    val projection = arrayOf(
      DocumentsContract.Document.COLUMN_DISPLAY_NAME,
      DocumentsContract.Document.COLUMN_DOCUMENT_ID
    )
    val contentResolver = appContext.contentResolver
    val lowerCaseFilename = segment.name.toLowerCase(Locale.US)

    return contentResolver.query(
      childrenUri,
      projection,
      selection,
      arrayOf(lowerCaseFilename),
      null
    )?.use { cursor ->
      while (cursor.moveToNext()) {
        if (cursor.isNull(name)) {
          continue
        }

        val foundFileName = cursor.getString(name)
          ?: continue

        if (!foundFileName.toLowerCase(Locale.US).startsWith(lowerCaseFilename)) {
          continue
        }

        val uri = DocumentsContract.buildDocumentUriUsingTree(
          root.uri,
          cursor.getString(documentId)
        )

        return@use DocumentFile.fromSingleUri(appContext, uri)
      }

      return@use null
    }
  }

  private fun createDocumentFileFromUri(file: AbstractFile, uri: Uri, index: Int): DocumentFile? {
    val builder = uri.buildUpon()
    val segments = file.getFileSegments()

    for (i in index until segments.size) {
      builder.appendPath(segments[i].name)
    }

    return DocumentFile.fromSingleUri(appContext, builder.build())
  }

  companion object {
    private const val TAG = "ExternalFileManager"
  }
}