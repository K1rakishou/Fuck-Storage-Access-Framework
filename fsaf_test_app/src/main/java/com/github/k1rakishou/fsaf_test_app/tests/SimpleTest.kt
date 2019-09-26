package com.github.k1rakishou.fsaf_test_app.tests

import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.DirectorySegment
import com.github.k1rakishou.fsaf.file.FileSegment
import kotlin.system.measureTimeMillis

class SimpleTest(
  tag: String,
  isFastMode: Boolean
) : BaseTest(tag, isFastMode) {

  fun runTests(fileManager: FileManager, baseDir: AbstractFile) {
    fileManager.deleteContent(baseDir)
    checkDirEmpty(fileManager, baseDir)

    kotlin.run {
      val time = measureTimeMillis {
        test1(fileManager, baseDir)
      }

      log("test1 took ${time}ms")
    }

    kotlin.run {
      val time = measureTimeMillis {
        test2(fileManager, baseDir)
      }

      log("test2 took ${time}ms")
    }

    kotlin.run {
      val time = measureTimeMillis {
        test3(fileManager, baseDir)
      }

      log("test3 took ${time}ms")
    }
  }

  private fun test3(fileManager: FileManager, baseDir: AbstractFile) {
    val externalFile = fileManager.create(
      baseDir,
      DirectorySegment("123"),
      DirectorySegment("456"),
      DirectorySegment("789"),
      FileSegment("test123.txt")
    )

    if (externalFile == null || !fileManager.exists(externalFile)) {
      throw TestException("Couldn't create test123.txt")
    }

    for (i in 0 until 1000) {
      if (!fileManager.exists(externalFile)) {
        throw TestException("Does not exist")
      }

      if (!fileManager.isFile(externalFile)) {
        throw TestException("Not a file")
      }

      if (fileManager.isDirectory(externalFile)) {
        throw TestException("Is a directory")
      }
    }
  }

  private fun test1(fileManager: FileManager, baseDir: AbstractFile) {
    val externalFile = fileManager.create(
      baseDir,
      DirectorySegment("123"),
      DirectorySegment("456"),
      DirectorySegment("789"),
      FileSegment("test123.txt")
    )

    if (externalFile == null || !fileManager.exists(externalFile)) {
      throw TestException("Couldn't create test123.txt")
    }

    if (!fileManager.isFile(externalFile)) {
      throw TestException("test123.txt is not a file")
    }

    if (fileManager.isDirectory(externalFile)) {
      throw TestException("test123.txt is a directory")
    }

    if (fileManager.getName(externalFile) != "test123.txt") {
      throw TestException("externalFile name != test123.txt")
    }

    val externalFile2Exists = baseDir.clone(
      DirectorySegment("123"),
      DirectorySegment("456"),
      DirectorySegment("789")
    )

    if (!fileManager.exists(externalFile2Exists)) {
      throw TestException("789 directory does not exist")
    }

    val dirToDelete = baseDir.clone(
      DirectorySegment("123")
    )

    if (!fileManager.delete(dirToDelete) && fileManager.exists(dirToDelete)) {
      throw TestException("Couldn't delete test123.txt")
    }

    checkDirEmpty(fileManager, baseDir)
  }

  private fun test2(fileManager: FileManager, baseDir: AbstractFile) {
    val externalFile = fileManager.create(
      baseDir,
      DirectorySegment("1234"),
      DirectorySegment("4566"),
      FileSegment("filename.json")
    )

    if (externalFile == null || !fileManager.exists(externalFile)) {
      throw TestException("Couldn't create filename.json")
    }

    if (!fileManager.isFile(externalFile)) {
      throw TestException("filename.json is not a file")
    }

    if (fileManager.isDirectory(externalFile)) {
      throw TestException("filename.json is not a directory")
    }

    if (fileManager.getName(externalFile) != "filename.json") {
      throw TestException("externalFile1 name != filename.json")
    }

    val dir = baseDir.clone(
      DirectorySegment("1234"),
      DirectorySegment("4566")
    )

    if (fileManager.getName(dir) != "4566") {
      throw TestException("dir.name != 4566, name = " + fileManager.getName(dir))
    }

    val foundFile = fileManager.findFile(dir, "filename.json")
    if (foundFile == null || !fileManager.exists(foundFile)) {
      throw TestException("Couldn't find filename.json")
    }
  }

}