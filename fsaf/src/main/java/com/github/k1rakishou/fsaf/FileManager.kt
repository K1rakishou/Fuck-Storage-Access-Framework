package com.github.k1rakishou.fsaf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.document_file.SnapshotDocumentFile
import com.github.k1rakishou.fsaf.extensions.copyInto
import com.github.k1rakishou.fsaf.extensions.extension
import com.github.k1rakishou.fsaf.extensions.splitIntoSegments
import com.github.k1rakishou.fsaf.file.*
import com.github.k1rakishou.fsaf.manager.BaseFileManager
import com.github.k1rakishou.fsaf.manager.ExternalFileManager
import com.github.k1rakishou.fsaf.manager.RawFileManager
import com.github.k1rakishou.fsaf.manager.base_directory.BaseDirectory
import com.github.k1rakishou.fsaf.manager.base_directory.DirectoryManager
import com.github.k1rakishou.fsaf.util.SAFHelper
import java.io.*
import java.util.*


class FileManager(
  private val appContext: Context,
  private val directoryManager: DirectoryManager = DirectoryManager()
) : BaseFileManager {
  private val managers = mutableMapOf<FileManagerId, BaseFileManager>()
  private val externalFileManager: BaseFileManager = ExternalFileManager(
    appContext,
    directoryManager
  )
  private val rawFileManager: BaseFileManager = RawFileManager()

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

  /**
   * Creates a raw file from a path.
   * Use this method to convert a java File by this path into an AbstractFile.
   * Does not create file on the disk automatically!
   * */
  fun fromPath(path: String): RawFile {
    return fromRawFile(File(path))
  }

  /**
   * Create an external file from Uri.
   * Use this method to convert external file uri (file that may be located at sd card) into an
   * AbstractFile. If a file does not exist null is returned.
   * Does not create file on the disk automatically!
   * */
  fun fromUri(uri: Uri): ExternalFile {
    val documentFile = toDocumentFile(uri)
      ?: throw IllegalStateException("toDocumentFile returned null, uri = $uri")

    return if (documentFile.isFile()) {
      val filename = documentFile.name()
        ?: throw IllegalStateException("fromUri() queryTreeName() returned null")

      ExternalFile(appContext, Root.FileRoot(documentFile, filename))
    } else {
      ExternalFile(appContext, Root.DirRoot(documentFile))
    }
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

  inline fun <reified T> registerBaseDir(baseDirectory: BaseDirectory) {
    registerBaseDir(T::class.java, baseDirectory)
  }

  fun registerBaseDir(clazz: Class<*>, baseDirectory: BaseDirectory) {
    directoryManager.registerBaseDir(clazz, baseDirectory)
  }

  inline fun <reified T> unregisterBaseDir() {
    unregisterBaseDir(T::class.java)
  }

  fun unregisterBaseDir(clazz: Class<*>) {
    directoryManager.unregisterBaseDir(clazz)
  }

  inline fun <reified T> newBaseDirectoryFile(): AbstractFile? {
    return newBaseDirectoryFile(T::class.java)
  }

  /**
   * Instantiates a new AbstractFile with the root being in the base directory with [clazz]
   * base directory id.
   *
   * Does not create file on the disk automatically! (just like the Java File)
   * */
  fun newBaseDirectoryFile(clazz: Class<*>): AbstractFile? {
    val baseDir = directoryManager.getBaseDirByClass(clazz)
    if (baseDir == null) {
      Log.e(TAG, "Base directory with class $clazz is not registered")
      return null
    }

    val dirUri = baseDir.getDirUri()
    if (dirUri != null) {
      if (!SAFHelper.isTreeUri(baseDir)) {
        Log.e(TAG, "Not a tree uri ${baseDir.dirPath()}")
        return null
      }

      val treeDirectory = try {
        DocumentFile.fromTreeUri(appContext, dirUri)
      } catch (error: Throwable) {
        Log.e(TAG, "Error while trying to create TreeDocumentFile, dirUri = ${baseDir.dirPath()}")
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

    val dirFile = baseDir.getDirFile()
    if (dirFile != null) {
      return RawFile(
        Root.DirRoot(dirFile)
      )
    }

    throw IllegalStateException("${baseDir.javaClass.name} is not supported!")
  }

  inline fun <reified T> baseDirectoryExists(): Boolean {
    return baseDirectoryExists(T::class.java)
  }

  fun baseDirectoryExists(clazz: Class<*>): Boolean {
    val baseDirFile = newBaseDirectoryFile(clazz as Class<BaseDirectory>)
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
   * Similar to [AbstractFile.cloneUnsafe]. Use only if you are sure in your input.
   * */
  fun createUnsafe(baseDir: AbstractFile, path: String): AbstractFile? {
    val segmentStrings = path.splitIntoSegments()

    val segments = segmentStrings.mapIndexed { index, segmentString ->
      if (index == segmentStrings.lastIndex && segmentString.extension() != null) {
        return@mapIndexed FileSegment(segmentString)
      }

      return@mapIndexed DirectorySegment(segmentString)
    }

    return create(baseDir, segments)
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
    if (segments.isEmpty() && exists(baseDir)) {
      return baseDir
    }

    val segmentsToAppend = if (segments.isEmpty()) {
      if (baseDir.getFileSegments().isEmpty()) {
        emptyList()
      } else {
        baseDir.getFileSegments()
      }
    } else {
      segments
    }

    val manager = managers[baseDir.getFileManagerId()]
      ?: throw NotImplementedError(
        "Not implemented for ${baseDir.javaClass.name}, " +
          "fileManagerId = ${baseDir.getFileManagerId()}"
      )

    return manager.create(baseDir.clone(), segmentsToAppend)
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
   * [recursively] will [sourceDir]'s sub directories and files as well, if false then only the
   * contents of the [sourceDir] will be copied into the [destDir].
   *
   * [updateFunc] is a callback that has two parameters:
   * 1 - How many files have been copied
   * 2 - Total amount of files to be copied
   *
   * if [updateFunc] returns true that means that we need to cancel the copying. If [updateFunc] is
   * not provided then there is no way to cancel this method
   * */
  fun copyDirectoryWithContent(
    sourceDir: AbstractFile,
    destDir: AbstractFile,
    recursively: Boolean,
    // You may want to show some kind of a progress dialog which will show how many files have
    // been copied so far and how many are left
    updateFunc: ((Int, Int) -> Boolean)? = null
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

      if (directoryManager.isBaseDir(file)) {
        // Base directory
        continue
      }

      val rawSegments = file.getFullPath()
        .removePrefix(prefix)
        .splitIntoSegments()

      if (rawSegments.isEmpty()) {
        // The sourceDir itself
        continue
      }

      // TODO: we probably can optimise this by filtering segments that are already a part of other
      //  segments, i.e. "/123/456" is a part of "/123/456/file.txt" so when we create
      //  "/123/456/file.txt" we also automatically create "/123/456" so we can filter it out
      //  beforehand thus reducing the needed amount of file operations

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

      val result = updateFunc?.invoke(currentFileIndex, totalFilesCount) ?: false
      if (result) {
        Log.e(TAG, "Cancelled")
        return true
      }
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
    val manager = managers[file.getFileManagerId()]
      ?: throw NotImplementedError("Not implemented for ${file.javaClass.name}")

    return manager.withFileDescriptor(file, fileDescriptorMode, func)
  }

  private fun combine(docFile: SnapshotDocumentFile): Pair<ExternalFile, SnapshotDocumentFile>? {
    if (docFile.name() == null) {
      return null
    }

    val root = if (docFile.isDirectory()) {
      Root.DirRoot(docFile)
    } else {
      Root.FileRoot(docFile, docFile.name()!!)
    }

    return Pair(
      ExternalFile(appContext, root as Root<CachingDocumentFile>),
      docFile
    )
  }

  fun <T> withSnapshot(dir: AbstractFile, includeSubDirs: Boolean = false, func: () -> T?): T? {
    createSnapshot(dir, includeSubDirs)

    try {
      return func()
    } finally {
      releaseSnapshot(dir)
    }
  }

  /**
   * Manually create a snapshot of a directory (with sub directories if [includeSubDirs] is true).
   * Snapshot is only created for ExternalFiles! RawFiles are not supported and snapshot won't be
   * created. This method exists to make bulk Storage Access Framework operations faster!
   *
   * !!!DON'T FORGET TO RELEASE THE SNAPSHOT ONCE YOU ARE DONE WITH IT!!!
   * Or just use [withSnapshot] instead which does it for you.
   *
   * Usually you should just follow this scheme:
   * You have a directory with lots of files and maybe even sub directories with their own files.
   * You want to search for multiple files get their names, sizes check whether they exist or not.
   * This is an ideal case for making a snapshot. It will load the whole directory with sub
   * directories and files into memory pre-load all of the files properties (like size/name etc)
   * so when you do some file operation (like exists()) it will be way faster than a normal
   * operation non-snapshot operation. After you are done - release the snapshot or use
   * [withSnapshot] which will do it for you.
   * */
  fun createSnapshot(dir: AbstractFile, includeSubDirs: Boolean = false) {
    if (dir is ExternalFile) {
      val externalFileManager = getExternalFileManager()
      val directories = arrayListOf<ExternalFile>().apply { this.ensureCapacity(16) }

      traverseDirectory(dir, includeSubDirs, TraverseMode.OnlyDirs) { file ->
        directories += file as ExternalFile
      }

      for (directory in directories) {
        val parentUri = directory.getFileRoot<CachingDocumentFile>().holder.uri()
        val isBaseDir = directoryManager.isBaseDir(directory)

        val documentFiles = SAFHelper.listFilesFast(appContext, parentUri, isBaseDir)
        if (documentFiles.isNotEmpty()) {
          val pairs = documentFiles.mapNotNull { docFile -> combine(docFile) }
          externalFileManager.cacheFiles(pairs)
        }
      }
    } else {
      Log.d(TAG, "createSnapshot called for RawFile backed directory. Snapshot was not created.")
    }
  }

  /**
   * Removes the whole sub tree from the FastFileSearchTree with all of the cached files
   * */
  fun releaseSnapshot(dir: AbstractFile) {
    if (dir is ExternalFile) {
      val externalFileManager = getExternalFileManager()
      externalFileManager.uncacheFilesInSubTree(dir)
    } else {
      Log.d(TAG, "createSnapshot called for RawFile backed directory. Snapshot was not created.")
    }
  }

  private fun toDocumentFile(uri: Uri): CachingDocumentFile? {
    try {
      val file = try {
        val docTreeFile = DocumentFile.fromTreeUri(appContext, uri)

        // FIXME: Damn this is hacky as fuck and I don't even know whether it works 100% or not
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