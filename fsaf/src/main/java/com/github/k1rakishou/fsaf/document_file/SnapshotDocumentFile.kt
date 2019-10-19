package com.github.k1rakishou.fsaf.document_file

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.DocumentsContract
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile

class SnapshotDocumentFile(
  appContext: Context,
  delegate: DocumentFile,
  private val fileName: String?,
  private val fileMimeType: String?,
  private val fileFlags: Int,
  private val fileLastModified: Long,
  private val fileLength: Long
) : CachingDocumentFile(appContext, delegate) {

  override val exists: Boolean
    get() = true
  override val isFile: Boolean
    get() {
      return !(DocumentsContract.Document.MIME_TYPE_DIR == fileMimeType
        || TextUtils.isEmpty(fileMimeType))
    }
  override val isDirectory: Boolean
    get() = DocumentsContract.Document.MIME_TYPE_DIR == fileMimeType
  override val canRead: Boolean
    get() = canRead()
  override val canWrite: Boolean
    get() = canWrite()
  override val name: String?
    get() = fileName
  override val length: Long
    get() {
      check(!isDirectory) { "Cannot get the length of a directory" }
      return fileLength
    }
  override val lastModified: Long
    get() = fileLastModified

  private fun canRead(): Boolean {
    val result = appContext.checkCallingOrSelfUriPermission(
      uri,
      Intent.FLAG_GRANT_READ_URI_PERMISSION
    )

    if (result != PackageManager.PERMISSION_GRANTED) {
      return false
    }

    return !TextUtils.isEmpty(fileMimeType)
  }

  private fun canWrite(): Boolean {
    val result = appContext.checkCallingOrSelfUriPermission(
      uri,
      Intent.FLAG_GRANT_READ_URI_PERMISSION
    )

    if (result != PackageManager.PERMISSION_GRANTED) {
      return false
    }

    if (TextUtils.isEmpty(fileMimeType)) {
      return false
    }

    // Deletable documents considered writable
    if (fileFlags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0) {
      return true
    }

    if (DocumentsContract.Document.MIME_TYPE_DIR == fileMimeType
      && (fileFlags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0)
    ) {
      return true
    } else if (!TextUtils.isEmpty(fileMimeType)
      && (fileFlags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE != 0)
    ) {
      return true
    }

    return false
  }

}