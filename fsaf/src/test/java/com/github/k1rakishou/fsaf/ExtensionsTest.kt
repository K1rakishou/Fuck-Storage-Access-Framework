package com.github.k1rakishou.fsaf

import com.github.k1rakishou.fsaf.extensions.splitIntoSegments
import junit.framework.Assert.assertEquals
import org.junit.Test

class ExtensionsTest {

  @Test
  fun testSplitIntoSegments() {
    val string = "content://com.android.externalstorage.documents/tree/0EF0-1C1F%3Atest/document/0EF0-1C1F%3Atest"

    /**
     * com.android.externalstorage.documents
     * tree
     * 0EF0-1C1F%3Atest
     * document
     * 0EF0-1C1F%3Atest
     * */

    val segments = string.splitIntoSegments()
    assertEquals(5, segments.size)

    assertEquals("com.android.externalstorage.documents", segments[0])
    assertEquals("tree", segments[1])
    assertEquals("0EF0-1C1F%3Atest", segments[2])
    assertEquals("document", segments[3])
    assertEquals("0EF0-1C1F%3Atest", segments[4])
  }
}