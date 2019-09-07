package com.github.k1rakishou.fsaf.callback

import android.net.Uri

interface ChooserCallback {
  fun onResult(uri: Uri)
  fun onCancel(reason: String)
}