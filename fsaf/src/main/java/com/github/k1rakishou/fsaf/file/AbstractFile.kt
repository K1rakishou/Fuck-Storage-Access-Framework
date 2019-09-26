package com.github.k1rakishou.fsaf.file

import com.github.k1rakishou.fsaf.extensions.extension

// TODO: rewrite
/**
 * An abstraction class over both the Java File and the new Storage Access Framework DocumentFile.
 *
 * Some methods are marked with [MutableMethod] annotation. This means that such methods are gonna
 * mutate the inner data of the [AbstractFile] (such as root or segments). Sometimes this behavior is
 * not desirable. For example, when you have an AbstractFile representing some directory that may
 * not even exists on the disk and you want to check whether it exists and if it does check some
 * additional files inside that directory. In such case you may want to preserve the [AbstractFile]
 * that represents that directory in it's original state. To do this you have to call the [clone]
 * method on the file that represents the directory. It will create a copy of the file that you can
 * safely work without worry that the original file may change.
 *
 * Other methods are marked with [ImmutableMethod] annotation. This means that those methods create a
 * copy of the [AbstractFile] internally and are safe to use without calling [clone]
 *
 * Examples.
 *
 * Usually you want to create an [AbstractFile] pointing to some directory (like the Kuroba base dir)
 * and then create either subdirectories or files inside that directory. You can start with one of the
 * following methods:
 *
 * // Creates an [AbstractFile] from base SAF directory. Be aware that the Uri must not be created
 * // manually! This won't work with SAF since one file may have it's Uri changed when Android decides to
 * // do so. Usually you want to call file or directory chooser via SAF API (there are methods for
 * // that in the [FileManager] class) which will return an Uri that you can then pass into fromUri()
 * // method. But usually you don't even need to do this since we usually do this once to get the
 * // Kuroba base directory and then just do our work inside of that directory.
 * AbstractFile baseDir = fileManager.fromUri(Uri.parse(ChanSettings.saveLocationUri.get()));
 *
 * // Creates an [AbstractFile] from base raw directory
 * AbstractFile baseDir = fileManager.fromRawFile(new File(ChanSettings.saveLocation.get()));
 *
 * // Same as above
 * AbstractFile baseDir = fileManager.fromPath(ChanSettings.saveLocation.get());
 *
 *
 * Then you can start appending subdirectories or a filename:
 *
 * // This will create a "test.txt" file located at <Kuroba_Base_Dir>/dir1/dir2/dir3, i.e.
 * // <Kuroba_Base_Dir>/dir1/dir2/dir3/test.txt
 * AbstractFile newFile = baseDir
 *      .appendSubDirSegment("dir1")
 *      .appendSubDirSegment("dir2")
 *      .appendSubDirSegment("dir3")
 *      .appendFileNameSegment("test.txt")
 *      .createNew();
 *
 *  Then you can call methods that are similar to the standard Java File API, e.g. [exists],
 *  [getName], [getLength], [isFile], [isDirectory], [canRead], [canWrite] etc.
 *
 *  If you want to work with multiple files in a directory (or sub directories) you may want
 *  to [clone] the file that represents that directory, e.g:
 *
 *  AbstractFile clonedFile = baseDir.clone();
 *
 *  AbstractFile f1 = clonedFile
 *      .appendFileNameSegment("f1.txt")
 *      .createNew();
 *  AbstractFile f2 = clonedFile
 *      .appendFileNameSegment("f2.txt")
 *      .createNew();
 *  AbstractFile f3 = clonedFile
 *      .appendFileNameSegment("f3.txt")
 *      .createNew();
 *
 *  You have to do this because some methods may mutate the internal state of the [AbstractFile], so
 *  after calling, let's say:
 *
 *  AbstractFile f1 = baseDir
 *      .appendFileNameSegment("f1.txt")
 *      .createNew();
 *
 *  Without cloning it first baseDir will be start pointing to <Kuroba_Base_Dir>/f1.txt instead of
 *  just <Kuroba_Base_Dir>. The same thing applies to any method marked with [MutableMethod] annotation.
 *  methods marked with [ImmutableMethod] do this stuff internally so they are safe to use without
 *  cloning.
 *
 *  Sometimes you don't know which external directory to choose to store a new file (the SAF or the
 *  old raw Java File external directory). In this case you can use:
 *
 *  AbstractFile baseDir = fileManager.newSaveLocationFile();
 *
 *  Method which will create an [AbstractFile] with root pointing to either Kuroba SAF base directory
 *  (if user has set it) or if he didn't then to the default external directory (Backed by raw
 *  Java File) or
 *
 *  AbstractFile baseDir = fileManager.newLocalThreadFile();
 *
 *  Method which will create an [AbstractFile] with root pointing to either user's selected local
 *  threads directory or to the default external local threads directory
 *
 *  If you want to search for a lot of files in a directory (including subdirectories) you may want
 *  to take a look at [FastFileSearchTree]
 * */
abstract class AbstractFile(
  protected val root: Root<*>,

  /**
   * /test/123/test2/filename.txt -> 4 segments
   * */
  protected val segments: MutableList<Segment>
) : IsFileManager {

  fun getFileSegments(): List<Segment> {
    return segments
  }

  fun <T> getFileRoot(): Root<T> {
    return root as Root<T>
  }

  abstract fun getFullPath(): String

  /**
   * Clone a file and append new segments (newSegments may be empty)
   * */
  fun clone(vararg newSegments: Segment): AbstractFile {
    return clone(newSegments.toList())
  }

  /**
   * Clone a file and append new segments
   * */
  fun clone(newSegments: List<Segment>): AbstractFile {
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

    return cloneInternal(newSegments)
  }

  /**
   * When doing something with an [AbstractFile] (like appending a subdir or a filename) the
   * [AbstractFile] will change because it's mutable. So if you don't want to change the original
   * [AbstractFile] you need to make a copy via this method (like, if you want to search for
   * a couple of files in the same directory you would want to clone the directory
   * [AbstractFile] and then append the filename to those copies).
   *
   * [newSegments] parameter appends new segments to the original segments after cloning.
   * May be empty
   * */
  protected abstract fun cloneInternal(newSegments: List<Segment>): AbstractFile

  private fun isFilenameAppended(): Boolean =
    segments.lastOrNull()?.isFileName ?: false

  override fun toString(): String {
    return getFullPath()
  }
}