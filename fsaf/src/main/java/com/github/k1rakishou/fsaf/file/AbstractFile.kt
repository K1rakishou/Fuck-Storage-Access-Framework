package com.github.k1rakishou.fsaf.file

import com.github.k1rakishou.fsaf.extensions.extension
import com.github.k1rakishou.fsaf.extensions.splitIntoSegments

/**
 * An abstraction class over both the Java File and the Storage Access Framework DocumentFile.
 * */
abstract class AbstractFile(
  protected val root: Root<*>,
  protected val segments: List<Segment>
) : HasFileManagerId {

  /**
   * Returns not-yet-applied to the Root segments
   * */
  fun getFileSegments(): List<Segment> {
    return segments
  }

  /**
   * Returns the current root of this file
   * */
  fun <T> getFileRoot(): Root<T> {
    return root as Root<T>
  }

  /**
   * Returns full file path whether it's a DocumentFile (in this case the uri string is returned)
   * or a regular File
   * */
  abstract fun getFullPath(): String

  /**
   * Takes a path string that may look like this: "123/456/" or like this "/123/456/" or like this
   * "123/456" or like this "123/456/test.txt" and splits it into a list of [Segment]s
   * If the last segment of [path] has an extension assumes it as a [FileSegment], if it doesn't then
   * assumes it as a [DirectorySegment]. This function is unsafe. Only use it if you are certain in
   * your input. Otherwise use other versions of [clone]. This method exists only because
   * sometimes it's really tedious to create segments by hand.
   * */
  fun cloneUnsafe(path: String): AbstractFile {
    val segmentStrings = path.splitIntoSegments()

    val segments = segmentStrings.mapIndexed { index, segmentString ->
      if (index == segmentStrings.lastIndex && segmentString.extension() != null) {
        return@mapIndexed FileSegment(segmentString)
      }

      return@mapIndexed DirectorySegment(segmentString)
    }

    return clone(segments)
  }

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
            "index = ${index}/${newSegments.lastIndex}, bad name = ${segment.name}"
        }
      }
    }

    return cloneInternal(newSegments)
  }

  /**
   * When doing something with an [AbstractFile] (like appending a sub-dir or a filename) the
   * [AbstractFile]'s internal state will be modified. So if you don't want to modify the original
   * [AbstractFile] you need to create it's copy via one of the [clone] methods (for example,
   * if you want to search for a couple of files in the same directory you would
   * want to clone the directory's [AbstractFile] and then append the filenames to the copy.
   * The original file won't be changed only the copy).
   *
   * [newSegments] parameter is a list of path segments that will be appended to the file after cloning.
   * May be empty.
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