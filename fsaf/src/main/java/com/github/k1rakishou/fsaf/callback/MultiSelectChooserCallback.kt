package com.github.k1rakishou.fsaf.callback

import android.net.Uri

interface MultiSelectChooserCallback {
  fun onResult(uris: List<Uri>)
  fun onCancel(reason: String)
}