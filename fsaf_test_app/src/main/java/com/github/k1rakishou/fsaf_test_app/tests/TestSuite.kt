package com.github.k1rakishou.fsaf_test_app.tests

import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.file.AbstractFile
import kotlin.system.measureTimeMillis

class TestSuite(
  private val fileManager: FileManager
) {
  private val TAG = "TestSuite"

  // TODO: findFile tests with dir and file
  fun runTests(baseDirSAF: AbstractFile, baseDirFile: AbstractFile) {
    try {
      println("$TAG =============== START TESTS ===============")
      println("$TAG baseDirSAF = ${baseDirSAF.getFullPath()}")
      println("$TAG baseDirFile = ${baseDirFile.getFullPath()}")

      runTestsWithCaching(fileManager, baseDirSAF, baseDirFile)

      println("$TAG =============== END TESTS ===============")
    } catch (error: Throwable) {
      println("$TAG =============== ERROR ===============")
      throw error
    }
  }

  private fun runTestsWithCaching(
    fileManager: FileManager,
    baseDirSAF: AbstractFile,
    baseDirFile: AbstractFile
  ) {
    val time = measureTimeMillis {
      SimpleTest("$TAG SimpleTest", true).runTests(fileManager, baseDirSAF)
      CreateFilesTest("$TAG CreateFilesTest", true).runTests(fileManager, baseDirSAF)
      SnapshotTest("$TAG SnapshotTest", true).runTests(fileManager, baseDirSAF)
      CopyTest("$TAG CopyTest", true).runTests(fileManager, baseDirSAF, baseDirFile)
    }

    println("$TAG runTestsWithCaching took ${time}ms")
  }

}