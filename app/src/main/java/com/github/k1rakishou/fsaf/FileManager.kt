package com.github.k1rakishou.fsaf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.github.k1rakishou.fsaf.callback.DirectoryChooserCallback
import com.github.k1rakishou.fsaf.callback.FileChooserCallback
import com.github.k1rakishou.fsaf.callback.FileCreateCallback
import com.github.k1rakishou.fsaf.callback.StartActivityCallbacks
import com.github.k1rakishou.fsaf.extensions.copyInto
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.ExternalFile
import com.github.k1rakishou.fsaf.file.RawFile
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.util.*

class FileManager(
  private val appContext: Context
) {
  private val fileChooser = FileChooser(appContext)

  /**
   * Used for calling Android File picker
   * */
  fun setCallbacks(startActivityCallbacks: StartActivityCallbacks) {
    fileChooser.setCallbacks(startActivityCallbacks)
  }

  fun removeCallbacks() {
    fileChooser.removeCallbacks()
  }

  //=======================================================
  // Api to open file/directory chooser and handling the result
  //=======================================================

  fun openChooseDirectoryDialog(callback: DirectoryChooserCallback) {
    fileChooser.openChooseDirectoryDialog(callback)
  }

  fun openChooseFileDialog(callback: FileChooserCallback) {
    fileChooser.openChooseFileDialog(callback)
  }

  fun openCreateFileDialog(filename: String, callback: FileCreateCallback) {
    fileChooser.openCreateFileDialog(filename, callback)
  }

  fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    return fileChooser.onActivityResult(requestCode, resultCode, data)
  }

  //=======================================================
  // Api to convert native file/documentFile classes into our own abstractions
  //=======================================================

//  fun baseSaveLocalDirectoryExists(): Boolean {
//    val baseDirFile = newSaveLocationFile()
//    if (baseDirFile == null) {
//      return false
//    }
//
//    if (!baseDirFile.exists()) {
//      return false
//    }
//
//    return true
//  }
//
//  fun baseLocalThreadsDirectoryExists(): Boolean {
//    val baseDirFile = newLocalThreadFile()
//    if (baseDirFile == null) {
//      return false
//    }
//
//    if (!baseDirFile.exists()) {
//      return false
//    }
//
//    return true
//  }

  /**
   * Create a raw file from a path.
   * Use this method to convert a java File by this path into an AbstractFile.
   * Does not create file on the disk automatically!
   * */
  fun fromPath(path: String): RawFile {
    return fromRawFile(File(path))
  }

  /**
   * Create RawFile from Java File.
   * Use this method to convert a java File into an AbstractFile.
   * Does not create file on the disk automatically!
   * */
  fun fromRawFile(file: File): RawFile {
    if (file.isFile) {
      return RawFile(AbstractFile.Root.FileRoot(file, file.name))
    }

    return RawFile(AbstractFile.Root.DirRoot(file))
  }

  /**
   * Create an external file from Uri.
   * Use this method to convert external file uri (file that may be located at sd card) into an
   * AbstractFile. If a file does not exist null is returned.
   * Does not create file on the disk automatically!
   * */
  fun fromUri(uri: Uri): ExternalFile? {
    val documentFile = toDocumentFile(uri)
    if (documentFile == null) {
      return null
    }

    return if (documentFile.isFile) {
      val filename = documentFile.name
      if (filename == null) {
        throw IllegalStateException("fromUri() queryTreeName() returned null")
      }

      ExternalFile(appContext, AbstractFile.Root.FileRoot(documentFile, filename))
    } else {
      ExternalFile(appContext, AbstractFile.Root.DirRoot(documentFile))
    }
  }

