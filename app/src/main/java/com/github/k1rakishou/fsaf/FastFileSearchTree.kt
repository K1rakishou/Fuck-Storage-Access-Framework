package com.github.k1rakishou.fsaf

import com.github.k1rakishou.fsaf.file.ExternalFile
import java.io.File

/**
 * Represents a snapshot of a directory with all of the inner directories and files. Useful, when
 * you have a directory with lots of files and you want to search for many files inside the directory.
 * We don't balance the tree because it is built from already existing balanced file tree.
 *
 * NOTE: Only use this when dealing with SAF files, i.e. ExternalFile. RawFile is backed by the
 * Java File so it doesn't need this at all. That's because ExternalFile is really slow when you
 * want to search for many files.
 * */
class FastFileSearchTree(
  val root: FastFileSearchTreeNode = FastFileSearchTreeNode(FastFileSearchTreeNode.NodeType.Root)
) {

  fun insertFile(file: ExternalFile): Boolean {
    val segmentNames = file.getSegmentNames()
    require(segmentNames.isNotEmpty()) { "file segments must not be empty" }

    return root.insert(segmentNames)
  }

  fun insertFiles(files: List<ExternalFile>) {
    files.forEach { file -> insertFile(file) }
  }

  fun contains(file: ExternalFile): Boolean {
    val segmentNames = file.getSegmentNames()
    require(segmentNames.isNotEmpty()) { "file segments must not be empty" }

    return root.find(segmentNames)
  }

  fun visitPath(segmentNames: List<String>, func: (Int, FastFileSearchTreeNode) -> Boolean) {
    require(segmentNames.isNotEmpty()) {
      "segments to visit list must not be empty"
    }

    root.visitPath(segmentNames, 0, func)
  }

  fun visit(func: (FastFileSearchTreeNode) -> Boolean) {
    func(root)
    root.visit(func)
  }

  // Just to make it easier for the GC
  fun clear() {
    root.clear()
  }

  fun withTree(func: FastFileSearchTree.() -> Unit) {
    func(this)
    clear()
  }

  /**
   * ===================================
   * For tests
   * ===================================
   * */
  fun insertSegments(segmentNames: List<String>): Boolean {
    require(segmentNames.isNotEmpty()) { "file segments must not be empty" }
    return root.insert(segmentNames)
  }

  fun insertManySegments(manySegmentNames: List<List<String>>): Boolean {
    return manySegmentNames.all { segments -> insertSegments(segments) }
  }

  fun containsSegment(segmentNames: List<String>): Boolean {
    require(segmentNames.isNotEmpty()) { "file segments must not be empty" }
    return root.find(segmentNames)
  }

  // ===================================

  override fun toString(): String {
    return root.toString()
  }
}

class FastFileSearchTreeNode(
  // Current directory segment
  private var nodeType: NodeType = NodeType.Leaf,
  // Parent directory
  private var parent: FastFileSearchTreeNode? = null,
  // Files inside this directory
  private val children: MutableMap<String, FastFileSearchTreeNode> = mutableMapOf()
) {

  fun getCurrentNodeName(): String = nodeType.name
  fun getCurrentParent(): FastFileSearchTreeNode? = parent
  fun getCurrentChildren(): MutableMap<String, FastFileSearchTreeNode> = children

  fun insert(segments: List<String>): Boolean {
    if (segments.isEmpty()) {
      return false
    }

    val firstSegment = segments.first()

    if (!children.containsKey(firstSegment)) {
      children[firstSegment] = FastFileSearchTreeNode(parent = this)
    }

    return children[firstSegment]?.insertInternal(segments, 1)
      ?: false
  }

  fun find(segmentNames: List<String>): Boolean {
    return findInternal(segmentNames, 0)
  }

  fun clear() {
    children.values.forEach { node ->
      node.clear()
      node.children.clear()
    }
  }

  fun getFullPath(): String {
    if (parent == null) {
      return nodeType.name
    }

    return checkNotNull(parent).getFullPath() + File.separatorChar + nodeType.name
  }

  fun isRoot(): Boolean {
    return nodeType == NodeType.Root
  }

  fun isLeaf(): Boolean {
    return nodeType == NodeType.Leaf
  }

  fun isNode(): Boolean {
    return !isRoot() && !isLeaf()
  }

  private fun insertInternal(
    segmentNames: List<String>,
    index: Int
  ): Boolean {
    val newSegmentName = segmentNames.getOrNull(index)
      ?: return false

    if (nodeType != NodeType.Leaf && nodeType.name != newSegmentName) {
      // Not found
      return false
    }

    if (nodeType == NodeType.Leaf) {
      check(!children.containsKey(newSegmentName)) {
        "children already contain segment name $newSegmentName even " +
          "though current node doesn't have a segmentName"
      }

      nodeType = NodeType.Node(newSegmentName)
    } else {
      if (children.containsKey(newSegmentName)) {
        // Already exists
        return true
      }
    }

    val newNode = FastFileSearchTreeNode(NodeType.Leaf, this)
    children[newSegmentName] = newNode

    segmentNames.getOrNull(index + 1)
      ?: return true // end reached

    return newNode.insertInternal(
      segmentNames,
      index + 1
    )
  }

  fun visitPath(
    segmentNames: List<String>,
    index: Int,
    func: (Int, FastFileSearchTreeNode) -> Boolean
  ) {
    val currentSegment = segmentNames.getOrNull(index)
      ?: return

    if (nodeType == NodeType.Leaf || nodeType.name != currentSegment) {
      return
    }

    if (!func(index, this)) {
      return
    }

    children[currentSegment]?.visitPath(segmentNames, index + 1, func)
  }

  fun visit(func: (FastFileSearchTreeNode) -> Boolean) {
    children.forEach { (_, node) ->
      if (!func(node)) {
        return@forEach
      }

      node.visit(func)
    }
  }

  private fun findInternal(
    segmentNames: List<String>,
    index: Int
  ): Boolean {
    val currentNodeType = segmentNames.getOrNull(index)
      ?: return false

    if (currentNodeType == nodeType.name) {
      return true
    }

    val nextNode = children[currentNodeType]
      ?: return false

    return nextNode.findInternal(segmentNames, index + 1)
  }

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

    other as FastFileSearchTreeNode
    return this.getFullPath() == other.getFullPath()
  }

  override fun hashCode(): Int {
    return getFullPath().hashCode()
  }

  override fun toString(): String {
    return nodeType.name
  }

  sealed class NodeType(val name: String) {
    object Root : NodeType(ROOT)
    class Node(name: String) : NodeType(name)
    object Leaf : NodeType(LEAF)
  }

  companion object {
    const val ROOT = "<ROOT>"
    const val LEAF = "<LEAF>"
  }
}