# !!!WIP!!! 
# Fuck Storage Access Framework (FSAF)

This library in under heavy development so it's not a good idea to use it yet. 
Some API changes may happen in the future.

If you ever had to deal with Storage Access Framework you must understand the pain 
imposed on you by it's API and lack of any good examples.

This tiny library attempts to hide away that API, providing a well-known Java File-like API instead, 
abstracting away both the SAF files (or DocumentFiles) and the standard Java Files. 
Basically with this library you don't even need to think about what kind of file to use because 
it's all being figured out internally.

The SAF is slow, especially when you want to do some file operations with many different files. 
The other goal of this library is to provide an API that will significantly improve the file 
operations speed when dealing with lots of files or with nested directories with files, etc.

When moving away from the Java File API to SAF, you may encounter problems with migration because 
some users may not want to change their directories with files right away 
(And it will probably be a pain in the ass to do that manually). 
Yet another goal of this library is to make this migration seamless. Or even leave both variants.

Samples
---

There are three main scenarios when dealing with files:
* Read or write to a user-provided file.
* Create a new file in a user-provided directory with a user-provided name.
* Use a user-provided directory as a file-dump throughout the app's lifetime.

The first two a usually pretty simple to implement even with the normal SAF api since it is usually 
a one-time operation. But the third one is not that trivial.
Let's see how this library helps you dealing with these three scenarios when using SAF.

### Read or write to user-provided file

It's pretty simple, just use the `FileChooser.openChooseFileDialog()` method:

```kotlin
fileChooser.openChooseFileDialog(object : FileChooserCallback() {
    override fun onResult(uri: Uri) {
      val externalFile = fileManager.fromUri(uri)
      if (externalFile == null) {
        println("Couldn't convert Uri to an ExternalFile")
        return
      }
      
      println("name = ${fileManager.getName(externalFile)}")
    }

    override fun onCancel(reason: String) {
      println("Canceled by user")
    }
  })
```

### Create a new file in a user-provided directory with the user-provided name.

The same goes for the scenario where you want to create a file inside a directory chosen by the user,
use the `FileChooser.openCreateFileDialog()` method:

```kotlin
fileChooser.openCreateFileDialog("text.txt", object : FileCreateCallback() {
    override fun onResult(uri: Uri) {
      val externalFile = fileManager.fromUri(uri)
      if (externalFile == null) {
        println("Couldn't convert Uri to an ExternalFile")
        return
      }

      println("exists = ${fileManager.exists(externalFile)}")
    }

    override fun onCancel(reason: String) {
      println("Canceled by user")
    }
  })
```

### Use a user-provided directory as a files dump throughout the app's lifetime.

This is where thing start to get pretty hard. First of all you need a directory which you will then
use to store some file (downloaded images/videos etc). That directory will have to have the proper
read/write permissions and the persistence permission. Without the persistence permission you won't
be able to access the directory after the phone restart. FSAF automatically adds all the necessary 
flags to require both read/write and the persistence permission when choosing a directory via the
FileChooser.

```kotlin
fileChooser.openChooseDirectoryDialog(object : DirectoryChooserCallback() {
    override fun onResult(uri: Uri) {
      println("treeUri = ${uri}")
    }
    
    override fun onCancel(reason: String) {
      println("Canceled by user")
    }
  })
```

After getting the directory Uri you will probably have to store it somewhere so you don't lose it.
Then you need to register a `BaseDirectory`. A `BaseDirectory` is a root directory inside of which 
you will be able to create new file/directories/sub-directories etc. You can register a BaseDirectory
by inheriting from a `BaseDirectory` class and by overriding the required methods:

```kotlin
class TestBaseDirectory(
  private val getBaseDirUriFunc: () -> Uri?,
  private val getBaseDirFileFunc: () -> File?
) : BaseDirectory() {

  override fun getDirUri(): Uri? = getBaseDirUriFunc.invoke()
  override fun getDirFile(): File? = getBaseDirFileFunc.invoke()
  override fun currentActiveBaseDirType(): ActiveBaseDirType = ActiveBaseDirType.SafBaseDir
}
```

Then you need to instantiate it and register it in the `FileManager`:

```kotlin
private val testBaseDirectory = TestBaseDirectory({
    getTreeUri()
  }, {
    null
  })
  
fileManager.registerBaseDir<TestBaseDirectory>(testBaseDirectory)
```

And that's it. Now you can create any file or directory inside the base directory.

`BaseDirectory` requires you to override three methods:
* `getDirUri()` you have to return the Uri to the base directory that was returned to you in the 
`onResult` callback after calling `FileChooser.openChooseDirectoryDialog()`. This is your base 
directory's Uri somewhere inside SAF. It may be on a SD-card or on the external phone memory. It 
should always return a non-null value.
* `getDirFile()` this an alternative Java file backed directory. The point of it is that it is 
usually impossible to force users to switch from one thing to another. So this process may take 
some time and to make it seamless for the users you may add an ability for the users to select 
either a Java file backed directory of the SAF directory. And to figure out when kind of base 
directory is currently being used you need to change return value of the third overridden method.
* `currentActiveBaseDirType()` you have to return either `ActiveBaseDirType.SafBaseDir` or
`ActiveBaseDirType.JavaFileBaseDir` depending on what the user has selected. This method is called 
every time you want to create a new file or directory to figure out where it should be created. 
When not using the alternative Java file directory you may want to always return 
`ActiveBaseDirType.SafBaseDir` here.

