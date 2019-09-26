package com.github.k1rakishou.fsaf_test_app.extensions

import android.content.ContentResolver

internal const val CONTENT_TYPE = "${ContentResolver.SCHEME_CONTENT}://"
internal const val FILE_TYPE = "${ContentResolver.SCHEME_FILE}://"
internal val uriTypes = arrayOf(CONTENT_TYPE, FILE_TYPE)

internal const val ENCODED_SEPARATOR = "%2F"
internal const val FILE_SEPARATOR1 = "/"
internal const val FILE_SEPARATOR2 = "\\"

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