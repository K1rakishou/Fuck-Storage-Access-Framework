package com.github.k1rakishou.fsaf.file

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.github.k1rakishou.fsaf.extensions.appendMany
import com.github.k1rakishou.fsaf.extensions.getMimeFromFilename

class ExternalFile(
  private val appContext: Context,
  root: Root<DocumentFile>,
  segments: MutableList<Segment> = mutableListOf()
) : AbstractFile(root, segments) {
  private val mimeTypeMap = MimeTypeMap.getSingleton()

  override fun appendSubDirSegment(name: String): ExternalFile {
    check(root !is Root.FileRoot) { "root is already FileRoot, cannot append anything anymore" }
    return super.appendSubDirSegmentInner(name) as ExternalFile
  }

  override fun appendFileNameSegment(name: String): ExternalFile {
    check(root !is Root.FileRoot) { "root is already FileRoot, cannot append anything anymore" }
    return super.appendFileNameSegmentInner(name) as ExternalFile
  }

  @Suppress("UNCHECKED_CAST")
  override fun createNew(): ExternalFile? {
    check(root !is Root.FileRoot) {
      "root is already FileRoot, cannot append anything anymore"
    }

    if (segments.isEmpty()) {
      // Root is probably already exists and there is no point in creating it again so just
      // return null here
      return null
    }

    var newFile: DocumentFile? = null

    for (segment in segments) {
      val file = newFile ?: (root as Root<DocumentFile>).holder

      val prevFile = file.findFile(segment.name)
      if (prevFile != null) {
        // File already exists, no need to create it again (and we won't be able)
        newFile = prevFile
        continue
      }

      if (!segment.isFileName) {
        newFile = file.createDirectory(segment.name)
        if (newFile == null) {
          Log.e(
            TAG, "createNew() file.createDirectory() returned null, file.uri = ${file.uri}, " +
              "segment.name = ${segment.name}"
          )
          return null
        }
      } else {
        newFile = file.createFile(mimeTypeMap.getMimeFromFilename(segment.name), segment.name)
        if (newFile == null) {
          Log.e(
            TAG, "createNew() file.createFile returned null, file.uri = ${file.uri}, " +
              "segment.name = ${segment.name}"
          )
          return null
        }

        // Ignore any left segments (which we shouldn't have) after encountering fileName
        // segment
        return ExternalFile(appContext, Root.FileRoot(newFile, segment.name))
      }
    }

    if (newFile == null) {
      Log.e(TAG, "result file is null")
      return null
    }

    if (segments.size < 1) {
      Log.e(TAG, "Must be at least one segment!")
      return null
    }

    val lastSegment = segments.last()
    val isLastSegmentFilename = lastSegment.isFileName

    val root = if (isLastSegmentFilename) {
      Root.FileRoot(newFile, lastSegment.name)
    } else {
      Root.DirRoot(newFile)
    }

    return ExternalFile(appContext, root)
  }

  @Suppress("UNCHECKED_CAST")
  override fun getFullPath(): String {
    return Uri.parse((root as Root<DocumentFile>).holder.uri.toString()).buildUpon()
      .appendMany(segments.map { segment -> segment.name })
      .build()
      .toString()
  }

  @Suppress("UNCHECKED_CAST")
  override fun clone(): ExternalFile = ExternalFile(
    appContext,
    (root as Root<DocumentFile>).clone(),
    segments.toMutableList()
  )

  override fun getFileManagerId(): FileManagerId = FILE_MANAGER_ID

  companion object {
    private const val TAG = "ExternalFile"
    val FILE_MANAGER_ID = FileManagerId(TAG)
  }
}