Here is how a base directory may look like when using both methods:

```kotlin
class SavedFilesBaseDirectory(
) : BaseDirectory() {

    override fun getDirFile(): File? {
        return File(ChanSettings.saveLocation.fileApiBaseDir.get())
    }

    override fun getDirUri(): Uri? {
        return Uri.parse(ChanSettings.saveLocation.safBaseDir.get())
    }

    override fun currentActiveBaseDirType(): ActiveBaseDirType {
        return when {
            ChanSettings.saveLocation.isSafDirActive() -> ActiveBaseDirType.SafBaseDir
            ChanSettings.saveLocation.isFileDirActive() -> ActiveBaseDirType.JavaFileBaseDir
            else -> throw IllegalStateException("SavedFilesBaseDirectory: No active base directory!!!")
        }
    }
} 
```

Where `ChanSettings.saveLocation` and `ChanSettings.saveLocation` are just wrappers over shared 
prefs.

Now that everything is set up lets see how can we create files/directories and use couple of 
standard file operations.

### Creating a new file or directory

It's pretty simple (especially when using Kotlin):

```kotlin
val baseDirectory: AbstractFile = fileManager.newBaseDirectoryFile<TestBaseDirectory>()
```

This will instantiate a new `AbstractFile` class, but IT WILL NOT create anything on the disk yet. 
Think of it like of the regular Java File where, to physically create a file on the disk, you need to
call the `createNew()`/`mkdir()` method first. `AbstractFile` is just a class with no segments and the 
root that is pointing to the base directory. Now lets create a couple of directories and files:

```kotlin
val file1: AbstractFile? = fileManager.create(baseDir, FileSegment("file1.txt"))
val dir1: AbstractFile? = fileManager.create(baseDir, DirectorySegment("dir1"))
val file2: AbstractFile? = fileManager.create(baseDir, DirectorySegment("dir1"), FileSegment("file2.txt"))
val file3: AbstractFile? = fileManager.create(baseDir, DirectorySegment("dir1"), DirectorySegment("dir2"), FileSegment("file3.txt"))
```

This will create `file1.txt` and `dir1` inside the base directory. Then it will create `file2.txt`
inside `dir1` and after that `file3.txt` inside `dir2` inside `dir1`, so it will look like this:

```kotlin
ROOT/file1.txt
ROOT/dir1
ROOT/dir1/file1.txt
ROOT/dir1/file2.txt
ROOT/dir1/dir2/file3.txt
```

And that's it. There couple other overloaded versions of the `create()` method you can find all of 
them in the `FileManager` class.

### Checking whether a base directory exists

User may delete your base directory at any time! So you have to check whether it exists before doing 
anything. Usually you want to do it before calling `FileManager.newBaseDirectoryFile()`. To check
whether a base directory exists use `FileMananger.baseDirectoryExists()` method.

### Forgetting and unregistering a base directory

User may want to change a base directory at any time and you need to handle that. Before registering
a new base directory if an old base directory still exists you may want to give back all of the 
directory permissions (Well, actually nobody is forcing you not to do that but it's a good practice
to do that). Use `FileChooser.forgetSAFTree()` method to revoke any permissions you have for that 
directory. Then you can also remove it from the `FileManager` by using `FileManager.unregisterBaseDir()`.

### Reading from/Writing to a file

You can use for that `FileManager.withFileDescriptor()` method. 
It takes an `AbstractFile` (which must be a file not a directory!) a `FileDescriptorMode` which 
describes what you want to do with a file (read/write/write truncate) and a lambda into 
which the `FileDescriptor` will be passed.

Alternatively you can use `FileManager.getInputStream()` or `FileManager.getOutputStream()`.

### Directory snapshot

SAF is slow. Every SAF file IO takes like 20-30ms because it uses an IPC call to do it. 
And sometimes you may want to check whether a lot of files exist on the disk and if they
don't then create them (or something similar that requires a lot of file operations). It's so slow
that even in google example they use 
[hacks](https://github.com/android/storage-samples/blob/master/ActionOpenDocumentTree/app/src/main/java/com/example/android/ktfiles/CachingDocumentFile.kt#L25) 
in their examples to make it faster. Well this library use hack as well to make even more fast.
Basically if you need to make lots of file operations the fastest way to do this is to read the 
whole directory (with files/sub-directories an all the metadata like file names/file sizes etc) 
at a go into a Tree structure and do all the necessary operations on it. This is what snapshots are 
for.

To create a directory snapshot use `FileManager.createSnapshot()` method. If you want to include 
sub-directories into the snapshot as well use `includeSubDirs` parameter. After creating a snapshot
you can do anything you want with it but after you are done DO NOT FORGET TO RELEASE IT by using
`FileManager.releaseSnapshot()` where you need to provide the same `AbstractFile` as the parameter
which MUST BE a directory.

Alternatively you may use `FileManager.withSnapshot()` which will release the snapshot automatically 
for you.