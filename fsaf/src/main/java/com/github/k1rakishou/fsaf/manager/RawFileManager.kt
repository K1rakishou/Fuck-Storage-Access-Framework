package com.github.k1rakishou.fsaf.manager

import android.util.Log
import com.github.k1rakishou.fsaf.extensions.appendMany
import com.github.k1rakishou.fsaf.file.*
import com.github.k1rakishou.fsaf.util.FSAFUtils
import java.io.*

class RawFileManager : BaseFileManager {

  override fun create(baseDir: AbstractFile, segments: List<Segment>): RawFile? {
    val root = baseDir.getFileRoot<File>()
    check(root !is Root.FileRoot) {
      "create() root is already FileRoot, cannot append anything anymore"
    }

    check(segments.isNotEmpty()) { "root has already been created" }

    var newFile = root.holder
    for (segment in segments) {
      newFile = File(newFile, segment.name)

      if (segment.isFileName) {
        if (!newFile.exists() && !newFile.createNewFile()) {
          Log.e(TAG, "create() Could not create a new file, path = " + newFile.absolutePath)
          return null
        }
      } else {
        if (!newFile.exists() && !newFile.mkdir()) {
          Log.e(TAG, "create() Could not create a new directory, path = " + newFile.absolutePath)
          return null
        }
      }

      if (segment.isFileName) {
        return RawFile(Root.FileRoot(newFile, segment.name))
      }
    }

    return RawFile(Root.DirRoot(newFile))
  }

  override fun exists(file: AbstractFile): Boolean =
    toFile(file.clone()).exists()

  override fun isFile(file: AbstractFile): Boolean =
    toFile(file.clone()).isFile

  override fun isDirectory(file: AbstractFile): Boolean =
    toFile(file.clone()).isDirectory

  override fun canRead(file: AbstractFile): Boolean =
    toFile(file.clone()).canRead()

  override fun canWrite(file: AbstractFile): Boolean =
    toFile(file.clone()).canWrite()

  override fun getSegmentNames(file: AbstractFile): List<String> {
    return file.getFullPath()
      .split(File.separatorChar)
  }

  override fun delete(file: AbstractFile): Boolean {
    val javaFile = toFile(file.clone())
    if (javaFile.isFile) {
      return javaFile.delete()
    }

    return FSAFUtils.deleteDirectory(javaFile, true)
  }

  override fun deleteContent(dir: AbstractFile) {
    val directory = toFile(dir.clone())
    if (!directory.isDirectory) {
      Log.e(TAG, "deleteContent() Only directories are supported (files can't have contents anyway)")
      return
    }

    FSAFUtils.deleteDirectory(directory, false)
  }

  override fun getInputStream(file: AbstractFile): InputStream? {
    val clonedFile = toFile(file.clone())

    if (!clonedFile.exists()) {
      Log.e(TAG, "getInputStream() file does not exist, path = ${clonedFile.absolutePath}")
      return null
    }

    if (!clonedFile.isFile) {
      Log.e(TAG, "getInputStream() file is not a file, path = ${clonedFile.absolutePath}")
      return null
    }

    if (!clonedFile.canRead()) {
      Log.e(TAG, "getInputStream() cannot read from file, path = ${clonedFile.absolutePath}")
      return null
    }

    return clonedFile.inputStream()
  }

  override fun getOutputStream(file: AbstractFile): OutputStream? {
    val clonedFile = toFile(file.clone())

    if (!clonedFile.exists()) {
      Log.e(TAG, "getOutputStream() file does not exist, path = ${clonedFile.absolutePath}")
      return null
    }

    if (!clonedFile.isFile) {
      Log.e(TAG, "getOutputStream() file is not a file, path = ${clonedFile.absolutePath}")
      return null
    }

    if (!clonedFile.canWrite()) {
      Log.e(TAG, "getOutputStream() cannot write to file, path = ${clonedFile.absolutePath}")
      return null
    }

    return clonedFile.outputStream()
  }

  override fun getName(file: AbstractFile): String? {
    return toFile(file.clone()).name
  }

  override fun findFile(dir: AbstractFile, fileName: String): RawFile? {
    val root = dir.getFileRoot<File>()
    val segments = dir.getFileSegments()
    check(root !is Root.FileRoot) { "findFile() Cannot use FileRoot as directory" }

    if (segments.isNotEmpty()) {
      check(!segments.last().isFileName) { "findFile() Cannot do search when last segment is file" }
    }

    var copy = File(root.holder.absolutePath)
    if (segments.isNotEmpty()) {
      copy = copy.appendMany(segments.map { segment -> segment.name })
    }

    val resultFile = File(copy.absolutePath, fileName)
    if (!resultFile.exists()) {
      return null
    }

    val newRoot = if (resultFile.isFile) {
      Root.FileRoot(resultFile, resultFile.name)
    } else {
      Root.DirRoot(resultFile)
    }

    return RawFile(newRoot)
  }

  override fun getLength(file: AbstractFile): Long = toFile(file.clone()).length()

  override fun lastModified(file: AbstractFile): Long {
    return toFile(file.clone()).lastModified()
  }

  override fun listFiles(dir: AbstractFile): List<RawFile> {
    val root = dir.getFileRoot<File>()
    check(root !is Root.FileRoot) { "listFiles() Cannot use listFiles with FileRoot" }

    return toFile(dir.clone())
      .listFiles()
      ?.map { file -> RawFile(Root.DirRoot(file)) }
      ?: emptyList()
  }

  /**
   * We do not support FastFileSearchTree for RawFile so this method just calls listFiles instead
   * */
  override fun listSnapshotFiles(dir: AbstractFile, recursively: Boolean): List<AbstractFile> {
    if (!recursively) {
      return listFiles(dir)
    }

    val resultFiles = ArrayList<AbstractFile>(32)
    val files = listFiles(dir)
    if (files.isEmpty()) {
      return emptyList()
    }

    files.forEach { file ->
      val jFile = toFile(file)

      if (jFile.isDirectory) {
        resultFiles += listSnapshotFiles(file, recursively)
      } else {
        resultFiles += file
      }
    }

    return resultFiles
  }

  override fun <T> withFileDescriptor(
    file: AbstractFile,
    fileDescriptorMode: FileDescriptorMode,
    func: (FileDescriptor) -> T?
  ): T? {
    val fileCopy = toFile(file.clone())

    return when (fileDescriptorMode) {
      FileDescriptorMode.Read -> FileInputStream(fileCopy).use { fis -> func(fis.fd) }
      FileDescriptorMode.Write -> FileOutputStream(fileCopy, false).use { fos -> func(fos.fd) }
      FileDescriptorMode.WriteTruncate -> FileOutputStream(
        fileCopy,
        true
      ).use { fos -> func(fos.fd) }
      else -> {
        throw NotImplementedError("withFileDescriptor() Not implemented for " +
          "fileDescriptorMode = ${fileDescriptorMode.name}")
      }
    }
  }

  private fun toFile(file: AbstractFile): File {
    val root = file.getFileRoot<File>()
    val segments = file.getFileSegments()

    return if (segments.isEmpty()) {
      root.holder
    } else {
      root.holder.appendMany(segments.map { segment -> segment.name })
    }
  }

  companion object {
    private const val TAG = "RawFileManager"
  }
}