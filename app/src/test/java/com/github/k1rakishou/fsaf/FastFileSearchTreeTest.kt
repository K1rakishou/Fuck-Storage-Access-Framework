package com.github.k1rakishou.fsaf

import org.junit.Assert.*
import org.junit.Test

class FastFileSearchTreeTest {

  private val segmentsList = listOf(
    listOf("0", "00", "000.txt"),
    listOf("1", "11", "111.txt"),
    listOf("2", "22", "222.txt"),
    listOf("3", "33", "333.txt"),
    listOf("4", "44", "444.txt"),
    listOf("5", "55", "555.txt"),
    listOf("6", "66", "666.txt"),
    listOf("7", "77", "777.txt"),
    listOf("8", "88", "888.txt"),
    listOf("9", "99", "999.txt")
  )
  private val valuesList = listOf(
    0,
    111,
    222,
    333,
    444,
    555,
    666,
    777,
    888,
    999
  )

  @Test
  fun `Empty FastFileSearchTree must only have Root node`() {
    val fastFileSearchTree = FastFileSearchTree<Int>()
    assertEquals(FastFileSearchTreeNode.ROOT, fastFileSearchTree.root.getNodeName())
  }

  @Test
  fun `test insert many unique nodes`() {
    val fastFileSearchTree = FastFileSearchTree<Int>()
    val pairs = segmentsList.zip(valuesList) { first, second -> Pair(first, second) }

    assertTrue(fastFileSearchTree.insertManySegments(pairs))
    assertEquals(10, fastFileSearchTree.root.getNodeChildren().size)

    segmentsList.forEach { pathToVisit ->
      fastFileSearchTree.visitPath(pathToVisit) { index, node ->
        assertEquals(pathToVisit[index], node.getNodeName())
        assertEquals(1, node.getNodeChildren().size)
        assertEquals(valuesList[index], node.getNodeChildren())

        return@visitPath true
      }
    }

    val children = fastFileSearchTree.root.getNodeChildren()

    assertChildrenAreLeafs(children)
    nodeTypesCheck(fastFileSearchTree)
  }

  @Test
  fun `test many not unique nodes`() {
    val fastFileSearchTree = FastFileSearchTree<Int>()
    val pairs = segmentsList.zip(valuesList) { first, second -> Pair(first, second) }

    assertTrue(fastFileSearchTree.insertManySegments(pairs))
    assertTrue(fastFileSearchTree.insertManySegments(pairs))
    assertEquals(10, fastFileSearchTree.root.getNodeChildren().size)

    segmentsList.forEach { pathToVisit ->
      fastFileSearchTree.visitPath(pathToVisit) { index, node ->
        assertEquals(pathToVisit[index], node.getNodeName())
        assertEquals(1, node.getNodeChildren().size)
        assertEquals(valuesList[index], node.getNodeChildren())

        return@visitPath true
      }
    }

    val children = fastFileSearchTree.root.getNodeChildren()

    assertChildrenAreLeafs(children)
    nodeTypesCheck(fastFileSearchTree)
  }

  @Test
  fun `test contains not existing node returns false`() {
    val fastFileSearchTree = FastFileSearchTree<Int>()
    assertFalse(fastFileSearchTree.containsSegment(listOf("123", "456", "111.txt")))
  }

  @Test
  fun `test contains existing returns true`() {
    val fastFileSearchTree = FastFileSearchTree<Int>()
    val segments = listOf("123", "456", "111.txt")

    assertTrue(fastFileSearchTree.insertSegments(segments, 1))
    assertTrue(fastFileSearchTree.containsSegment(segments))
    assertEquals(1, fastFileSearchTree.findSegment(segments))
  }

  private fun assertChildrenAreLeafs(children: MutableMap<String, FastFileSearchTreeNode<Int>>) {
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["0"]!!.getNodeChildren()["00"]!!.getNodeChildren()["000.txt"]!!.getNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["1"]!!.getNodeChildren()["11"]!!.getNodeChildren()["111.txt"]!!.getNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["2"]!!.getNodeChildren()["22"]!!.getNodeChildren()["222.txt"]!!.getNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["3"]!!.getNodeChildren()["33"]!!.getNodeChildren()["333.txt"]!!.getNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["4"]!!.getNodeChildren()["44"]!!.getNodeChildren()["444.txt"]!!.getNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["5"]!!.getNodeChildren()["55"]!!.getNodeChildren()["555.txt"]!!.getNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["6"]!!.getNodeChildren()["66"]!!.getNodeChildren()["666.txt"]!!.getNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["7"]!!.getNodeChildren()["77"]!!.getNodeChildren()["777.txt"]!!.getNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["8"]!!.getNodeChildren()["88"]!!.getNodeChildren()["888.txt"]!!.getNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["9"]!!.getNodeChildren()["99"]!!.getNodeChildren()["999.txt"]!!.getNodeName()
    )
  }

  private fun nodeTypesCheck(fastFileSearchTree: FastFileSearchTree<Int>) {
    val collectedNodes = mutableListOf<FastFileSearchTreeNode<Int>>()
    fastFileSearchTree.visit { node ->
      collectedNodes += node
    }

    val rootNodes = collectedNodes.filter { node -> node.isRoot() }
    val leafNodes = collectedNodes.filter { node -> node.isLeaf() }
    val nodes = collectedNodes.filter { node -> node.isNode() }

    assertEquals(1, rootNodes.size)
    assertEquals(10, leafNodes.size)
    assertEquals(20, nodes.size)
  }
}