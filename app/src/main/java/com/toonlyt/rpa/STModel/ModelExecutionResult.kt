package com.toonlyt.rpa.STModel

import android.graphics.Bitmap

@SuppressWarnings("GoodTime")
data class ModelExecutionResult(
  val styledImage: Bitmap,
  val preProcessTime: Long = 0L,
  val stylePredictTime: Long = 0L,
  val styleTransferTime: Long = 0L,
  val postProcessTime: Long = 0L,
  val totalExecutionTime: Long = 0L,
  val executionLog: String = "",
  val errorMessage: String = ""
)
