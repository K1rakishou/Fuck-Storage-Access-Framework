package com.github.k1rakishou.fsaf.callback

import android.content.Intent

interface StartActivityCallbacks {
  fun myStartActivityForResult(intent: Intent, requestCode: Int)
}