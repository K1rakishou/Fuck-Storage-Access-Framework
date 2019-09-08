package com.github.k1rakishou.fsaf.file

import android.util.Log
import com.github.k1rakishou.fsaf.extensions.appendMany
import java.io.File

class RawFile(
  root: Root<File>,
  segments: MutableList<Segment> = mutableListOf()
) : AbstractFile(root, segments) {

  override fun appendSubDirSegment(name: String): RawFile {
    check(root !is Root.FileRoot) { "root is already FileRoot, cannot append anything anymore" }
    return super.appendSubDirSegmentInner(name) as RawFile
  }

  override fun appendFileNameSegment(name: String): RawFile {
    check(root !is Root.FileRoot) { "root is already FileRoot, cannot append anything anymore" }
    return super.appendFileNameSegmentInner(name) as RawFile
  }

  override fun createNew(): RawFile? {
    check(root !is Root.FileRoot) {
      "root is already FileRoot, cannot append anything anymore"
    }

    root.holder as File

    if (segments.isEmpty()) {
      if (!root.holder.exists()) {
        if (root.holder.isFile) {
          if (!root.holder.createNewFile()) {
            Log.e(TAG, "Couldn't create file, path = ${root.holder.absolutePath}")
            return null
          }
        } else {
          if (!root.holder.mkdirs()) {
            Log.e(TAG, "Couldn't create directory, path = ${root.holder.absolutePath}")
            return null
          }
        }
      }

      return this
    }

    var newFile = root.holder as File
    for (segment in segments) {
      newFile = File(newFile, segment.name)

      if (segment.isFileName) {
        if (!newFile.exists() && !newFile.createNewFile()) {
          Log.e(TAG, "Could not create a new file, path = " + newFile.absolutePath)
          return null
        }
      } else {
        if (!newFile.exists() && !newFile.mkdir()) {
          Log.e(TAG, "Could not create a new directory, path = " + newFile.absolutePath)
          return null
        }
      }

      if (segment.isFileName) {
        return RawFile(Root.FileRoot(newFile, segment.name))
      }
    }

    return RawFile(Root.DirRoot(newFile))
  }

  override fun clone(): RawFile = RawFile(
    root.clone() as Root<File>,
    segments.toMutableList()
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