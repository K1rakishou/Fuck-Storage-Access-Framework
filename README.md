# !!!WIP!!! 
# Fuck Storage Access Framework (FSAF)

This library in under heavy development so it's not a good idea to use it yet. Some API changes may happen in the future.

If you ever had to deal with Storage Access Framework you must understand the pain imposed on you by it's API and lack of any good examples.

This tiny library attempts to hide away that API, providing a well-known Java File-like API instead, abstracting away both the SAF files (or DocumentFiles) and the standart Java Files. Basically with this library you don't even need to think about what kind of file to use because it's all being figured out internally.

The SAF is slow, especially when you want to do some file operations on many different files. The other goal of this library is to provide an API that will significantly improve file operations when dealing with lots of files or with nested directories with files, etc.

When moving away from the Java File API to SAF, you may encounter problems with migration because some users may not want to change their directories with files right away (And it will probably be a pain in the ass to do that manually). Yet another goal of this library is to make this migration seamless. 

Samples (TODO)
---
