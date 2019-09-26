package com.github.k1rakishou.fsaf_test_app.tests

import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.file.AbstractFile

abstract class BaseTest(
  private val tag: String,
  private val isFastMode: Boolean
) {

  protected fun log(message: String) {
    println("$tag, isFastMode = $isFastMode, $message")
  }

  protected fun checkDirEmpty(fileManager: FileManager, dirUri: AbstractFile) {
    val files = fileManager.listFiles(dirUri)
    if (files.isNotEmpty()) {
      throw TestException("Couldn't not delete some files in the base directory: ${files}")
    }
  }
}