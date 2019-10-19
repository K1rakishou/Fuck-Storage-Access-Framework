package com.github.k1rakishou.fsaf.file

import com.github.k1rakishou.fsaf.extensions.extension

/**
 * An abstraction class over both the Java File and the new Storage Access Framework DocumentFile.
 * */
abstract class AbstractFile(
  protected val root: Root<*>,

  /**
   * /test/123/test2/filename.txt -> 4 segments
   * */
  protected val segments: MutableList<Segment>
) : HasFileManagerId {

  fun getFileSegments(): List<Segment> {
    return segments
  }

  /**
   * Returns the current root of this file
   * */
  fun <T> getFileRoot(): Root<T> {
    return root as Root<T>
  }

  abstract fun getFullPath(): String

  /**
   * Clones the file and appends new segment
   * */
  fun clone(newSegment: Segment): AbstractFile {
    return clone(listOf(newSegment))
  }

  /**
   * Clones the file and appends new segments (newSegments may be empty)
   * */
  fun clone(vararg newSegments: Segment): AbstractFile {
    return clone(newSegments.toList())
  }

  /**
   * Clones the file and appends new segments (newSegments may be empty)
   * */
  fun clone(newSegments: List<Segment>): AbstractFile {
    if (newSegments.isNotEmpty()) {
      newSegments.forEach { segment ->
        require(segment.name.isNotBlank()) { "Bad name: ${segment.name}" }
      }

      check(!isFilenameAppended()) { "Cannot append anything after file name has been appended" }

      newSegments.forEachIndexed { index, segment ->
        require(!(segment.name.extension() != null && index != newSegments.lastIndex)) {
          "Only the last segment may have an extension, bad segment " +
            "index = ${index}/${newSegments.lastIndex}, bad name = $segment.name"
        }
      }
    }

    return cloneInternal(newSegments)
  }

  /**
   * When doing something with an [AbstractFile] (like appending a subdir or a filename) the
   * [AbstractFile]'s internal state will be modified. So if you don't want to modify the original
   * [AbstractFile] you need to create it's copy via the [cloneInternal] method
   * (for example, if you want to search for a couple of files in the same directory you would
   * want to clone the directory's [AbstractFile] and then append the filename to the copy.
   * The original file won't be changed only the copy).
   *
   * [newSegments] parameter appends new segments to the original segments after cloning.
   * May be empty
   * */
  protected abstract fun cloneInternal(newSegments: List<Segment>): AbstractFile

  private fun isFilenameAppended(): Boolean =
    segments.lastOrNull()?.isFileName ?: false


  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other === this) {
      return true
    }

    if (other.javaClass != this.javaClass) {
      return false
    }

    other as AbstractFile
    return other.getFullPath() == this.getFullPath()
  }

  override fun hashCode(): Int {
    return getFullPath().hashCode()
  }

  override fun toString(): String {
    return getFullPath()
  }
}