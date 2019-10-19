package com.github.k1rakishou.fsaf_test_app

import android.net.Uri
import com.github.k1rakishou.fsaf.manager.AbstractBaseDirectory

class TestBaseDirectory(dirUri: Uri) : AbstractBaseDirectory(dirUri) {

  override fun baseDirectoryId(): String = BASE_DIR_ID

  companion object {
    const val BASE_DIR_ID = "test_base_dir_id"
  }
}