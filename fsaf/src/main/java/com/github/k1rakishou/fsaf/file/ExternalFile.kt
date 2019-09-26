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

  override fun appendSubDirSegment(name: String): ExternalFile {
    check(root !is Root.FileRoot) { "root is already FileRoot, cannot append anything anymore" }
    return super.appendSubDirSegmentInner(name) as ExternalFile
  }

  override fun appendFileNameSegment(name: String): ExternalFile {
    check(root !is Root.FileRoot) { "root is already FileRoot, cannot append anything anymore" }
    return super.appendFileNameSegmentInner(name) as ExternalFile
  }

  @Suppress("UNCHECKED_CAST")
  override fun getFullPath(): String {
    return Uri.parse((root as Root<CachingDocumentFile>).holder.uri.toString()).buildUpon()
      .appendMany(segments.map { segment -> segment.name })
      .build()
      .toString()
  }

  @Suppress("UNCHECKED_CAST")
  override fun clone(): ExternalFile = ExternalFile(
    appContext,
    (root as Root<CachingDocumentFile>).clone(),
    segments.toMutableList()
  )

  override fun getFileManagerId(): FileManagerId = FILE_MANAGER_ID

  companion object {
    private const val TAG = "ExternalFile"
    val FILE_MANAGER_ID = FileManagerId(TAG)
  }
}