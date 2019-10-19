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
  val baseDirectoryId: String,
  val dirUri: Uri?,
  val dirFile: File?
) {

  init {
    check(!(dirUri == null && dirFile == null)) { "Cannot have both dirUri and dirFile be null!" }
  }

  fun isBaseDir(dirPath: Uri): Boolean {
    if (dirUri == null) {
      return false
    }

    return dirUri == dirPath
  }

  fun isBaseDir(dirPath: File): Boolean {
    if (dirFile == null) {
      return false
    }

    return dirFile == dirPath
  }

  fun isBaseDir(dir: AbstractFile): Boolean {
    if (dir is ExternalFile) {
      if (dirUri == null) {
        return false
      }

      return dir.getFileRoot<CachingDocumentFile>().holder.uri == dirUri
    } else if (dir is RawFile) {
      if (dirFile == null) {
        return false
      }

      return dir.getFileRoot<File>().holder.absolutePath == dirFile.absolutePath
    }

    throw IllegalStateException("${dir.javaClass.name} is not supported!")
  }

  fun dirPath(): String {
    if (dirUri != null) {
      return dirUri.toString()
    }

    return dirFile!!.absolutePath
  }
}
