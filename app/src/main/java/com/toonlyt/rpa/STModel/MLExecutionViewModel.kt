package com.toonlyt.rpa.STModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MLExecutionViewModel : ViewModel() {

  private val _styledBitmap = MutableLiveData<ModelExecutionResult>()

  val styledBitmap: LiveData<ModelExecutionResult>
    get() = _styledBitmap

  private val viewModelJob = Job()
  private val viewModelScope = CoroutineScope(viewModelJob)

  fun onApplyStyle(
          context: Context,
          contentFilePath: String,
          styleFilePath: String,
          styleTransferModelExecutor: StyleTransferModelExecutor,
          inferenceThread: ExecutorCoroutineDispatcher
  ) {
    viewModelScope.launch(inferenceThread) {
      val result =
        styleTransferModelExecutor.execute(contentFilePath, styleFilePath, context)
      _styledBitmap.postValue(result)
    }
  }
}
