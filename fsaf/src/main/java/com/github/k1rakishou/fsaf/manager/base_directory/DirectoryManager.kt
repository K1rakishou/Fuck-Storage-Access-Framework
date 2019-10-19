package com.github.k1rakishou.fsaf.manager.base_directory

import android.net.Uri
import android.util.Log
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.file.AbstractFile

class DirectoryManager {
  private val baseDirList = mutableMapOf<String, BaseDirectory>()

  fun registerBaseDir(baseDirectory: BaseDirectory) {
    baseDirList.put(baseDirectory.baseDirectoryId, baseDirectory)
  }

  fun unregisterBaseDir(baseDirectory: BaseDirectory) {
    baseDirList.remove(baseDirectory.baseDirectoryId)
  }

  fun unregisterBaseDir(dirUri: Uri) {
    getBaseDir(dirUri)?.let { baseDir -> unregisterBaseDir(baseDir) }
  }

  fun isBaseDir(dir: AbstractFile): Boolean {
    return baseDirList.values.any { baseDir -> baseDir.isBaseDir(dir) }
  }

  fun isBaseDir(dir: Uri): Boolean {
    return baseDirList.values.any { baseDir ->
      baseDir.isBaseDir(dir)
    }
  }

  fun isBaseDir(dir: CachingDocumentFile): Boolean {
    if (!dir.isDirectory) {
      Log.e(TAG, "dir ${dir.uri} is not a directory")
      return false
    }

    return baseDirList.values.any { baseDir ->
      baseDir.isBaseDir(dir.uri)
    }
  }

  fun getBaseDirById(baseDirId: String): BaseDirectory? {
    return baseDirList[baseDirId]
  }

  fun getBaseDir(dir: Uri): BaseDirectory? {
    return baseDirList.values.firstOrNull { baseDir ->
      baseDir.isBaseDir(dir)
    }
  }

  companion object {
    private const val TAG = "DirectoryManager"
  }
}