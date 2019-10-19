package com.github.k1rakishou.fsaf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.document_file.SnapshotDocumentFile
import com.github.k1rakishou.fsaf.extensions.copyInto
import com.github.k1rakishou.fsaf.extensions.splitIntoSegments
import com.github.k1rakishou.fsaf.file.*
import com.github.k1rakishou.fsaf.manager.BaseDirectoryManager
import com.github.k1rakishou.fsaf.manager.BaseFileManager
import com.github.k1rakishou.fsaf.manager.ExternalFileManager
import com.github.k1rakishou.fsaf.manager.RawFileManager
import com.github.k1rakishou.fsaf.util.SAFHelper
import java.io.*
import java.util.*


class FileManager(
  private val appContext: Context,
  private val baseDirectoryManager: BaseDirectoryManager,
  externalFileManager: ExternalFileManager = ExternalFileManager(
    appContext,
    ExternalFileManager.SearchMode.Fast,
    baseDirectoryManager
  ),
  rawFileManager: RawFileManager = RawFileManager()
) : BaseFileManager {
  private val managers = mutableMapOf<FileManagerId, BaseFileManager>()

  init {
    addCustomFileManager(ExternalFile.FILE_MANAGER_ID, externalFileManager)
    addCustomFileManager(RawFile.FILE_MANAGER_ID, rawFileManager)
  }

  fun addCustomFileManager(fileManagerId: FileManagerId, customManager: BaseFileManager) {
    managers[fileManagerId] = customManager
  }

  fun getExternalFileManager(): ExternalFileManager {
    return managers[ExternalFile.FILE_MANAGER_ID] as? ExternalFileManager
      ?: throw IllegalStateException("ExternalFileManager is not added")
  }

  fun getRawFileManager(): RawFileManager {
    return managers[RawFile.FILE_MANAGER_ID] as? RawFileManager
      ?: throw IllegalStateException("RawFileManager is not added")
  }

  fun baseDirectoryExists(baseDirId: String): Boolean {
    val baseDirFile = newBaseDirectoryFile(baseDirId)
    if (baseDirFile == null) {
      Log.e(TAG, "baseDirectoryExists() newBaseDirectoryFile returned null")
      return false
    }

    if (!exists(baseDirFile)) {
      return false
    }

    return true
  }

  /**
   * Creates a raw file from a path.
   * Use this method to convert a java File by this path into an AbstractFile.
   * Does not create file on the disk automatically!
   * */
  fun fromPath(path: String): RawFile {
    return fromRawFile(File(path))
  }

  /**
   * Creates RawFile from the Java File.
   * Use this method to convert a java File into an AbstractFile.
   * Does not create file on the disk automatically!
   * */
  fun fromRawFile(file: File): RawFile {
    if (file.isFile) {
      return RawFile(Root.FileRoot(file, file.name))
    }

    return RawFile(Root.DirRoot(file))
  }

  /**
   * Instantiates a new AbstractFile with the root being in the base directory with [baseDirId]
   * base directory id.
   *
   * Does not create file on the disk automatically! (just like the Java File)
   * */
  fun newBaseDirectoryFile(baseDirId: String): AbstractFile? {
    val baseDir = baseDirectoryManager.getBaseDirById(baseDirId)
    if (baseDir == null) {
      Log.e(TAG, "Base directory with id $baseDirId is not registered")
      return null
    }

    if (!SAFHelper.isTreeUri(baseDir.dirUri)) {
      Log.e(TAG, "Not a tree uri ${baseDir.dirUri}")
      return null
    }

    val treeDirectory = try {
      DocumentFile.fromTreeUri(appContext, baseDir.dirUri)
    } catch (error: Throwable) {
      Log.e(TAG, "Error while trying to create TreeDocumentFile, dirUri = ${baseDir.dirUri}")
      return null
    }

    if (treeDirectory == null) {
      return null
    }

    return ExternalFile(
        appContext,
        Root.DirRoot(CachingDocumentFile(appContext, treeDirectory))
      )
  }

  /**
   * [name] should not contain an extension
   * */
  fun createDir(baseDir: AbstractFile, name: String): AbstractFile? {
    return create(baseDir, DirectorySegment(name))
  }

  fun createFile(baseDir: AbstractFile, name: String): AbstractFile? {
    return create(baseDir, FileSegment(name))
  }

  fun create(baseDir: AbstractFile, segment: Segment): AbstractFile? {
    return create(baseDir, listOf(segment))
  }

  fun create(baseDir: AbstractFile, vararg segments: Segment): AbstractFile? {
    return create(baseDir, segments.toList())
  }

  override fun create(baseDir: AbstractFile, segments: List<Segment>): AbstractFile? {
    require(segments.isNotEmpty()) { "No segments provided" }

    return managers[baseDir.getFileManagerId()]?.create(baseDir.clone(), segments)
      ?: throw NotImplementedError("Not implemented for ${baseDir.javaClass.name}")
  }

  /**
   * Copies one file's contents into another. Does not, in any way, modify the source file
   * */
  fun copyFileContents(sourceFile: AbstractFile, destinationFile: AbstractFile): Boolean {
    return try {
      getInputStream(sourceFile)?.use { inputStream ->
        getOutputStream(destinationFile)?.use { outputStream ->
          inputStream.copyInto(outputStream)
          return@use true
        }
      } ?: false
    } catch (e: IOException) {
      Log.e(TAG, "IOException while copying one file into another", e)
      false
    }
  }

  /**
   * VERY SLOW!!!
   *
   * [recursively] will [sourceDir]'s sub directories and files as well, if false then only the
   * contents of the [sourceDir] will be copied into the [destDir].
   *
   * [updateFunc] is a callback that has two parameters:
   * 1 - How many files have been copied
   * 2 - Total amount of files to be copied
   * */
  fun copyDirectoryWithContent(
    sourceDir: AbstractFile,
    destDir: AbstractFile,
    recursively: Boolean,
    // You may want to show some kind of a progress dialog which will show how many files have
    // been copied so far and how many are left
    updateFunc: ((Int, Int) -> Unit)? = null
  ): Boolean {
    if (!exists(sourceDir)) {
      Log.e(TAG, "Source directory does not exists, path = ${sourceDir.getFullPath()}")
      return false
    }

    if (listFiles(sourceDir).isEmpty()) {
      Log.d(TAG, "Source directory is empty, nothing to copy")
      return true
    }

    if (!exists(destDir)) {
      Log.e(TAG, "Destination directory does not exists, path = ${sourceDir.getFullPath()}")
      return false
    }

    if (!isDirectory(sourceDir)) {
      Log.e(TAG, "Source directory is not a directory, path = ${sourceDir.getFullPath()}")
      return false
    }

    if (!isDirectory(destDir)) {
      Log.e(TAG, "Destination directory is not a directory, path = ${destDir.getFullPath()}")
      return false
    }

    // TODO: instead of collecting all of the files and only then start copying, do the copying
    //  file by file inside the traverse callback to save the memory
    val files = LinkedList<AbstractFile>()
    traverseDirectory(sourceDir, recursively, TraverseMode.Both) { file ->
      files += file
    }

    if (files.isEmpty()) {
      // No files, so do nothing
      Log.d(TAG, "No files were collected, nothing to copy")
      return true
    }

    val totalFilesCount = files.size
    val prefix = sourceDir.getFullPath()

    for ((currentFileIndex, file) in files.withIndex()) {
      // Holy shit this hack is so fucking disgusting and may break literally any minute.
      // If this shit breaks then blame google for providing such a retarded fucking API.

      // Basically we have a directory, let's say /123 and we want to copy all
      // of it's files into /456. So we collect every file in /123 then we iterate every
      // collected file, remove base directory prefix (/123 in this case) and recreate this
      // file with the same directory structure in another base directory (/456 in this case).
      // Let's was we have the following files:
      //
      // /123/1.txt
      // /123/111/2.txt
      // /123/222/3.txt
      //
      // After calling this method we will have these files copied into /456:
      //
      // /456/1.txt
      // /456/111/2.txt
      // /456/222/3.txt
      //
      // Such is the idea of this hack.

      val rawSegments = file.getFullPath()
        .removePrefix(prefix)
        .splitIntoSegments()

      if (rawSegments.isEmpty()) {
        // Base directory
        continue
      }

      // TODO: we probably can optimise this by filtering segments that are already a part of other
      //  segments, i.e. "/123/456" is a part of "/123/456/file.txt" so when we create
      //  "/123/456/file.txt" we also automatically create "/123/456" so we can filter it out
      //  beforehand

      val segments = if (isFile(file)) {
        // Last segment must be a file name
        rawSegments.mapIndexed { index, segmentName ->
          if (index == rawSegments.lastIndex) {
            return@mapIndexed FileSegment(segmentName)
          } else {
            return@mapIndexed DirectorySegment(segmentName)
          }
        }
      } else {
        // All segments are directory segments
        rawSegments.map { segmentName -> DirectorySegment(segmentName) }
      }

      val newFile = create(destDir, segments)
      if (newFile == null) {
        Log.e(TAG, "Couldn't create inner file with name ${getName(file)}")
        return false
      }

      if (isFile(file)) {
        if (!copyFileContents(file, newFile)) {
          Log.e(TAG, "Couldn't copy one file into another")
          return false
        }
      }

      updateFunc?.invoke(currentFileIndex, totalFilesCount)
    }

    return true
  }

  /**
   * Traverses the current directory (or also any sub-directory if [recursively] is true) and passes
   * files or directories (or both) into the [func] callback. Does not pass the directory where the
   * traversing has been started.
   *
   * [recursively] means that all of the subdirectories and their subdirectories will be traversed.
   * If false then only the [directory] dir's files will be traversed. If [traverseMode]
   * is [TraverseMode.OnlyFiles] and [recursively] is true then all the sub-directories will be
   * traversed but only the files will be passed into the callback
   * */
  fun traverseDirectory(
    directory: AbstractFile,
    recursively: Boolean,
    traverseMode: TraverseMode,
    func: (AbstractFile) -> Unit
  ) {
    if (!exists(directory)) {
      Log.e(TAG, "Source directory does not exists, path = ${directory.getFullPath()}")
      return
    }

    if (!isDirectory(directory)) {
      Log.e(TAG, "Source directory is not a directory, path = ${directory.getFullPath()}")
      return
    }

    if (listFiles(directory).isEmpty()) {
      Log.d(TAG, "Source directory is empty, nothing to copy")
      return
    }

    if (!recursively) {
      listFiles(directory).forEach { file -> func(file) }
      return
    }

    val queue = LinkedList<AbstractFile>()
    queue.offer(directory)

    val directoryFullPath = directory.getFullPath()

    // Collect all of the inner files in the source directory
    while (queue.isNotEmpty()) {
      val file = queue.poll()
        ?: break

      when {
        isDirectory(file) -> {
          val innerFiles = listFiles(file)
          if (traverseMode.includeDirs()) {
            if (file.getFullPath() != directoryFullPath) {
              func(file)
            }
          }

          innerFiles.forEach { innerFile ->
            queue.offer(innerFile)
          }
        }
        isFile(file) -> {
          if (traverseMode.includeFiles()) {
            if (file.getFullPath() != directoryFullPath) {
              func(file)
            }
          }
        }
        else -> throw IllegalArgumentException(
          "traverseMode does not include neither dir now files, " +
            "traverseMode = ${traverseMode.name}"
        )
      }
    }
  }

  override fun exists(file: AbstractFile): Boolean {
    return managers[file.getFileManagerId()]?.exists(file)
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )
  }

  override fun isFile(file: AbstractFile): Boolean {
    return managers[file.getFileManagerId()]?.isFile(file)
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )
  }

  override fun isDirectory(file: AbstractFile): Boolean {
    return managers[file.getFileManagerId()]?.isDirectory(file)
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )
  }

  override fun canRead(file: AbstractFile): Boolean {
    return managers[file.getFileManagerId()]?.canRead(file)
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )
  }

  override fun canWrite(file: AbstractFile): Boolean {
    return managers[file.getFileManagerId()]?.canWrite(file)
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )
  }

  override fun getSegmentNames(file: AbstractFile): List<String> {
    return managers[file.getFileManagerId()]?.getSegmentNames(file)
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )
  }

  override fun delete(file: AbstractFile): Boolean {
    return managers[file.getFileManagerId()]?.delete(file)
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )
  }

  override fun deleteContent(dir: AbstractFile) {
    managers[dir.getFileManagerId()]?.deleteContent(dir)
      ?: throw NotImplementedError(
        "Not implemented for ${dir.javaClass.name}, " +
          "fileManagerId = ${dir.getFileManagerId()}"
      )
  }

  override fun getInputStream(file: AbstractFile): InputStream? {
    val manager = managers[file.getFileManagerId()]
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )

    return manager.getInputStream(file)
  }

  override fun getOutputStream(file: AbstractFile): OutputStream? {
    val manager = managers[file.getFileManagerId()]
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )

    return manager.getOutputStream(file)
  }

  override fun getName(file: AbstractFile): String {
    return managers[file.getFileManagerId()]?.getName(file)
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )
  }

  override fun findFile(dir: AbstractFile, fileName: String): AbstractFile? {
    val manager = managers[dir.getFileManagerId()]
      ?: throw NotImplementedError(
        "Not implemented for ${dir.javaClass.name}, " +
          "fileManagerId = ${dir.getFileManagerId()}"
      )

    return manager.findFile(dir, fileName)
  }

  override fun getLength(file: AbstractFile): Long {
    return managers[file.getFileManagerId()]?.getLength(file)
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )
  }

  override fun listFiles(dir: AbstractFile): List<AbstractFile> {
    return managers[dir.getFileManagerId()]?.listFiles(dir)
      ?: throw NotImplementedError(
        "Not implemented for ${dir.javaClass.name}, " +
          "fileManagerId = ${dir.getFileManagerId()}"
      )
  }

  override fun listSnapshotFiles(dir: AbstractFile, recursively: Boolean): List<AbstractFile> {
    return managers[ExternalFile.FILE_MANAGER_ID]?.listSnapshotFiles(dir, recursively)
      ?: throw NotImplementedError("Only implemented for ExternalFiles!")
  }

  override fun lastModified(file: AbstractFile): Long {
    return managers[file.getFileManagerId()]?.lastModified(file)
      ?: throw NotImplementedError(
        "Not implemented for ${file.javaClass.name}, " +
          "fileManagerId = ${file.getFileManagerId()}"
      )
  }

  override fun <T> withFileDescriptor(
    file: AbstractFile,
    fileDescriptorMode: FileDescriptorMode,
    func: (FileDescriptor) -> T?
  ): T? {
    return managers[file.getFileManagerId()]?.withFileDescriptor(file, fileDescriptorMode, func)
      ?: throw NotImplementedError("Not implemented for ${file.javaClass.name}")
  }

  fun snapshot(dir: ExternalFile, includeSubDirs: Boolean = false, func: () -> Unit) {

    fun combine(docFile: SnapshotDocumentFile): Pair<ExternalFile, SnapshotDocumentFile>? {
      if (docFile.name == null) {
        return null
      }

      val root = if (docFile.isDirectory) {
        Root.DirRoot(docFile)
      } else {
        Root.FileRoot(docFile, docFile.name!!)
      }

      return Pair(
        ExternalFile(appContext, root as Root<CachingDocumentFile>),
        docFile
      )
    }

    val externalFileManager = getExternalFileManager()
    val directories = arrayListOf<ExternalFile>().apply { this.ensureCapacity(16) }

    traverseDirectory(dir, includeSubDirs, TraverseMode.OnlyDirs) { file ->
      directories += file as ExternalFile
    }

    for (directory in directories) {
      val parentUri = directory.getFileRoot<CachingDocumentFile>().holder.uri
      val isBaseDir = baseDirectoryManager.isBaseDir(directory)

      val documentFiles = SAFHelper.listFilesFast(appContext, parentUri, isBaseDir)
      if (documentFiles.isNotEmpty()) {
        val pairs = documentFiles.mapNotNull { docFile -> combine(docFile) }
        externalFileManager.cacheFiles(pairs)
      }
    }

    try {
      func()
    } finally {
      externalFileManager.uncacheFilesInSubTree(dir)
    }
  }

  private fun toDocumentFile(uri: Uri): CachingDocumentFile? {
    try {
      val file = try {
        val docTreeFile = DocumentFile.fromTreeUri(appContext, uri)

        // FIXME: Damn this is hacky as fuck and I don't even know whether it works 100% or not
        //  and it constantly give me warnings like:
        //  "W/DocumentFile: Failed query: java.lang.UnsupportedOperationException: Unsupported Uri"
        //  but still works (!!!)
        if (docTreeFile != null && isBogusTreeUri(uri, docTreeFile.uri)) {
          DocumentFile.fromSingleUri(appContext, uri)
        } else {
          docTreeFile
        }

      } catch (ignored: IllegalArgumentException) {
        DocumentFile.fromSingleUri(appContext, uri)
      }

      if (file == null) {
        Log.e(TAG, "Couldn't convert uri ${uri} into a DocumentFile")
        return null
      }

      if (!file.exists()) {
        return null
      }

      return CachingDocumentFile(appContext, file)
    } catch (e: IllegalArgumentException) {
      Log.e(TAG, "Provided uri is neither a treeUri nor singleUri, uri = $uri")
      return null
    }
  }

  /**
   * When we try to pass a bogus uri to DocumentFile.fromTreeUri it will return us the base tree uri.
   * This means that it cannot be used as a tree uri so we need to use DocumentFile.fromSingleUri
   * instead.
   * */
  private fun isBogusTreeUri(uri: Uri, docTreeUri: Uri): Boolean {
    if (uri.toString() == docTreeUri.toString()) {
      return false
    }

    val docUri = DocumentsContract.buildDocumentUriUsingTree(
      uri,
      DocumentsContract.getDocumentId(uri)
    )

    return docUri.toString() == docTreeUri.toString()
  }

  enum class TraverseMode {
    OnlyFiles,
    OnlyDirs,
    Both;

    fun includeDirs(): Boolean = this == OnlyDirs || this == Both
    fun includeFiles(): Boolean = this == OnlyFiles || this == Both
  }

  companion object {
    private const val TAG = "FileManager"
  }
}