package com.github.k1rakishou.fsaf_test_app.tests

import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.file.AbstractFile
import java.io.File
import kotlin.system.measureTimeMillis

class TestSuite(
    private val fastFileManager: FileManager,
    private val slowFileManager: FileManager
) {
    private val TAG = "TestSuite"

    // TODO: findFile tests with dir and file
    fun runTests(baseDirSAF: AbstractFile, baseDirFile: File) {
        try {
            println("$TAG =============== START TESTS ===============")

            runTestsWithCaching(fastFileManager, baseDirSAF, baseDirFile)
            println(TAG)
            runTestsWithoutCaching(slowFileManager, baseDirSAF, baseDirFile)

            println("$TAG =============== END TESTS ===============")
        } catch (error: Throwable) {
            println("$TAG =============== ERROR ===============")
            throw error
        }
    }

    private fun runTestsWithCaching(fileManager: FileManager, baseDirSAF: AbstractFile, baseDirFile: File) {
        val time = measureTimeMillis {
            SimpleTest("$TAG SimpleTest", true).runTests(fileManager, baseDirSAF)
            CreateFilesTest("$TAG CreateFilesTest", true).runTests(fileManager, baseDirSAF)
            SnapshotTest("$TAG SnapshotTest", true).runTests(fileManager, baseDirSAF)
        }

        println("$TAG runTestsWithCaching took ${time}ms")
    }

    private fun runTestsWithoutCaching(fileManager: FileManager, baseDirSAF: AbstractFile, baseDirFile: File) {
        val time = measureTimeMillis {
            SimpleTest("$TAG SimpleTest", false).runTests(fileManager, baseDirSAF)
            CreateFilesTest("$TAG CreateFilesTest", false).runTests(fileManager, baseDirSAF)
            SnapshotTest("$TAG SnapshotTest", false).runTests(fileManager, baseDirSAF)
        }

        println("$TAG runTestsWithoutCaching took ${time}ms")
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
}