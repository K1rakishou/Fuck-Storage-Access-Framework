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

  @Test
  fun `Empty FastFileSearchTree must only have Root node`() {
    val fastFileSearchTree = FastFileSearchTree()
    assertEquals(FastFileSearchTreeNode.ROOT, fastFileSearchTree.root.getCurrentNodeName())
  }

  @Test
  fun `test insert many unique nodes`() {
    val fastFileSearchTree = FastFileSearchTree()

    assertTrue(fastFileSearchTree.insertManySegments(segmentsList))
    assertEquals(10, fastFileSearchTree.root.getCurrentChildren().size)

    segmentsList.forEach { pathToVisit ->
      fastFileSearchTree.visitPath(pathToVisit) { index, node ->
        assertEquals(pathToVisit[index], node.getCurrentNodeName())
        assertEquals(1, node.getCurrentChildren().size)

        return@visitPath true
      }
    }

    val children = fastFileSearchTree.root.getCurrentChildren()

    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["0"]!!.getCurrentChildren()["00"]!!.getCurrentChildren()["000.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["1"]!!.getCurrentChildren()["11"]!!.getCurrentChildren()["111.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["2"]!!.getCurrentChildren()["22"]!!.getCurrentChildren()["222.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["3"]!!.getCurrentChildren()["33"]!!.getCurrentChildren()["333.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["4"]!!.getCurrentChildren()["44"]!!.getCurrentChildren()["444.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["5"]!!.getCurrentChildren()["55"]!!.getCurrentChildren()["555.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["6"]!!.getCurrentChildren()["66"]!!.getCurrentChildren()["666.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["7"]!!.getCurrentChildren()["77"]!!.getCurrentChildren()["777.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["8"]!!.getCurrentChildren()["88"]!!.getCurrentChildren()["888.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["9"]!!.getCurrentChildren()["99"]!!.getCurrentChildren()["999.txt"]!!.getCurrentNodeName()
    )

    nodeTypesCheck(fastFileSearchTree)
  }

  @Test
  fun `test many not unique nodes`() {
    val fastFileSearchTree = FastFileSearchTree()

    assertTrue(fastFileSearchTree.insertManySegments(segmentsList))
    assertTrue(fastFileSearchTree.insertManySegments(segmentsList))
    assertEquals(10, fastFileSearchTree.root.getCurrentChildren().size)

    segmentsList.forEach { pathToVisit ->
      fastFileSearchTree.visitPath(pathToVisit) { index, node ->
        assertEquals(pathToVisit[index], node.getCurrentNodeName())
        assertEquals(1, node.getCurrentChildren().size)

        return@visitPath true
      }
    }

    val children = fastFileSearchTree.root.getCurrentChildren()

    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["0"]!!.getCurrentChildren()["00"]!!.getCurrentChildren()["000.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["1"]!!.getCurrentChildren()["11"]!!.getCurrentChildren()["111.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["2"]!!.getCurrentChildren()["22"]!!.getCurrentChildren()["222.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["3"]!!.getCurrentChildren()["33"]!!.getCurrentChildren()["333.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["4"]!!.getCurrentChildren()["44"]!!.getCurrentChildren()["444.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["5"]!!.getCurrentChildren()["55"]!!.getCurrentChildren()["555.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["6"]!!.getCurrentChildren()["66"]!!.getCurrentChildren()["666.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["7"]!!.getCurrentChildren()["77"]!!.getCurrentChildren()["777.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["8"]!!.getCurrentChildren()["88"]!!.getCurrentChildren()["888.txt"]!!.getCurrentNodeName()
    )
    assertEquals(
      FastFileSearchTreeNode.LEAF,
      children["9"]!!.getCurrentChildren()["99"]!!.getCurrentChildren()["999.txt"]!!.getCurrentNodeName()
    )

    nodeTypesCheck(fastFileSearchTree)
  }

  @Test
  fun `test contains not existing node returns false`() {
    val fastFileSearchTree = FastFileSearchTree()
    assertFalse(fastFileSearchTree.containsSegment(listOf("123", "456", "111.txt")))
  }

  private fun nodeTypesCheck(fastFileSearchTree: FastFileSearchTree) {
    val collectedNodes = mutableListOf<FastFileSearchTreeNode>()
    fastFileSearchTree.visit { node ->
      collectedNodes += node
      return@visit true
    }

    val rootNodes = collectedNodes.filter { node -> node.isRoot() }
    val leafNodes = collectedNodes.filter { node -> node.isLeaf() }
    val nodes = collectedNodes.filter { node -> node.isNode() }

    assertEquals(1, rootNodes.size)
    assertEquals(10, leafNodes.size)
    assertEquals(20, nodes.size)
  }
}