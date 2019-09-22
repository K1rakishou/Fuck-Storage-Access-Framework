package com.github.k1rakishou.fsaf.manager

import android.util.Log
import com.github.k1rakishou.fsaf.extensions.appendMany
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.RawFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class RawFileManager : BaseFileManager {

  override fun exists(file: AbstractFile): Boolean = toFile(file.clone()).exists()
  override fun isFile(file: AbstractFile): Boolean = toFile(file.clone()).isFile
  override fun isDirectory(file: AbstractFile): Boolean = toFile(file.clone()).isDirectory
  override fun canRead(file: AbstractFile): Boolean = toFile(file.clone()).canRead()
  override fun canWrite(file: AbstractFile): Boolean = toFile(file.clone()).canWrite()

  override fun getSegmentNames(file: AbstractFile): List<String> {
    return file.getFullPath()
      .split(File.separatorChar)
  }

  override fun delete(file: AbstractFile): Boolean {
    return toFile(file.clone()).delete()
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

  override fun getName(file: AbstractFile): String {
    return toFile(file.clone()).name
  }

  override fun findFile(dir: AbstractFile, fileName: String): RawFile? {
    val root = dir.getFileRoot<File>()
    val segments = dir.getFileSegments()
    check(root !is AbstractFile.Root.FileRoot) { "Cannot use FileRoot as directory" }

    val copy = File(root.holder.absolutePath)
    if (segments.isNotEmpty()) {
      copy.appendMany(segments.map { segment -> segment.name })
    }

    val resultFile = File(copy.absolutePath, fileName)
    if (!resultFile.exists()) {
      return null
    }

    val newRoot = if (resultFile.isFile) {
      AbstractFile.Root.FileRoot(resultFile, resultFile.name)
    } else {
      AbstractFile.Root.DirRoot(resultFile)
    }

    return RawFile(newRoot)
  }

  override fun getLength(file: AbstractFile): Long = toFile(file.clone()).length()

  override fun lastModified(file: AbstractFile): Long {
    return toFile(file.clone()).lastModified()
  }

  override fun listFiles(dir: AbstractFile): List<RawFile> {
    val root = dir.getFileRoot<File>()
    check(root !is AbstractFile.Root.FileRoot) { "Cannot use listFiles with FileRoot" }

    return toFile(dir.clone())
      .listFiles()
      ?.map { file -> RawFile(AbstractFile.Root.DirRoot(file)) }
      ?: emptyList()
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