package com.github.k1rakishou.fsaf.manager

import com.github.k1rakishou.fsaf.annotation.ImmutableMethod
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.FileDescriptorMode
import java.io.FileDescriptor
import java.io.InputStream
import java.io.OutputStream

interface BaseFileManager {

  /**
   * Creates a new file that consists of the root directory and segments (sub dirs or the file name)
   * Behave similarly to Java's mkdirs() method but work not only with directories but files as well.
   * */
  @ImmutableMethod
  fun create(file: AbstractFile): AbstractFile?

  @ImmutableMethod
  fun exists(file: AbstractFile): Boolean

  @ImmutableMethod
  fun isFile(file: AbstractFile): Boolean

  @ImmutableMethod
  fun isDirectory(file: AbstractFile): Boolean

  @ImmutableMethod
  fun canRead(file: AbstractFile): Boolean

  @ImmutableMethod
  fun canWrite(file: AbstractFile): Boolean

  @ImmutableMethod
  fun getSegmentNames(file: AbstractFile): List<String>

  @ImmutableMethod
  fun delete(file: AbstractFile): Boolean

  @ImmutableMethod
  fun deleteContent(dir: AbstractFile)

  @ImmutableMethod
  fun getInputStream(file: AbstractFile): InputStream?

  @ImmutableMethod
  fun getOutputStream(file: AbstractFile): OutputStream?

  @ImmutableMethod
  fun getName(file: AbstractFile): String

  @ImmutableMethod
  fun findFile(dir: AbstractFile, fileName: String): AbstractFile?

  @ImmutableMethod
  fun getLength(file: AbstractFile): Long

  @ImmutableMethod
  fun listFiles(dir: AbstractFile): List<AbstractFile>

  @ImmutableMethod
  fun lastModified(file: AbstractFile): Long

  @ImmutableMethod
  fun <T> withFileDescriptor(
    file: AbstractFile,
    fileDescriptorMode: FileDescriptorMode,
    func: (FileDescriptor) -> T?): T?
}