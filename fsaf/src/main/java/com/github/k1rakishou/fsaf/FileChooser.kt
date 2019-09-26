package com.github.k1rakishou.fsaf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.github.k1rakishou.fsaf.callback.*
import com.github.k1rakishou.fsaf.extensions.getMimeFromFilename

class FileChooser(
  private val appContext: Context
) {
  private val callbacksMap = hashMapOf<Int, ChooserCallback>()
  private val mimeTypeMap = MimeTypeMap.getSingleton()

  private var requestCode = 10000
  private var fsafActivityCallbacks: FSAFActivityCallbacks? = null

  fun setCallbacks(FSAFActivityCallbacks: FSAFActivityCallbacks) {
    this.fsafActivityCallbacks = FSAFActivityCallbacks
  }

  fun removeCallbacks() {
    this.fsafActivityCallbacks = null
  }

  fun openChooseDirectoryDialog(directoryChooserCallback: DirectoryChooserCallback) {
    fsafActivityCallbacks?.let { callbacks ->
      val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
      intent.putExtra("android.content.extra.SHOW_ADVANCED", true)

      intent.addFlags(
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
          Intent.FLAG_GRANT_READ_URI_PERMISSION or
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      )

      val nextRequestCode = ++requestCode
      callbacksMap[nextRequestCode] = directoryChooserCallback as ChooserCallback

      try {
        callbacks.fsafStartActivityForResult(intent, nextRequestCode)
      } catch (e: Exception) {
        callbacksMap.remove(nextRequestCode)
        directoryChooserCallback.onCancel(
          e.message
            ?: "openChooseDirectoryDialog() Unknown error"
        )
      }
    }
  }

  fun openChooseFileDialog(fileChooserCallback: FileChooserCallback) {
    fsafActivityCallbacks?.let { callbacks ->
      val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
      intent.addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      )

      intent.addCategory(Intent.CATEGORY_OPENABLE)
      intent.type = "*/*"

      val nextRequestCode = ++requestCode
      callbacksMap[nextRequestCode] = fileChooserCallback as ChooserCallback

      try {
        callbacks.fsafStartActivityForResult(intent, nextRequestCode)
      } catch (e: Exception) {
        callbacksMap.remove(nextRequestCode)
        fileChooserCallback.onCancel(e.message ?: "openChooseFileDialog() Unknown error")
      }
    }
  }

  fun openCreateFileDialog(
    fileName: String,
    fileCreateCallback: FileCreateCallback
  ) {
    fsafActivityCallbacks?.let { callbacks ->
      val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
      intent.addFlags(
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
          Intent.FLAG_GRANT_READ_URI_PERMISSION or
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      )

      intent.addCategory(Intent.CATEGORY_OPENABLE)
      intent.type = mimeTypeMap.getMimeFromFilename(fileName)
      intent.putExtra(Intent.EXTRA_TITLE, fileName)

      val nextRequestCode = ++requestCode
      callbacksMap[nextRequestCode] = fileCreateCallback as ChooserCallback

      try {
        callbacks.fsafStartActivityForResult(intent, nextRequestCode)
      } catch (e: Exception) {
        callbacksMap.remove(nextRequestCode)
        fileCreateCallback.onCancel(e.message ?: "openCreateFileDialog() Unknown error")
      }
    }
  }

  fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    val callback = callbacksMap[requestCode]
    if (callback == null) {
      Log.d(TAG, "Callback is already removed from the map, resultCode = $requestCode")
      return false
    }

    try {
      if (fsafActivityCallbacks == null) {
        // Skip all requests when the callback is not set
        Log.d(TAG, "Callback is not attached")
        return false
      }

      when (callback) {
        is DirectoryChooserCallback -> {
          handleDirectoryChooserCallback(callback, resultCode, data)
        }
        is FileChooserCallback -> {
          handleFileChooserCallback(callback, resultCode, data)
        }
        is FileCreateCallback -> {
          handleFileCreateCallback(callback, resultCode, data)
        }
        else -> throw IllegalArgumentException("Not implemented for ${callback.javaClass.name}")
      }

      return true
    } finally {
      callbacksMap.remove(requestCode)
    }
  }

  private fun handleFileCreateCallback(
    callback: FileCreateCallback,
    resultCode: Int,
    intent: Intent?
  ) {
    if (resultCode != Activity.RESULT_OK) {
      val msg = "handleFileCreateCallback() Non OK result ($resultCode)"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    if (intent == null) {
      val msg = "handleFileCreateCallback() Intent is null"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    val read = (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
    val write = (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0

    if (!read) {
      val msg = "handleFileCreateCallback() No grant read uri permission given"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    if (!write) {
      val msg = "handleFileCreateCallback() No grant write uri permission given"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    val uri = intent.data
    if (uri == null) {
      val msg = "handleFileCreateCallback() intent.getData() == null"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    callback.onResult(uri)
  }

  private fun handleFileChooserCallback(
    callback: FileChooserCallback,
    resultCode: Int,
    intent: Intent?
  ) {
    if (resultCode != Activity.RESULT_OK) {
      val msg = "handleFileChooserCallback() Non OK result ($resultCode)"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    if (intent == null) {
      val msg = "handleFileChooserCallback() Intent is null"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    val read = (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
    val write = (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0

    if (!read) {
      val msg = "handleFileChooserCallback() No grant read uri permission given"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    if (!write) {
      val msg = "handleFileChooserCallback() No grant write uri permission given"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    val uri = intent.data
    if (uri == null) {
      val msg = "handleFileChooserCallback() intent.getData() == null"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    callback.onResult(uri)
  }

  private fun handleDirectoryChooserCallback(
    callback: DirectoryChooserCallback,
    resultCode: Int,
    intent: Intent?
  ) {
    if (resultCode != Activity.RESULT_OK) {
      val msg = "handleDirectoryChooserCallback() Non OK result ($resultCode)"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    if (intent == null) {
      val msg = "handleDirectoryChooserCallback() Intent is null"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    val read = (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
    val write = (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0

    if (!read) {
      val msg = "handleDirectoryChooserCallback() No grant read uri permission given"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    if (!write) {
      val msg = "handleDirectoryChooserCallback() No grant write uri permission given"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    val uri = intent.data
    if (uri == null) {
      val msg = "handleDirectoryChooserCallback() intent.getData() == null"

      Log.e(TAG, msg)
      callback.onCancel(msg)
      return
    }

    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
      Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    val contentResolver = appContext.contentResolver
    contentResolver.takePersistableUriPermission(uri, flags)

    val treeUri = DocumentFile.fromTreeUri(appContext, uri)!!.uri
    Log.d(TAG, "treeUri = ${treeUri}")

    callback.onResult(treeUri)
  }

  fun forgetSAFTree(directoryUri: Uri): Boolean {
    val directory = DocumentFile.fromTreeUri(appContext, directoryUri)
    if (directory == null) {
      Log.e(TAG, "forgetSAFTree() DocumentFile.fromTreeUri returned null")
      return false
    }

    if (!directory.exists()) {
      Log.e(TAG, "Couldn't revoke permissions from directory because it does not exist, path = $directoryUri")
      return false
    }

    if (!directory.isDirectory) {
      Log.e(TAG, "Couldn't revoke permissions from directory it is not a directory, path = $directoryUri")
      return false
    }

    return try {
      val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

      val rootUri = DocumentsContract.buildTreeDocumentUri(
        directoryUri.authority,
        DocumentsContract.getTreeDocumentId(directoryUri)
      )

      appContext.contentResolver.releasePersistableUriPermission(rootUri, flags)
      appContext.revokeUriPermission(rootUri, flags)

      Log.d(TAG, "Revoke old path permissions success on $directoryUri")
      true
    } catch (err: Exception) {
      Log.e(TAG, "Error revoking old path permissions on $directoryUri", err)
      false
    }
  }

  companion object {
    private const val TAG = "FileChooser"
  }
}