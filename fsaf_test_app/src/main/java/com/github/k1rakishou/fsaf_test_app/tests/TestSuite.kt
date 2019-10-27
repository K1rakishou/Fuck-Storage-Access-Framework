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

      runTestsWithSAFFiles(fileManager, baseDirSAF, baseDirFile)
      runTestsWithJavaFiles(fileManager, baseDirSAF, baseDirFile)

      check(fileManager.deleteContent(baseDirFile)) { "deleteContent baseDirFile returned false" }
      check(fileManager.deleteContent(baseDirSAF)) { "deleteContent baseDirSAF returned false" }

      println("$TAG =============== END TESTS ===============")
    } catch (error: Throwable) {
      println("$TAG =============== ERROR ===============")
      throw error
    }
  }

  private fun runTestsWithSAFFiles(
    fileManager: FileManager,
    baseDirSAF: AbstractFile,
    baseDirFile: AbstractFile
  ) {
    val time = measureTimeMillis {
      SimpleTest("$TAG SimpleTest").runTests(fileManager, baseDirSAF)
      CreateFilesTest("$TAG CreateFilesTest").runTests(fileManager, baseDirSAF)
      SnapshotTest("$TAG SnapshotTest").runTests(fileManager, baseDirSAF)
      DeleteTest("$TAG DeleteTest").runTests(fileManager, baseDirSAF)
      CopyTest("$TAG CopyTest").runTests(
        fileManager,
        baseDirSAF,
        baseDirFile,
        CopyTestType.FromSafDirToRegularDir
      )
    }

    println("$TAG runTestsWithSAFFiles took ${time}ms")
  }

  private fun runTestsWithJavaFiles(
    fileManager: FileManager,
    baseDirSAF: AbstractFile,
    baseDirFile: AbstractFile
  ) {
    val time = measureTimeMillis {
      SimpleTest("$TAG SimpleTest").runTests(fileManager, baseDirFile)
      CreateFilesTest("$TAG CreateFilesTest").runTests(fileManager, baseDirFile)
      SnapshotTest("$TAG SnapshotTest").runTests(fileManager, baseDirFile)
      DeleteTest("$TAG DeleteTest").runTests(fileManager, baseDirFile)
      CopyTest("$TAG CopyTest").runTests(
        fileManager,
        baseDirFile,
        baseDirSAF,
        CopyTestType.FromRegularDitToSafDir
      )
    }

    println("$TAG runTestsWithJavaFiles took ${time}ms")
  }
}