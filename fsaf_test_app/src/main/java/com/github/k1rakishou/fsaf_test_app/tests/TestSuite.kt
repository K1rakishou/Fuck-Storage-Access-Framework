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

    private fun runTestsWithCaching(
        fileManager: FileManager,
        baseDirSAF: AbstractFile,
        baseDirFile: File
    ) {
        val time = measureTimeMillis {
            SimpleTest("$TAG SimpleTest", true).runTests(fileManager, baseDirSAF)
            CreateFilesTest("$TAG CreateFilesTest", true).runTests(fileManager, baseDirSAF)
            SnapshotTest("$TAG SnapshotTest", true).runTests(fileManager, baseDirSAF)
        }

        println("$TAG runTestsWithCaching took ${time}ms")
    }

    private fun runTestsWithoutCaching(
        fileManager: FileManager,
        baseDirSAF: AbstractFile,
        baseDirFile: File
    ) {
        val time = measureTimeMillis {
            SimpleTest("$TAG SimpleTest", false).runTests(fileManager, baseDirSAF)
            CreateFilesTest("$TAG CreateFilesTest", false).runTests(fileManager, baseDirSAF)
            SnapshotTest("$TAG SnapshotTest", false).runTests(fileManager, baseDirSAF)
        }

        println("$TAG runTestsWithoutCaching took ${time}ms")
    }

}