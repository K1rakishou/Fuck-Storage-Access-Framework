package com.github.k1rakishou.fsaf.manager

import android.net.Uri
import com.github.k1rakishou.fsaf.file.AbstractFile

class BaseDirectoryManager {
  private val baseDirList = mutableMapOf<String, AbstractBaseDirectory>()

  fun registerBaseDir(baseDirectory: AbstractBaseDirectory) {
    baseDirList.put(baseDirectory.baseDirectoryId(), baseDirectory)
  }

  fun unregisterBaseDir(baseDirectory: AbstractBaseDirectory) {
    baseDirList.remove(baseDirectory.baseDirectoryId())
  }

  fun unregisterBaseDir(dirUri: Uri) {
    getBaseDirByUri(dirUri)?.let { baseDir -> unregisterBaseDir(baseDir) }
  }

  fun isBaseDir(dir: AbstractFile): Boolean {
    return baseDirList.values.any { baseDir -> baseDir.isBaseDir(dir) }
  }

  fun isBaseDir(parentDirUri: Uri): Boolean {
    return baseDirList.values.any { baseDir -> baseDir.isBaseDir(parentDirUri) }
  }

  fun getBaseDirById(baseDirId: String): AbstractBaseDirectory? {
    return baseDirList[baseDirId]
  }

  fun getBaseDirByUri(dirUri: Uri): AbstractBaseDirectory? {
    return baseDirList.values.firstOrNull { baseDir -> baseDir.dirUri == dirUri }
  }

}