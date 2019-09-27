package com.github.k1rakishou.fsaf.document_file

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

open class CachingDocumentFile(
  protected val appContext: Context,
  val delegate: DocumentFile
) {
  open val exists: Boolean by lazy { delegate.exists() }
  open val isFile: Boolean by lazy { delegate.isFile }
  open val isDirectory: Boolean by lazy { delegate.isDirectory }
  open val canRead: Boolean by lazy { delegate.canRead() }
  open val canWrite: Boolean by lazy { delegate.canWrite() }
  open val name: String? by lazy { delegate.name }
  open val length: Long by lazy { delegate.length() }
  open val lastModified: Long by lazy { delegate.lastModified() }
  open val uri: Uri by lazy { delegate.uri }

  fun listFiles(): List<CachingDocumentFile> =
    delegate.listFiles().map { file ->
      CachingDocumentFile(
        appContext,
        file
      )
    }

  fun createDirectory(name: String): CachingDocumentFile? {
    val result = delegate.createDirectory(name)
      ?: return null

    return CachingDocumentFile(appContext, result)
  }

  fun createFile(mime: String, name: String): CachingDocumentFile? {
    val result = delegate.createFile(mime, name)
      ?: return null

    return CachingDocumentFile(appContext, result)
  }

  fun delete(): Boolean {
    return delegate.delete()
  }
}