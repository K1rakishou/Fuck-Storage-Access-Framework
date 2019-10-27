package com.github.k1rakishou.fsaf.manager.base_directory

import android.net.Uri
import android.util.Log
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.file.AbstractFile

/**
 * A class that is responsible for base directories registering/unregistering etc.
 * */
open class DirectoryManager {
  private val baseDirList = mutableMapOf<Class<BaseDirectory>, BaseDirectory>()

  open fun registerBaseDir(clazz: Class<*>, baseDirectory: BaseDirectory) {
    baseDirList.put(clazz as Class<BaseDirectory>, baseDirectory)
  }

  open fun unregisterBaseDir(clazz: Class<*>) {
    baseDirList.remove(clazz)
  }

  open fun isBaseDir(dir: AbstractFile): Boolean {
    return baseDirList.values.any { baseDir -> baseDir.isBaseDir(dir) }
  }

  open fun isBaseDir(dir: Uri): Boolean {
    return baseDirList.values.any { baseDir ->
      baseDir.isBaseDir(dir)
    }
  }

  open fun isBaseDir(dir: CachingDocumentFile): Boolean {
    if (!dir.isDirectory()) {
      Log.e(TAG, "dir ${dir.uri()} is not a directory")
      return false
    }

    return baseDirList.values.any { baseDir ->
      baseDir.isBaseDir(dir.uri())
    }
  }

  inline fun <reified T : BaseDirectory> getBaseDirByClass(): BaseDirectory? {
    return getBaseDirByClass(T::class.java)
  }

  open fun getBaseDirByClass(clazz: Class<*>): BaseDirectory? {
    return baseDirList[clazz as Class<BaseDirectory>]
  }

  open fun getBaseDir(dir: Uri): BaseDirectory? {
    return baseDirList.values.firstOrNull { baseDir ->
      baseDir.isBaseDir(dir)
    }
  }

  companion object {
    private const val TAG = "DirectoryManager"
  }
}