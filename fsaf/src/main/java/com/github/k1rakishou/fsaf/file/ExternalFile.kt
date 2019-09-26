package com.github.k1rakishou.fsaf.file

import android.content.Context
import android.net.Uri
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.extensions.appendMany

class ExternalFile(
  private val appContext: Context,
  root: Root<CachingDocumentFile>,
  segments: MutableList<Segment> = mutableListOf()
) : AbstractFile(root, segments) {

  @Suppress("UNCHECKED_CAST")
  override fun getFullPath(): String {
    return Uri.parse((root as Root<CachingDocumentFile>).holder.uri.toString()).buildUpon()
      .appendMany(segments.map { segment -> segment.name })
      .build()
      .toString()
  }

  @Suppress("UNCHECKED_CAST")
  override fun cloneInternal(newSegments: List<Segment>): ExternalFile = ExternalFile(
    appContext,
    (root as Root<CachingDocumentFile>).clone(),
    segments.toMutableList().apply { addAll(newSegments) }
  )

  override fun getFileManagerId(): FileManagerId = FILE_MANAGER_ID

  companion object {
    private const val TAG = "ExternalFile"
    val FILE_MANAGER_ID = FileManagerId(TAG)
  }
}