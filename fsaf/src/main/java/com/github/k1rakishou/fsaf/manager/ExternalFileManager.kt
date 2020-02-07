package com.github.k1rakishou.fsaf.manager

import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.github.k1rakishou.fsaf.BadPathSymbolResolutionStrategy
import com.github.k1rakishou.fsaf.FastFileSearchTree
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.document_file.SnapshotDocumentFile
import com.github.k1rakishou.fsaf.extensions.getMimeFromFilename
import com.github.k1rakishou.fsaf.extensions.splitIntoSegments
import com.github.k1rakishou.fsaf.file.*
import com.github.k1rakishou.fsaf.manager.base_directory.DirectoryManager
import com.github.k1rakishou.fsaf.util.SAFHelper
import java.io.FileDescriptor
import java.io.InputStream
import java.io.OutputStream

/**
 * Provide an API to work with SAF files
 * */
class ExternalFileManager(
  private val appContext: Context,
  private val badPathSymbolResolutionStrategy: BadPathSymbolResolutionStrategy,
  private val directoryManager: DirectoryManager
) : BaseFileManager {
  private val mimeTypeMap = MimeTypeMap.getSingleton()
  private val fastFileSearchTree: FastFileSearchTree<CachingDocumentFile> = FastFileSearchTree()

  // For tests
  fun getFastFileSearchTree() = fastFileSearchTree

  /**
   * Caches multiple files associated with their SnapshotDocumentFile into FastFileSearchTree
   * */
  fun cacheFiles(files: List<Pair<ExternalFile, SnapshotDocumentFile>>) {
    for ((externalFile, snapshotDocFile) in files) {
      cacheFile(externalFile, snapshotDocFile)
    }
  }

  private fun cacheFile(
    externalFile: ExternalFile,
    snapshotDocFile: SnapshotDocumentFile
  ) {
    val segments = externalFile.getFullPath().splitIntoSegments()
    if (segments.isEmpty()) {
      Log.e(TAG, "cacheFile() splitIntoSegments() returned an empty list")
      return
    }

    check(fastFileSearchTree.insertSegments(segments, snapshotDocFile)) {
      "cacheFile() Couldn't insert new segments into the tree"
    }
  }

  /**
   * Removes a whole sub-tree from the FastFileSearchTree.
   * Lets say there is a sub-tree "/1/2/3/<XXX>" which may contain a lot of cached files. After
   * calling this method all of the <XXX> files will be removed from the FastFileSearchTree and
   * only the "/1/2/3/" will be left
   * */
  fun uncacheFilesInSubTree(file: AbstractFile) {
    val segments = file.getFullPath().splitIntoSegments()
    if (segments.isEmpty()) {
      Log.e(TAG, "uncacheFile.splitIntoSegments() returned empty list")
      return
    }

    check(fastFileSearchTree.removeSegments(segments)) {
      "uncacheFilesInSubTree() Couldn't remove sub-tree"
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun create(baseDir: AbstractFile, segments: List<Segment>): ExternalFile? {
    val root = baseDir.getFileRoot<CachingDocumentFile>()
    check(root !is Root.FileRoot) {
      "create() root is already FileRoot, cannot append anything anymore"
    }

    if (segments.isEmpty()) {
      if (exists(baseDir)) {
        return baseDir as ExternalFile
      }

      throw IllegalStateException("create() Segments are empty and " +
        "baseDir (${baseDir.getFullPath()}) does not exist")
    }

    var newFile: CachingDocumentFile? = null

    for (segment in segments) {
      val innerFile = newFile ?: root.holder

      val mimeType = if (segment.isFileName) {
        mimeTypeMap.getMimeFromFilename(segment.name)
      } else {
        DocumentsContract.Document.MIME_TYPE_DIR
      }

      // Check whether this segment already exists on the disk
      val foundFile = SAFHelper.findCachingFile(
        appContext,
        innerFile.uri(),
        segment.name,
        directoryManager.isBaseDir(innerFile)
      )

      if (foundFile != null) {
        if (foundFile.isFile()) {
          // Ignore any left segments (which we shouldn't have) after encountering fileName
          // segment
          return ExternalFile(
            appContext,
            badPathSymbolResolutionStrategy,
            Root.FileRoot(foundFile, segment.name)
          )
        } else {
          newFile = foundFile
        }

        continue
      }

      val newUri = DocumentsContract.createDocument(
        appContext.contentResolver,
        innerFile.uri(),
        mimeType,
        segment.name
      )

      if (newUri == null) {
        Log.e(
          TAG, "create() DocumentsContract.createDocument returned null, " +
            "file.uri = ${innerFile.uri()}, segment.name = ${segment.name}"
        )
        return null
      }

      val createdFile = if (segment.isFileName) {
        DocumentFile.fromSingleUri(appContext, newUri)
      } else {
        DocumentFile.fromTreeUri(appContext, newUri)
      }

      if (createdFile == null) {
        Log.e(TAG, "create() Couldn't create DocumentFile out of uri, directoryUri = ${newUri}")
        return null
      }

      if (segment.isFileName) {
        // Ignore any left segments (which we shouldn't have) after encountering fileName
        // segment

        val newRoot = Root.FileRoot(
          CachingDocumentFile(appContext, createdFile),
          segment.name
        )

        return ExternalFile(
          appContext,
          badPathSymbolResolutionStrategy,
          newRoot
        )
      } else {
        newFile = CachingDocumentFile(appContext, createdFile)
      }
    }

    if (newFile == null) {
      Log.e(TAG, "create() result file is null")
      return null
    }

    val lastSegment = segments.last()
    val isLastSegmentFilename = lastSegment.isFileName

    val newRoot = if (isLastSegmentFilename) {
      Root.FileRoot(newFile, lastSegment.name)
    } else {
      Root.DirRoot(newFile)
    }

    return ExternalFile(
      appContext,
      badPathSymbolResolutionStrategy,
      newRoot
    )
  }

  override fun exists(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.exists() ?: false

  override fun isFile(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.isFile() ?: false

  override fun isDirectory(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.isDirectory() ?: false

  override fun canRead(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.canRead() ?: false

  override fun canWrite(file: AbstractFile): Boolean =
    toDocumentFile(file.clone())?.canWrite() ?: false

  override fun getSegmentNames(file: AbstractFile): List<String> {
    return file.getFullPath().splitIntoSegments()
  }

  override fun delete(file: AbstractFile): Boolean {
    if (!exists(file)) {
      return true
    }

    val documentFile = toDocumentFile(file.clone())
      ?: return true

    val segments = file
      .toString()
      .splitIntoSegments()
    check(segments.isNotEmpty())

    if (!fastFileSearchTree.removeSegments(segments)) {
      Log.e(TAG, "delete(), Couldn't remove segments $segments from fastFileSearchTree")
      return false
    }

    return documentFile.delete()
  }

  override fun deleteContent(dir: AbstractFile): Boolean {
    val documentFile = toDocumentFile(dir.clone())
      ?: return false

    if (!documentFile.isDirectory()) {
      Log.e(TAG, "deleteContent() Only directories are supported (files can't have contents anyway)")
      return false
    }

    val filesInDirectory = SAFHelper.listFilesFast(
      appContext,
      documentFile.uri(),
      directoryManager.isBaseDir(documentFile)
    )

    for (fileInDirectory in filesInDirectory) {
      val segments = fileInDirectory.uri().toString().splitIntoSegments()

      if (!fastFileSearchTree.removeSegments(segments)) {
        Log.e(TAG, "deleteContent() Couldn't remove segments $segments from fastFileSearchTree")
        return false
      }
    }

    // This may delete only some files and leave other but at least you will know that something
    // went wrong (just don't forget to check the result)
    return filesInDirectory.all { file -> file.delete() }
  }

  override fun getInputStream(file: AbstractFile): InputStream? {
    val contentResolver = appContext.contentResolver
    val documentFile = toDocumentFile(file.clone())

    if (documentFile == null) {
      Log.e(TAG, "getInputStream() toDocumentFile() returned null")
      return null
    }

    if (!documentFile.exists()) {
      Log.e(TAG, "getInputStream() documentFile does not exist, uri = ${documentFile.uri()}")
      return null
    }

    if (!documentFile.isFile()) {
      Log.e(TAG, "getInputStream() documentFile is not a file, uri = ${documentFile.uri()}")
      return null
    }

    if (!documentFile.canRead()) {
      Log.e(TAG, "getInputStream() cannot read from documentFile, uri = ${documentFile.uri()}")
      return null
    }

    return contentResolver.openInputStream(documentFile.uri())
  }

  override fun getOutputStream(file: AbstractFile): OutputStream? {
    val contentResolver = appContext.contentResolver
    val documentFile = toDocumentFile(file.clone())

    if (documentFile == null) {
      Log.e(TAG, "getOutputStream() toDocumentFile() returned null")
      return null
    }

    if (!documentFile.exists()) {
      Log.e(TAG, "getOutputStream() documentFile does not exist, uri = ${documentFile.uri()}")
      return null
    }

    if (!documentFile.isFile()) {
      Log.e(TAG, "getOutputStream() documentFile is not a file, uri = ${documentFile.uri()}")
      return null
    }

    if (!documentFile.canWrite()) {
      Log.e(TAG, "getOutputStream() cannot write to documentFile, uri = ${documentFile.uri()}")
      return null
    }

    return contentResolver.openOutputStream(documentFile.uri())
  }

  override fun getName(file: AbstractFile): String? {
    val segments = file.getFileSegments()
    if (segments.isNotEmpty() && segments.last().isFileName) {
      return segments.last().name
    }

    val documentFile = toDocumentFile(file.clone())
      ?: throw IllegalStateException("getName() toDocumentFile() returned null")

    return documentFile.name()
  }

  override fun findFile(dir: AbstractFile, fileName: String): ExternalFile? {
    val root = dir.getFileRoot<CachingDocumentFile>()
    val segments = dir.getFileSegments()

    check(root !is Root.FileRoot) { "findFile() Cannot use FileRoot as directory" }
    if (segments.isNotEmpty()) {
      check(!segments.last().isFileName) { "findFile() Cannot do search when last segment is file" }
    }

    val parentDocFile = if (segments.isNotEmpty()) {
      SAFHelper.findDeepFile(
        appContext,
        root.holder.uri(),
        segments,
        directoryManager
      )
    } else {
      val docFile = DocumentFile.fromSingleUri(appContext, root.holder.uri())
      if (docFile != null) {
        CachingDocumentFile(appContext, docFile)
      } else {
        null
      }
    }

    if (parentDocFile == null) {
      return null
    }

    val cachingDocFile = SAFHelper.findCachingFile(
      appContext,
      parentDocFile.uri(),
      fileName,
      directoryManager.isBaseDir(parentDocFile)
    )

    if (cachingDocFile == null || !cachingDocFile.exists()) {
      return null
    }

    val innerRoot = if (cachingDocFile.isFile()) {
      Root.FileRoot(cachingDocFile, cachingDocFile.name()!!)
    } else {
      Root.DirRoot(cachingDocFile)
    }

    return ExternalFile(
      appContext,
      badPathSymbolResolutionStrategy,
      innerRoot
    )
  }

  override fun getLength(file: AbstractFile): Long = toDocumentFile(file.clone())?.length() ?: -1L

  override fun listFiles(dir: AbstractFile): List<ExternalFile> {
    val root = dir.getFileRoot<CachingDocumentFile>()
    check(root !is Root.FileRoot) { "listFiles() Cannot use listFiles with FileRoot" }

    val docFile = toDocumentFile(dir.clone())
      ?: return emptyList()

    return SAFHelper.listFilesFast(appContext, docFile.uri(), directoryManager.isBaseDir(dir))
      .map { snapshotFile ->
        val file = ExternalFile(
          appContext,
          badPathSymbolResolutionStrategy,
          Root.DirRoot(snapshotFile)
        )

        cacheFile(file, snapshotFile)
        return@map file
      }
  }

  override fun listSnapshotFiles(dir: AbstractFile, recursively: Boolean): List<AbstractFile> {
    val root = dir.getFileRoot<CachingDocumentFile>()
    check(root !is Root.FileRoot) { "listSnapshotFiles() Cannot use listFiles with FileRoot" }

    val resultList = ArrayList<SnapshotDocumentFile>(32)

    fastFileSearchTree.visitEverySegmentAfterPath(
      dir.getFullPath().splitIntoSegments(),
      recursively
    ) { node ->
      node.getNodeValue()?.let { resultList += it as SnapshotDocumentFile}
    }

    return resultList.map { snapshotFile ->
      ExternalFile(
        appContext,
        badPathSymbolResolutionStrategy,
        Root.DirRoot(snapshotFile)
      )
    }
  }

  override fun lastModified(file: AbstractFile): Long {
    return toDocumentFile(file.clone())?.lastModified() ?: 0L
  }

  override fun <T> withFileDescriptor(
    file: AbstractFile,
    fileDescriptorMode: FileDescriptorMode,
    func: (FileDescriptor) -> T?
  ): T? {
    if (isDirectory(file)) {
      Log.e(TAG, "withFileDescriptor() only works with files ")
      return null
    }

    return getParcelFileDescriptor(file, fileDescriptorMode)
      ?.use { pfd -> func(pfd.fileDescriptor) }
      ?: throw IllegalStateException(
        "withFileDescriptor() Could not get ParcelFileDescriptor " +
          "from root with uri = ${file.getFileRoot<DocumentFile>().holder.uri}"
      )
  }

  private fun toDocumentFile(file: AbstractFile): CachingDocumentFile? {
    // First of all check whether we already have SnapshotDocumentFile in the Tree
    val cachedFile = fastFileSearchTree.findSegment(
      file.getFullPath().splitIntoSegments()
    )

    if (cachedFile != null) {
      return cachedFile
    }

    val segments = file.getFileSegments()
    if (segments.isEmpty()) {
      return file.getFileRoot<CachingDocumentFile>().holder
    }

    val parentUri = file.getFileRoot<CachingDocumentFile>()
      .holder
      .uri()

    val notCachedFile = SAFHelper.findDeepFile(
      appContext,
      parentUri,
      segments,
      directoryManager
    )

    if (notCachedFile != null) {
      val result = fastFileSearchTree.insertSegments(
        notCachedFile.uri().toString().splitIntoSegments(),
        notCachedFile
      )

      check(result) {
        "toDocumentFile() Something went wrong when trying to " +
          "insert a new file into fastFileSearchTree"
      }
    }

    return notCachedFile
  }

  private fun getParcelFileDescriptor(
    file: AbstractFile,
    fileDescriptorMode: FileDescriptorMode
  ): ParcelFileDescriptor? {
    return appContext.contentResolver.openFileDescriptor(
      file.getFileRoot<CachingDocumentFile>().holder.uri(),
      fileDescriptorMode.mode
    )
  }

  companion object {
    private const val TAG = "ExternalFileManager"
  }
}