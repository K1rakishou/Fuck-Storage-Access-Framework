package com.github.k1rakishou.fsaf.util

object SAFUtils {

  /**
   * Merges paths that are fully contained in other paths, e.g.:
   *
   * "/123" and "/123/456" and "/123/456/789" -> "/123/456/789"
   * or
   * "/123/456" and "/123/456" -> "/123/456"
   * or
   * "/123/456" and "/123" -> "/123/456"
   * */
  fun mergePaths(pathList: List<String>): List<String> {
    if (pathList.isEmpty()) {
      return emptyList()
    }

    val capacity = if (pathList.size <= 1) {
      1
    } else {
      pathList.size / 2
    }

    val processed = HashSet<Int>(capacity)
    val filtered = HashSet<Int>(capacity)

    for (i in pathList.indices) {
      for (j in pathList.indices) {
        if (i == j) {
          continue
        }

        if (i in processed || i in filtered || j in processed || j in filtered) {
          continue
        }

        val path1 = pathList[i]
        val path2 = pathList[j]

        if (path1.length > path2.length) {
          continue
        }

        if (path2.contains(path1)) {
          filtered += i
          processed += i
        }
      }
    }

    return pathList.filterIndexed { index, _ -> index !in filtered }
  }

}