package com.github.k1rakishou.fsaf.manager.base_directory

import android.net.Uri
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.ExternalFile
import com.github.k1rakishou.fsaf.file.RawFile
import java.io.File

/**
 * There may be either both dirs set up (in this case the dirUri takes the precedence) or only one.
 * But there must be at least one!
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

  abstract fun getDirUri(): Uri?
  abstract fun getDirFile(): File?
}
