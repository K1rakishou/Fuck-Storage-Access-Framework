package com.github.k1rakishou.fsaf.manager

import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.FileDescriptorMode
import com.github.k1rakishou.fsaf.file.Segment
import java.io.FileDescriptor
import java.io.InputStream
import java.io.OutputStream

interface BaseFileManager {

  /**
   * Creates a new file that consists of the root directory and segments (sub dirs or the file name)
   * Behaves similarly to Java's mkdirs() method but work not only with directories but files as well.
   * */
  fun create(baseDir: AbstractFile, segments: List<Segment>): AbstractFile?

  /**
   * Check whether a file or a directory exists
   * */
  fun exists(file: AbstractFile): Boolean

  /**
   * Check whether this AbstractFile is actually a file on the disk
   * */
  fun isFile(file: AbstractFile): Boolean

  /**
   * Check whether this AbstractFile is actually a directory
   * */
  fun isDirectory(file: AbstractFile): Boolean

  /**
   * Check whether it is possible to read from this file (or whether a directory has a read
   * permission)
   * */
  fun canRead(file: AbstractFile): Boolean

  /**
   * Check whether it is possible to write into this file (or whether a directory has a write
   * permission)
   * */
  fun canWrite(file: AbstractFile): Boolean

  /**
   * Converts full file/directory path into a list of path segments ("/1/2/3" -> ["1", "2", "3"])
   * */
  fun getSegmentNames(file: AbstractFile): List<String>

  /**
   * Deletes this file or directory
   * */
  fun delete(file: AbstractFile): Boolean

  /**
   * Deletes contents of this directory. Does nothing is [dir] is not actually a directory.
   * */
  fun deleteContent(dir: AbstractFile): Boolean

  /**
   * Returns an input stream created from this file
   * */
  fun getInputStream(file: AbstractFile): InputStream?

  /**
   * Returns an output stream created from this file
   * */
  fun getOutputStream(file: AbstractFile): OutputStream?

  /**
   * Returns a name of this file or directory
   * */
  fun getName(file: AbstractFile): String?

  /**
   * Searches for a file with name [fileName] inside this directory
   * */
  fun findFile(dir: AbstractFile, fileName: String): AbstractFile?

  /**
   * Returns the length of this file
   * */
  fun getLength(file: AbstractFile): Long

  /**
   * Returns a list of all files and directories inside this directory
   * */
  fun listFiles(dir: AbstractFile): List<AbstractFile>

  /**
   * Returns a list of all files and directories inside this cached in FastFileSearchTree directory.
   * If [dir] is backed by a Java File then it just calls listFiles() on it.
   * */
  fun listSnapshotFiles(dir: AbstractFile, recursively: Boolean): List<AbstractFile>

  /**
   * Returns lastModified parameters of this file or directory
   * */
  fun lastModified(file: AbstractFile): Long

  /**
   * Useful method to safely work this this file's fileDescriptor (it is automatically closed upon
   * exiting the callback)
   * */
  fun <T> withFileDescriptor(
    file: AbstractFile,
    fileDescriptorMode: FileDescriptorMode,
    func: (FileDescriptor) -> T?
  ): T?
}