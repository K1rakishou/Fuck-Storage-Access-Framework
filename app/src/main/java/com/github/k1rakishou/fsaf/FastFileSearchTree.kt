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
class FastFileSearchTree<T>(
  val root: FastFileSearchTreeNode<T> = FastFileSearchTreeNode(nodeType = FastFileSearchTreeNode.NodeType.Root)
) {

  fun insertFile(file: ExternalFile, value: T): Boolean {
    val segmentNames = file.getFileSegments().map { segment -> segment.name }
    require(segmentNames.isNotEmpty()) { "file segments must not be empty" }

    return root.insert(segmentNames, value)
  }

  fun insertFiles(files: List<Pair<ExternalFile, T>>) {
    files.forEach { (file, value) -> insertFile(file, value) }
  }

  fun contains(file: ExternalFile): Boolean {
    val segmentNames = file.getFileSegments().map { segment -> segment.name }
    require(segmentNames.isNotEmpty()) { "file segments must not be empty" }

    return root.contains(segmentNames)
  }

  fun find(file: ExternalFile): T? {
    val segmentNames = file.getFileSegments().map { segment -> segment.name }
    require(segmentNames.isNotEmpty()) { "file segments must not be empty" }

    return root.find(segmentNames)
  }

  fun visitPath(segmentNames: List<String>, func: (Int, FastFileSearchTreeNode<T>) -> Boolean) {
    require(segmentNames.isNotEmpty()) {
      "segments to visit list must not be empty"
    }

    root.visitPath(segmentNames, 0, func)
  }

  fun visit(func: (FastFileSearchTreeNode<T>) -> Unit) {
    func(root)
    root.visit(func)
  }

  // Just to make it easier for the GC
  fun clear() {
    root.clear()
  }

  fun withTree(func: FastFileSearchTree<T>.() -> Unit) {
    func(this)
    clear()
  }

  /**
   * ===================================
   * For tests
   * ===================================
   * */
  fun insertSegments(segmentNames: List<String>, value: T): Boolean {
    require(segmentNames.isNotEmpty()) { "file segments must not be empty" }
    return root.insert(segmentNames, value)
  }

  fun insertManySegments(manySegmentNames: List<Pair<List<String>, T>>): Boolean {
    return manySegmentNames.all { (segments, value) -> insertSegments(segments, value) }
  }

  fun containsSegment(segmentNames: List<String>): Boolean {
    require(segmentNames.isNotEmpty()) { "file segments must not be empty" }
    return root.contains(segmentNames)
  }

  fun findSegment(segmentNames: List<String>): T? {
    require(segmentNames.isNotEmpty()) { "file segments must not be empty" }
    return root.find(segmentNames)
  }

  // ===================================

  override fun toString(): String {
    return root.toString()
  }
}

class FastFileSearchTreeNode<V>(
  // Current directory segment
  private var nodeType: NodeType = NodeType.Leaf,
  // Parent directory
  private var parent: FastFileSearchTreeNode<V>? = null,
  private var value: V? = null,
  // Files inside this directory
  private val children: MutableMap<String, FastFileSearchTreeNode<V>> = mutableMapOf()
) {

  fun getNodeName(): String = nodeType.name
  fun getNodeParent(): FastFileSearchTreeNode<V>? = parent
  fun getNodeChildren(): MutableMap<String, FastFileSearchTreeNode<V>> = children
  fun getNodeValue(): V? = value

  fun insert(segments: List<String>, value: V): Boolean {
    if (segments.isEmpty()) {
      return false
    }

    val firstSegment = segments.first()

    if (!children.containsKey(firstSegment)) {
      children[firstSegment] = FastFileSearchTreeNode(parent = this)
    }

    return children[firstSegment]?.insertInternal(segments, value, 1)
      ?: false
  }

  fun contains(segmentNames: List<String>): Boolean {
    return findInternal(segmentNames, 0) != null
  }

  fun find(segmentNames: List<String>): V? {
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
    value: V,
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

    val nextSegment = segmentNames.getOrNull(index + 1)
    if (nextSegment == null) {
      this.value = value
      return true
    }

    return newNode.insertInternal(
      segmentNames,
      value,
      index + 1
    )
  }

  private fun findInternal(
    segmentNames: List<String>,
    index: Int
  ): V? {
    val currentNodeType = segmentNames.getOrNull(index)
      ?: return null

    if (isRoot()) {
      val nextNode = children[currentNodeType]
        ?: return null

      return nextNode.findInternal(segmentNames, index + 1)
    }

    if (currentNodeType != nodeType.name) {
      return null
    }

    val nextNode = children[currentNodeType]
      ?: return null

    if (nextNode.isLeaf()) {
      return value
    }

    return nextNode.findInternal(segmentNames, index + 1)
  }

  fun visitPath(
    segmentNames: List<String>,
    index: Int,
    func: (Int, FastFileSearchTreeNode<V>) -> Boolean
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

  fun visit(func: (FastFileSearchTreeNode<V>) -> Unit) {
    children.forEach { (_, node) ->
      func(node)
      node.visit(func)
    }
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

    other as FastFileSearchTreeNode<V>
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