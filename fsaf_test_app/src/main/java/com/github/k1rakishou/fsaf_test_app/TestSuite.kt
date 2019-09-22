package com.github.k1rakishou.fsaf_test_app

import android.net.Uri
import com.github.k1rakishou.fsaf.FileManager
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class TestSuite(
    private val fastFileManager: FileManager,
    private val slowFileManager: FileManager
) {
    private val random = Random(System.currentTimeMillis())

    fun runTests(baseDir: Uri) {
        runTestsWithCaching(baseDir)
        runTestsWithoutCaching(baseDir)
    }

    private fun runTestsWithCaching(baseDir: Uri) {
        fastFileManager.deleteContent(fastFileManager.fromUri(baseDir)!!)

        val time = measureTimeMillis {
            test1(fastFileManager, baseDir)
            test2(fastFileManager, baseDir)
        }

        println("runTestsWithCaching took ${time}ms")
    }

    private fun runTestsWithoutCaching(baseDir: Uri) {
        slowFileManager.deleteContent(slowFileManager.fromUri(baseDir)!!)

        val time = measureTimeMillis {
            test1(slowFileManager, baseDir)
            test2(slowFileManager, baseDir)
        }

        println("runTestsWithoutCaching took ${time}ms")
    }

    private fun test1(fileManager: FileManager, baseDir: Uri) {
        val externalFile = fileManager.fromUri(baseDir)
            ?.appendSubDirSegment("123")
            ?.appendSubDirSegment("456")
            ?.appendSubDirSegment("789")
            ?.appendFileNameSegment("test123.txt")
            ?.createNew()

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

        val externalFile2Exists = fileManager.fromUri(baseDir)
            ?.appendSubDirSegment("123")
            ?.appendSubDirSegment("456")
            ?.appendSubDirSegment("789")
            ?.let { fileManager.exists(it) }


        if (externalFile2Exists == null || !externalFile2Exists) {
            throw TestException("789 directory does not exist")
        }

        val dirToDelete = fileManager.fromUri(baseDir)
            ?.appendSubDirSegment("123")
            ?: throw TestException("fileManager.fromUri(baseDir) returned null, baseDir = ${baseDir}")

        if (!fileManager.delete(dirToDelete) && fileManager.exists(dirToDelete)) {
            throw TestException("Couldn't delete test123.txt")
        }

        val files = fileManager.listFiles(fileManager.fromUri(baseDir)!!)
        if (files.isNotEmpty()) {
            throw TestException("Couldn't not delete some files in the base directory: ${files}")
        }
    }

    private fun test2(fileManager: FileManager, baseDir: Uri) {
        val externalFile = fileManager.fromUri(baseDir)
            ?.appendSubDirSegment("1234")
            ?.appendSubDirSegment("4566")
            ?.appendFileNameSegment("filename.json")
            ?.createNew()

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

        val dir = fileManager.fromUri(baseDir)
            ?.appendSubDirSegment("1234")
            ?.appendSubDirSegment("4566")

        if (dir == null) {
            throw TestException("fileManager.fromUri(baseDir) returned null, baseDir = $baseDir")
        }

        if (fileManager.getName(dir) != "4566") {
            throw TestException("dir.name != 4566, name = " + fileManager.getName(dir))
        }

        val foundFile = fileManager.findFile(dir, "filename.json")
        if (foundFile == null || !fileManager.exists(foundFile)) {
            throw TestException("Couldn't find filename.json")
        }
    }

    private fun test3() {
//        // Write string to the file
//        val testString = "Hello world"
//
//        foundFile!!.withFileDescriptor(FileDescriptorMode.WriteTruncate, { fd ->
//            try {
//                OutputStreamWriter(FileOutputStream(fd)).use({ osw ->
//                    osw.write(testString)
//                    osw.flush()
//                })
//            } catch (e: IOException) {
//                throw
//            }
//
//            Unit
//        })
//
//        if (foundFile!!.getLength() !== testString.length) {
//            throw TestException(("file length != testString.length(), file length = " + foundFile!!.getLength()))
//        }
//
//        foundFile!!.withFileDescriptor(FileDescriptorMode.Read, { fd ->
//            try {
//                InputStreamReader(FileInputStream(fd)).use { isr ->
//                    val stringBytes = CharArray(testString.length)
//                    val read = isr.read(stringBytes)
//
//                    if (read != testString.length) {
//                        throw TestException("read bytes != testString.length(), read = " + read)
//                    }
//
//                    val resultString = String(stringBytes)
//                    if (resultString != testString) {
//                        throw TestException(("resultString != testString, resultString = " + resultString))
//                    }
//
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//
//            Unit
//        })
//
//        // Write another string that is shorter than the previous string
//        val testString2 = "Hello"
//
//        foundFile!!.withFileDescriptor(FileDescriptorMode.WriteTruncate, { fd ->
//            try {
//                OutputStreamWriter(FileOutputStream(fd)).use { osw ->
//                    osw.write(testString2)
//                    osw.flush()
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//
//            Unit
//        })
//
//        if (foundFile!!.getLength() !== testString2.length) {
//            throw TestException(("file length != testString.length(), file length = " + foundFile!!.getLength()))
//        }
//
//        foundFile!!.withFileDescriptor(FileDescriptorMode.Read, { fd ->
//            InputStreamReader(FileInputStream(fd)).use { isr ->
//                val stringBytes = CharArray(testString2.length)
//                val read = isr.read(stringBytes)
//
//                if (read != testString2.length) {
//                    throw TestException("read bytes != testString2.length(), read = $read")
//                }
//
//                val resultString = String(stringBytes)
//                if (resultString != testString2) {
//                    throw TestException(("resultString != testString2, resultString = $resultString"))
//                }
//
//            }
//
//            Unit
//        })
    }

    class TestException(message: String) : Exception(message)
}