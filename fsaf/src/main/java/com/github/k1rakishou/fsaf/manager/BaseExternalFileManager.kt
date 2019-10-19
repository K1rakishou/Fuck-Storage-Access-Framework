package com.github.k1rakishou.fsaf.manager

import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.file.AbstractFile

interface BaseExternalFileManager : BaseFileManager {

  /**
   * Returns a list of all files and directories inside this cached in FastFileSearchTree directory.
   * */
  fun listCachedFile(dir: AbstractFile, recursively: Boolean): List<CachingDocumentFile>

}