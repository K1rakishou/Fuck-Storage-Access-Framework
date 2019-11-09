package com.github.k1rakishou.fsaf.file

import android.content.Context
import android.net.Uri
import com.github.k1rakishou.fsaf.BadPathSymbolResolutionStrategy
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.extensions.appendMany

class ExternalFile(
  private val appContext: Context,
  badPathSymbolResolutionStrategy: BadPathSymbolResolutionStrategy,
  root: Root<CachingDocumentFile>,
  segments: List<Segment> = listOf()
) : AbstractFile(badPathSymbolResolutionStrategy, root, segments) {

  @Suppress("UNCHECKED_CAST")
  override fun getFullPath(): String {
    val oldRootUri = (root as Root<CachingDocumentFile>).holder.uri()

    return Uri.parse(oldRootUri.toString())
      .buildUpon()
      .appendMany(segments.map { segment -> segment.name })
      .build()
      .toString()
  }

  @Suppress("UNCHECKED_CAST")
  override fun cloneInternal(newSegments: List<Segment>): ExternalFile = ExternalFile(
    appContext,
    badSymbolResolutionStrategy,
    (root as Root<CachingDocumentFile>).clone(),
    segments.toMutableList().apply { addAll(newSegments) }
  )

  override fun getFileManagerId(): FileManagerId = FILE_MANAGER_ID

  companion object {
    private const val TAG = "ExternalFile"
    val FILE_MANAGER_ID = FileManagerId(TAG)
  }
}