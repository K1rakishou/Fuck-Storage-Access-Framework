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
   * Behave similarly to Java's mkdirs() method but work not only with directories but files as well.
   * */
  fun create(baseDir: AbstractFile, segments: List<Segment>): AbstractFile?
  fun exists(file: AbstractFile): Boolean
  fun isFile(file: AbstractFile): Boolean
  fun isDirectory(file: AbstractFile): Boolean
  fun canRead(file: AbstractFile): Boolean
  fun canWrite(file: AbstractFile): Boolean
  fun getSegmentNames(file: AbstractFile): List<String>
  fun delete(file: AbstractFile): Boolean
  fun deleteContent(dir: AbstractFile)
  fun getInputStream(file: AbstractFile): InputStream?
  fun getOutputStream(file: AbstractFile): OutputStream?
  fun getName(file: AbstractFile): String
  fun findFile(dir: AbstractFile, fileName: String): AbstractFile?
  fun getLength(file: AbstractFile): Long
  fun listFiles(dir: AbstractFile): List<AbstractFile>
  fun lastModified(file: AbstractFile): Long
  fun <T> withFileDescriptor(file: AbstractFile, fileDescriptorMode: FileDescriptorMode, func: (FileDescriptor) -> T?): T?
}