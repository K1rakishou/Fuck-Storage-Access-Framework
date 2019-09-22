package com.github.k1rakishou.fsaf.extensions

import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal const val BINARY_FILE_MIME_TYPE = "application/octet-stream"

internal const val CONTENT_TYPE = "content://"
internal const val FILE_TYPE = "file://"
internal val uriTypes = arrayOf(CONTENT_TYPE, FILE_TYPE)

internal const val ENCODED_SEPARATOR = "%2F"
internal const val FILE_SEPARATOR1 = "/"
internal const val FILE_SEPARATOR2 = "\\"

@Throws(IOException::class)
internal fun InputStream.copyInto(outputStream: OutputStream) {
  var read: Int
  val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

  while (true) {
    read = this.read(buffer)
    if (read == -1) {
      break
    }

    outputStream.write(buffer, 0, read)
  }
}

internal fun String.extension(): String? {
  val index = this.indexOfLast { ch -> ch == '.' }
  if (index == -1) {
    return null
  }

  if (index == this.lastIndex) {
    // The dot is at the very end of the string, so there is no extension
    return null
  }

  return this.substring(index + 1)
}

internal fun Uri.Builder.appendMany(segments: List<String>): Uri.Builder {
  for (segment in segments) {
    this.appendPath(segment)
  }

  return this
}

internal fun MimeTypeMap.getMimeFromFilename(filename: String): String {
  val extension = filename.extension()
    ?: return BINARY_FILE_MIME_TYPE

  val mimeType = this.getMimeTypeFromExtension(extension)
  if (mimeType == null || mimeType.isEmpty()) {
    return BINARY_FILE_MIME_TYPE
  }

  return mimeType
}

internal fun File.appendMany(segments: List<String>): File {
  var newFile = File(this.absolutePath)

  for (segment in segments) {
    newFile = File(newFile, segment)
  }

  return newFile
}

internal fun String.splitIntoSegments(): List<String> {
  val uriType = uriTypes.firstOrNull { type -> this.startsWith(type) }
  val string = if (uriType != null) {
    this.substring(uriType.length, this.length)
  } else {
    this
  }

  return if (string.contains(FILE_SEPARATOR1)
    || string.contains(FILE_SEPARATOR2)
    || string.contains(ENCODED_SEPARATOR)
  ) {
    val split = string
      // First of all split by the "/" symbol
      .split(FILE_SEPARATOR1)
      // Then try to split every part again by the "\" symbol
      .flatMap { names -> names.split(FILE_SEPARATOR2) }
      // Then try to split every part again by the "%2F" symbol
      .flatMap { names -> names.split(ENCODED_SEPARATOR) }

    split
  } else {
    listOf(string)
  }
}