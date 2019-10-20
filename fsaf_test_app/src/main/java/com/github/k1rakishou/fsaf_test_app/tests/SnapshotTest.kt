package com.github.k1rakishou.fsaf_test_app.tests

import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.ExternalFile
import com.github.k1rakishou.fsaf_test_app.extensions.splitIntoSegments
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.system.measureTimeMillis

class SnapshotTest(
  tag: String,
  isFastMode: Boolean
) : BaseTest(tag, isFastMode) {

  fun runTests(fileManager: FileManager, baseDir: AbstractFile) {
    runTest(fileManager, baseDir) {
      val time = measureTimeMillis {
        test1(fileManager, baseDir)
      }

      log("test1 took ${time}ms")
    }
  }

  private fun test1(fileManager: FileManager, baseDir: AbstractFile) {
    val dir = fileManager.createDir(
      baseDir,
      "test"
    )

    if (dir == null || !fileManager.exists(dir) || !fileManager.isDirectory(dir)) {
      throw TestException("Couldn't create directory")
    }

    createFiles(fileManager, dir)

    fileManager.withSnapshot(dir as ExternalFile, true) {
      val files = fileManager.listSnapshotFiles(dir, false)

      val tests = 10
      for (i in 0 until tests) {
        val time = measureTimeMillis {
          for ((index, file) in files.withIndex()) {
            val expectedName = "${index}.txt"
            val actualName = fileManager.getName(file)

            if (expectedName != actualName) {
              throw TestException("Expected ${expectedName} but got ${actualName}")
            }

            if (!fileManager.exists(file)) {
              throw TestException("File ${file.getFullPath()} does not exist")
            }

            if (!fileManager.isFile(file)) {
              throw TestException("File ${file.getFullPath()} is not a file")
            }

            fileManager.getLength(file)
            fileManager.lastModified(file)

            if (!fileManager.canRead(file)) {
              throw TestException("Cannot read ${file.getFullPath()}")
            }

            if (!fileManager.canWrite(file)) {
              throw TestException("Cannot write to ${file.getFullPath()}")
            }
          }
        }

        log("withSnapshot test ${i} out of $tests, time = ${time}ms")
      }
    }

    val segments = dir.getFullPath().splitIntoSegments()

    if (fileManager.getExternalFileManager().getFastFileSearchTree().containsSegment(segments)) {
      throw TestException("Node ${segments} still exists")
    }
  }

  private fun createFiles(
    fileManager: FileManager,
    dir: AbstractFile
  ) {
    val count = 25

    for (i in 0 until count) {
      val fileName = "${i}.txt"

      val createdFile = fileManager.createFile(
        dir,
        fileName
      )

      if (createdFile == null || !fileManager.exists(createdFile) || !fileManager.isFile(createdFile)) {
        throw TestException("Couldn't create file name")
      }

      if (fileManager.getName(createdFile) != fileName) {
        throw TestException("Bad name ${fileManager.getName(createdFile)}")
      }

      fileManager.getOutputStream(createdFile)?.use { os ->
        DataOutputStream(os).use { dos ->
          dos.writeUTF(fileName)
        }
      } ?: throw TestException("Couldn't get output stream, file = ${createdFile.getFullPath()}")

      fileManager.getInputStream(createdFile)?.use { `is` ->
        DataInputStream(`is`).use { dis ->
          val readString = dis.readUTF()

          if (readString != fileName) {
            throw TestException("Wrong value read, expected = ${fileName}, actual = ${readString}")
          }
        }
      } ?: throw TestException("Couldn't get input stream, file = ${createdFile.getFullPath()}")
    }
  }

}