//  /**
//   * Instantiates a new AbstractFile with the root being in the local threads directory.
//   * Does not create file on the disk automatically!
//   * */
//  fun newLocalThreadFile(): AbstractFile? {
//    if (ChanSettings.localThreadLocation.get().isEmpty()
//      && ChanSettings.localThreadsLocationUri.get().isEmpty()
//    ) {
//      // wtf?
//      throw RuntimeException(
//        "Both local thread save locations are empty! " +
//          "Something went terribly wrong."
//      )
//    }
//
//    val uri = ChanSettings.localThreadsLocationUri.get()
//    if (uri.isNotEmpty()) {
//      // When we change localThreadsLocation we also set localThreadsLocationUri to an
//      // empty string, so we need to check whether the localThreadsLocationUri is empty or not,
//      // because saveLocation is never empty
//      val rootDirectory = DocumentFile.fromTreeUri(appContext, Uri.parse(uri))
//      if (rootDirectory == null) {
//        return null
//      }
//
//      return ExternalFile(
//        appContext,
//        AbstractFile.Root.DirRoot(rootDirectory)
//      )
//    }
//
//    val path = ChanSettings.localThreadLocation.get()
//    return RawFile(AbstractFile.Root.DirRoot(File(path)))
//  }
//
//  /**
//   * Instantiates a new AbstractFile with the root being in the app's base directory (either the Kuroba
//   * directory in case of using raw file api or the user's selected directory in case of using SAF).
//   * Does not create file on the disk automatically!
//   * */
//  fun newSaveLocationFile(): AbstractFile? {
//    if (ChanSettings.saveLocation.get().isEmpty() && ChanSettings.saveLocationUri.get().isEmpty()) {
//      // wtf?
//      throw RuntimeException("Both save locations are empty! Something went terribly wrong.")
//    }
//
//    val uri = ChanSettings.saveLocationUri.get()
//    if (uri.isNotEmpty()) {
//      // When we change saveLocation we also set saveLocationUri to an empty string, so we need
//      // to check whether the saveLocationUri is empty or not, because saveLocation is never
//      // empty
//      val rootDirectory = DocumentFile.fromTreeUri(appContext, Uri.parse(uri))
//      if (rootDirectory == null) {
//        return null
//      }
//
//      return ExternalFile(
//        appContext,
//        AbstractFile.Root.DirRoot(rootDirectory)
//      )
//    }
//
//    val path = ChanSettings.saveLocation.get()
//    return RawFile(AbstractFile.Root.DirRoot(File(path)))
//  }

  /**
   * Copy one file's contents into another
   * */
  fun copyFileContents(source: AbstractFile, destination: AbstractFile): Boolean {
    return try {
      source.getInputStream()?.use { inputStream ->
        destination.getOutputStream()?.use { outputStream ->
          inputStream.copyInto(outputStream)
          true
        }
      } ?: false
    } catch (e: IOException) {
      Log.e(TAG, "IOException while copying one file into another", e)
      false
    }
  }

  /**
   * VERY SLOW!!! DO NOT EVEN THINK RUNNING THIS ON THE MAIN THREAD!!!
   * */
  fun copyDirectoryWithContent(
    sourceDir: AbstractFile,
    destDir: AbstractFile,
    includeEmptyDirectories: Boolean,
    updateFunc: (Int, Int) -> Unit
  ): Boolean {
    if (!sourceDir.exists()) {
      Log.e(TAG, "Source directory does not exists, path = ${sourceDir.getFullPath()}")
      return false
    }

    if (sourceDir.listFiles().isEmpty()) {
      Log.d(TAG, "Source directory is empty, nothing to copy")
      return true
    }

    if (!destDir.exists()) {
      Log.e(TAG, "Destination directory does not exists, path = ${sourceDir.getFullPath()}")
      return false
    }

    if (!sourceDir.isDirectory()) {
      Log.e(TAG, "Source directory is not a directory, path = ${sourceDir.getFullPath()}")
      return false
    }

    if (!destDir.isDirectory()) {
      Log.e(TAG, "Destination directory is not a directory, path = ${destDir.getFullPath()}")
      return false
    }

    val files = collectAllFilesInDirTree(sourceDir, includeEmptyDirectories)
    if (files.isEmpty()) {
      // No files, so do nothing
      Log.d(TAG, "No files were collected, nothing to copy")
      return true
    }

    val totalFilesCount = files.size
    val prefix = sourceDir.getFullPath()

    for ((currentFileIndex, file) in files.withIndex()) {
      // Holy shit this hack is so fucking disgusting and may break literally any minute.
      // If this shit breaks then blame google for providing such a retarded fucking API.

      // Basically we have a directory, let's say /123 and we want to copy all
      // of it's files into /456. So we collect every file in /123 then we iterate every
      // collected file, remove base directory prefix (/123 in this case) and recreate this
      // file with the same directory structure in another base directory (/456 in this case).
      // Let's was we have the following files:
      //
      // /123/1.txt
      // /123/111/2.txt
      // /123/222/3.txt
      //
      // After calling this method we will have these files copied into /456:
      //
      // /456/1.txt
      // /456/111/2.txt
      // /456/222/3.txt
      //
      val fileInNewDirectory = destDir
        .clone()
        .appendFileNameSegment(file.getFullPath().removePrefix(prefix))
        .createNew()

      if (fileInNewDirectory == null) {
        Log.e(TAG, "Couldn't create inner file with name ${file.getName()}")
        return false
      }

      if (!copyFileContents(file, fileInNewDirectory)) {
        Log.e(TAG, "Couldn't copy one file into another")
        return false
      }

      updateFunc.invoke(currentFileIndex, totalFilesCount)
    }

    return true
  }

  fun countAllFilesInDirTree(sourceDir: AbstractFile): Int {
    if (!sourceDir.exists()) {
      Log.e(TAG, "Source directory does not exists, path = ${sourceDir.getFullPath()}")
      return 0
    }

    if (sourceDir.listFiles().isEmpty()) {
      Log.d(TAG, "Source directory is empty")
      return 0
    }

    if (!sourceDir.isDirectory()) {
      Log.e(TAG, "Source directory is not a directory, path = ${sourceDir.getFullPath()}")
      return 0
    }

    return collectAllFilesInDirTree(sourceDir, false).size
  }

  fun collectAllFilesInDirTree(
    sourceDir: AbstractFile,
    includeEmptyDirs: Boolean = false
  ): List<AbstractFile> {
    if (!sourceDir.exists()) {
      Log.e(TAG, "Source directory does not exists, path = ${sourceDir.getFullPath()}")
      return emptyList()
    }

    if (sourceDir.listFiles().isEmpty()) {
      Log.d(TAG, "Source directory is empty, nothing to copy")
      return emptyList()
    }

    if (!sourceDir.isDirectory()) {
      Log.e(TAG, "Source directory is not a directory, path = ${sourceDir.getFullPath()}")
      return emptyList()
    }

    val queue = LinkedList<AbstractFile>()
    val files = mutableListOf<AbstractFile>()
    queue.offer(sourceDir)

    // Collect all of the inner files in the source directory
    while (queue.isNotEmpty()) {
      val file = queue.poll()
      if (file.isDirectory()) {
        val innerFiles = file.listFiles()
        if (innerFiles.isEmpty() && includeEmptyDirs) {
          files.add(file)
        }

        innerFiles.forEach { queue.offer(it) }
      } else {
        files.add(file)
      }
    }

    return files
  }

  fun forgetSAFTree(directory: AbstractFile): Boolean {
    if (directory !is ExternalFile) {
      // Only ExternalFile is being used with SAF
      return true
    }

    val uri = Uri.parse(directory.getFullPath())

    if (!directory.exists()) {
      Log.e(
        TAG,
        "Couldn't revoke permissions from directory because it does not exist, path = $uri"
      )
      return false
    }

    if (!directory.isDirectory()) {
      Log.e(TAG, "Couldn't revoke permissions from directory it is not a directory, path = $uri")
      return false
    }

    return try {
      val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

      appContext.contentResolver.releasePersistableUriPermission(uri, flags)
      appContext.revokeUriPermission(uri, flags)

      Log.d(TAG, "Revoke old path permissions success on $uri")
      true
    } catch (err: Exception) {
      Log.e(TAG, "Error revoking old path permissions on $uri", err)
      false
    }
  }

  // TODO: comment
  fun emptyFastFileSearchTree(): FastFileSearchTree {
    return FastFileSearchTree()
  }

  // TODO: comment
  fun newFastFileSearchTree(sourceDir: ExternalFile): FastFileSearchTree? {
    val files = collectAllFilesInDirTree(sourceDir)
    if (files.isEmpty()) {
      Log.e(TAG, "No files in the directory to build FastFileSearchTree from")
      return null
    }

    check(files.all { file -> file is ExternalFile }) {
      "collectAllFilesInDirTree() returned at least one file that is not an instance of ExternalFile"
    }

    return FastFileSearchTree().apply {
      insertFiles(files as List<ExternalFile>)
    }
  }

  private fun toDocumentFile(uri: Uri): DocumentFile? {
    if (!DocumentFile.isDocumentUri(appContext, uri)) {
      Log.e(TAG, "Not a DocumentFile, uri = $uri")
      return null
    }

    val treeUri = try {
      // Will throw an exception if uri is not a treeUri. Hacky as fuck but I don't know
      // another way to check it.
      DocumentFile.fromTreeUri(appContext, uri)
    } catch (ignored: IllegalArgumentException) {
      null
    }

    if (treeUri != null) {
      return treeUri
    }

    return try {
      DocumentFile.fromSingleUri(appContext, uri)
    } catch (e: IllegalArgumentException) {
      Log.e(TAG, "Provided uri is neither a treeUri nor singleUri, uri = $uri")
      null
    }
  }

  companion object {
    private const val TAG = "FileManager"
  }
}