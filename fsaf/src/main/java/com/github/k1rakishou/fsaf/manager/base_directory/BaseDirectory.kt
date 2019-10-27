package com.github.k1rakishou.fsaf.manager.base_directory

import android.net.Uri
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.ExternalFile
import com.github.k1rakishou.fsaf.file.RawFile
import java.io.File

/**
 * Base directory is useful when you want to have a file dump directory for your app (like a
 * directory where you will be storing downloaded files or some user selected directory to be used
 * by your app). It may have a SAF directory (like on sd-card) and a regular java-file backed
 * directory (like a directory in the internal app storage or external like the Downloads directory)
 * or even both at the same time. When having both you may set up a mechanism that will
 * automatically switch from the SAF directory to the java backed directory if SAF directory is
 * deleted by the user of the permissions are not granted for that directory anymore.
 *
 * If you want to have a base directory you need to inherit from this class and then add register it
 * in the [DirectoryManager] via the [FileManager]
 * */
abstract class BaseDirectory(
  private val debugMode: Boolean
) {

  fun isBaseDir(dirPath: Uri): Boolean {
    if (debugMode) {
      check(!(getDirUri() == null && getDirFile() == null)) { "Both dirUri and dirFile are nulls!" }
    }

    if (getDirUri() == null) {
      return false
    }

    return getDirUri() == dirPath
  }

  fun isBaseDir(dirPath: File): Boolean {
    if (debugMode) {
      check(!(getDirUri() == null && getDirFile() == null)) { "Both dirUri and dirFile are nulls!" }
    }

    if (getDirFile() == null) {
      return false
    }

    return getDirFile() == dirPath
  }

  fun isBaseDir(dir: AbstractFile): Boolean {
    if (debugMode) {
      check(!(getDirUri() == null && getDirFile() == null)) { "Both dirUri and dirFile are nulls!" }
    }

    if (dir is ExternalFile) {
      if (getDirUri() == null) {
        return false
      }

      return dir.getFileRoot<CachingDocumentFile>().holder.uri() == getDirUri()
    } else if (dir is RawFile) {
      if (getDirFile() == null) {
        return false
      }

      return dir.getFileRoot<File>().holder.absolutePath == getDirFile()?.absolutePath
    }

    throw IllegalStateException("${dir.javaClass.name} is not supported!")
  }

  fun dirPath(): String {
    if (debugMode) {
      check(!(getDirUri() == null && getDirFile() == null)) { "Both dirUri and dirFile are nulls!" }
    }

    if (getDirUri() != null) {
      return getDirUri().toString()
    }

    return getDirFile()!!.absolutePath
  }

  /**
   * This should need to return an Uri to the SAF directory.
   *
   * If both [getDirUri] and [getDirFile] return null then methods like
   * [FileManager.newBaseDirectoryFile] will throw an exception!
   * */
  abstract fun getDirUri(): Uri?

  /**
   * This one should return a fallback java file backed directory.
   * */
  abstract fun getDirFile(): File?
}
