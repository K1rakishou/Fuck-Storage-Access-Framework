package com.github.k1rakishou.fsaf.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.document_file.SnapshotDocumentFile
import com.github.k1rakishou.fsaf.file.AbstractFile
import java.util.*

object SAFHelper {
  private val TAG = "SAFHelper"

  private val columns = arrayOf(
    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
    DocumentsContract.Document.COLUMN_MIME_TYPE,
    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    DocumentsContract.Document.COLUMN_FLAGS,
    DocumentsContract.Document.COLUMN_SIZE
  )

  /**
   * Finds a file that has multiple layers of sub directories
   * */
  fun findDeepFile(
    appContext: Context,
    parentUri: Uri,
    segments: List<AbstractFile.Segment>
  ): SnapshotDocumentFile? {
    check(segments.isNotEmpty()) { "segments must not be empty" }

    var file: SnapshotDocumentFile? = null

    for (segment in segments) {
      val uri = file?.uri ?: parentUri

      file = findSnapshotFile(appContext, uri, segment.name)
      if (file == null) {
        return null
      }
    }

    return file
  }

  /**
   * Finds a file and preloads all of it's info (like name size mime type etc.) because every one
   * of those operations require and IPC call which slow as fuck. So it's way faster to just preload
   * all that shit at once
   * */
  fun findSnapshotFile(
    appContext: Context,
    parentUri: Uri,
    name: String
  ): SnapshotDocumentFile? {
    val selection = DocumentsContract.Document.COLUMN_DISPLAY_NAME + " = ?"

    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
      parentUri,
      DocumentsContract.getDocumentId(parentUri)
    )

    val contentResolver = appContext.contentResolver
    val filename = name.toLowerCase(Locale.ROOT)

    return contentResolver.query(
      childrenUri,
      columns,
      selection,
      arrayOf(filename),
      null
    )?.use { cursor ->
      while (cursor.moveToNext()) {
        val displayName = cursor.getString(2)
        if (displayName.toLowerCase(Locale.ROOT) != filename) {
          continue
        }

        val documentId = cursor.getString(0)
        val mimeType = cursor.getString(1)
        val lastModified = cursor.getLong(3)
        val flags = cursor.getInt(4)
        val size = cursor.getLong(5)

        val fileUri = DocumentsContract.buildDocumentUriUsingTree(
          parentUri,
          documentId
        )

        val documentFile = DocumentFile.fromSingleUri(
          appContext,
          fileUri
        )

        if (documentFile == null) {
          return@use null
        }

        return@use SnapshotDocumentFile(
          appContext,
          documentFile,
          displayName,
          mimeType,
          flags,
          lastModified,
          size
        )
      }

      return@use null
    }
  }

  fun findCachingFile(
    appContext: Context,
    parentUri: Uri,
    name: String
  ): CachingDocumentFile? {
    TODO("Not implemented")
  }

  /**
   * Same as above but preloads the whole directory. Use by [FileManager.snapshot]
   * */
  fun listFilesFast(
    appContext: Context,
    parentUri: Uri
  ): List<SnapshotDocumentFile> {
    val resolver = appContext.contentResolver

    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
      parentUri,
      DocumentsContract.getTreeDocumentId(parentUri)
    )

    try {
      return resolver.query(childrenUri, columns, null, null, null)?.use { cursor ->
        val result = mutableListOf<SnapshotDocumentFile>()

        while (cursor.moveToNext()) {
          val documentId = cursor.getString(0)
          val mimeType = cursor.getString(1)
          val displayName = cursor.getString(2)
          val lastModified = cursor.getLong(3)
          val flags = cursor.getInt(4)
          val size = cursor.getLong(5)

          val documentUri = DocumentsContract.buildDocumentUriUsingTree(
            parentUri,
            documentId
          )

          val documentFile = if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
            DocumentFile.fromTreeUri(appContext, documentUri)
          } else {
            DocumentFile.fromSingleUri(appContext, documentUri)
          }

          if (documentFile == null) {
            continue
          }

          val snapshotDocumentFile = SnapshotDocumentFile(
            appContext,
            documentFile,
            displayName,
            mimeType,
            flags,
            lastModified,
            size
          )

          result += snapshotDocumentFile
        }

        return@use result
      } ?: emptyList()
    } catch (error: Throwable) {
      Log.e(TAG, "Failed query: $error")
      return emptyList()
    }
  }

}