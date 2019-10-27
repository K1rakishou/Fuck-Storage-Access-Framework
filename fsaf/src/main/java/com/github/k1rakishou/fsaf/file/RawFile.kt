package com.github.k1rakishou.fsaf.file

import com.github.k1rakishou.fsaf.extensions.appendMany
import java.io.File

class RawFile(
  root: Root<File>,
  segments: List<Segment> = listOf()
) : AbstractFile(root, segments) {

  override fun cloneInternal(newSegments: List<Segment>): RawFile = RawFile(
    root.clone() as Root<File>,
    segments.toMutableList().apply { addAll(newSegments) }
  )

  override fun getFullPath(): String {
    root.holder as File

    return File(root.holder.absolutePath)
      .appendMany(segments.map { segment -> segment.name })
      .absolutePath
  }

  override fun getFileManagerId(): FileManagerId = FILE_MANAGER_ID

  companion object {
    private const val TAG = "RawFile"
    val FILE_MANAGER_ID = FileManagerId(TAG)
  }
}