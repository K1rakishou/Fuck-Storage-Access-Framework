package com.github.k1rakishou.fsaf_test_app.tests

import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.DirectorySegment
import com.github.k1rakishou.fsaf.file.FileSegment
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.system.measureTimeMillis

class CreateFilesTest(
  tag: String,
  isFastMode: Boolean
) : BaseTest(tag, isFastMode) {

  fun runTests(fileManager: FileManager, baseDir: AbstractFile) {
    kotlin.run {
      val time = measureTimeMillis {
        test1(fileManager, baseDir)
      }

      log("test1 took ${time}ms")
    }
  }

  private fun test1(fileManager: FileManager, baseDir: AbstractFile) {
    fileManager.deleteContent(baseDir)
    checkDirEmpty(fileManager, baseDir)

    val dir = fileManager.createDir(
      baseDir,
      "test"
    )

    if (dir == null || !fileManager.exists(dir) || !fileManager.isDirectory(dir)) {
      throw TestException("Couldn't create directory")
    }

    val files = 25

    for (i in 0 until files) {
      val fileName = "${i}.txt"

      val createdFile = fileManager.create(
        dir,
        DirectorySegment(i.toString()),
        FileSegment(fileName)
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