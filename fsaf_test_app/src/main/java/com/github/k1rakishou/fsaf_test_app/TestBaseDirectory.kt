package com.github.k1rakishou.fsaf_test_app

import android.net.Uri
import com.github.k1rakishou.fsaf.manager.base_directory.BaseDirectory

class TestBaseDirectory(
  dirUri: Uri?
) : BaseDirectory(BASE_DIRECTORY_ID, dirUri, null) {

  companion object {
    const val BASE_DIRECTORY_ID = "test_base_directory"
  }
}