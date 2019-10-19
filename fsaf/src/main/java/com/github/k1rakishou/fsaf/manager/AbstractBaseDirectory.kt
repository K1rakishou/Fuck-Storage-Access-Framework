package com.github.k1rakishou.fsaf.manager

import android.net.Uri
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.file.AbstractFile

abstract class AbstractBaseDirectory(
  val dirUri: Uri
) {

  fun isBaseDir(dir: AbstractFile): Boolean {
    return isBaseDir(dir.getFileRoot<CachingDocumentFile>().holder.uri)
  }

  fun isBaseDir(dirUri: Uri): Boolean {
    return this.dirUri == dirUri
  }

  abstract fun baseDirectoryId(): String
}