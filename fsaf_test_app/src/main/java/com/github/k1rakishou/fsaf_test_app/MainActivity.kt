package com.github.k1rakishou.fsaf_test_app

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.k1rakishou.fsaf.FileChooser
import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.callback.DirectoryChooserCallback
import com.github.k1rakishou.fsaf.callback.FSAFActivityCallbacks
import com.github.k1rakishou.fsaf.util.SAFHelper
import com.github.k1rakishou.fsaf_test_app.tests.TestSuite
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity(), FSAFActivityCallbacks {
  private lateinit var testSuite: TestSuite

  private lateinit var fastFileManager: FileManager
  private lateinit var fileChooser: FileChooser

  private lateinit var sharedPreferences: SharedPreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    sharedPreferences = getSharedPreferences("test", MODE_PRIVATE)

    fastFileManager = FileManager(applicationContext)
    fileChooser = FileChooser(applicationContext)
    testSuite = TestSuite(fastFileManager)
    fileChooser.setCallbacks(this)

    if (getTreeUri() != null) {
      fastFileManager.registerBaseDir(TestBaseDirectory(getTreeUri()!!))
    }

    updateControls()

    open_document_tree_button.setOnClickListener {
      fileChooser.openChooseDirectoryDialog(object : DirectoryChooserCallback() {
        override fun onCancel(reason: String) {
          Toast.makeText(
            this@MainActivity,
            "Canceled, reason: $reason",
            Toast.LENGTH_SHORT
          ).show()
        }

        override fun onResult(uri: Uri) {
          println("treeUri = ${uri}")
          Toast.makeText(
            this@MainActivity,
            "treeUri = ${uri}",
            Toast.LENGTH_SHORT
          ).show()

          storeTreeUri(uri)
          updateControls()
        }
      })
    }

    forget_document_tree_button.setOnClickListener {
      removeTreeUri()
      updateControls()
    }

    run_tests_button.setOnClickListener {
      try {
        val baseSAFDir = fastFileManager.newBaseDirectoryFile(TestBaseDirectory.BASE_DIRECTORY_ID)!!
        val baseFileApiDir = fastFileManager.fromRawFile(File(Environment.getDownloadCacheDirectory(), "test"))

        testSuite.runTests(
          baseSAFDir,
          baseFileApiDir
        )

        val message = "=== ALL TESTS HAVE PASSED ==="
        println(message)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
      } catch (error: Throwable) {
        error.printStackTrace()
        Toast.makeText(this, error.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun updateControls() {
    val uri = getTreeUri()

    if (uri != null && SAFHelper.isTreeUri(uri)) {
      open_document_tree_button.isEnabled = false
      forget_document_tree_button.isEnabled = true
      run_tests_button.isEnabled = true
    } else {
      open_document_tree_button.isEnabled = true
      forget_document_tree_button.isEnabled = false
      run_tests_button.isEnabled = false
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    fileChooser.removeCallbacks()
  }

  override fun fsafStartActivityForResult(intent: Intent, requestCode: Int) {
    startActivityForResult(intent, requestCode)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    fileChooser.onActivityResult(requestCode, resultCode, data)
  }

  private fun storeTreeUri(uri: Uri) {
    fastFileManager.registerBaseDir(TestBaseDirectory(uri))
    sharedPreferences.edit().putString(TREE_URI, uri.toString()).apply()
  }

  private fun removeTreeUri() {
    val treeUri = getTreeUri()
    if (treeUri == null) {
      println("Already removed")
      return
    }

    fileChooser.forgetSAFTree(treeUri)
    fastFileManager.unregisterBaseDir(treeUri)
    sharedPreferences.edit().remove(TREE_URI).apply()
  }

  private fun getTreeUri(): Uri? {
    return sharedPreferences.getString(TREE_URI, null)
      ?.let { str -> Uri.parse(str) }
  }

  companion object {
    const val TREE_URI = "tree_uri"
  }
}
