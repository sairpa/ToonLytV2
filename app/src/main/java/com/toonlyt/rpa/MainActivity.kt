package com.toonlyt.rpa
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestOptions
import com.toonlyt.rpa.STModel.MLExecutionViewModel
import com.toonlyt.rpa.STModel.ModelExecutionResult
import com.toonlyt.rpa.STModel.StyleFragment
import com.toonlyt.rpa.STModel.StyleTransferModelExecutor
import com.toonlyt.rpa.camera.CameraFragment
import com.toonlyt.rpa.camera.ImageUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import org.tensorflow.lite.examples.rpa.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.concurrent.Executors

private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
private const val TAG = "MainActivity"
class MainActivity :
  AppCompatActivity(),
        StyleFragment.OnListFragmentInteractionListener,
  CameraFragment.OnCaptureFinished {
  var context:Context = this
  private var isRunningModel = false
  private val stylesFragment: StyleFragment = StyleFragment()
  private var selectedStyle: String = ""
  private lateinit var cameraFragment: CameraFragment
  private lateinit var viewModel: MLExecutionViewModel
  private lateinit var viewFinder: FrameLayout
  private lateinit var resultImageView: ImageView
  private lateinit var originalImageView: ImageView
  private lateinit var styleImageView: ImageView
  private var rerunButton: Int = 0
  private val pickImage = 100
  private lateinit var captureButton: ImageButton
  private lateinit var progressBar: ProgressBar
  lateinit var imgsave:Bitmap
  private lateinit var horizontalScrollView: HorizontalScrollView
  private var lastSavedFile = ""
  private var useGPU = false
  private var imageUri: Uri? = null
  private lateinit var styleTransferModelExecutor: StyleTransferModelExecutor
  private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  private val mainScope = MainScope()
  private var lensFacing = CameraCharacteristics.LENS_FACING_FRONT


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(findViewById(R.id.Toolbar))
    viewFinder = findViewById(R.id.view_finder)
    resultImageView = findViewById(R.id.result_imageview)
    originalImageView = findViewById(R.id.original_imageview)

    styleImageView = findViewById(R.id.style_imageview)
    captureButton = findViewById(R.id.capture_button)
    progressBar = findViewById(R.id.progress_circular)
    horizontalScrollView = findViewById(R.id.horizontal_scroll_view)

    if (allPermissionsGranted()) {
      addCameraFragment()
    } else {
      ActivityCompat.requestPermissions(
        this,
        REQUIRED_PERMISSIONS,
        REQUEST_CODE_PERMISSIONS
      )
    }


    viewModel = AndroidViewModelFactory(application).create(MLExecutionViewModel::class.java)

    viewModel.styledBitmap.observe(
      this,
      Observer { resultImage ->
        if (resultImage != null) {
          updateUIWithResults(resultImage)
        }
      }
    )


    mainScope.async(inferenceThread) {
      styleTransferModelExecutor = StyleTransferModelExecutor(this@MainActivity, useGPU)
      Log.d(TAG, "Executor created")
    }




    rerunButton = 0

    styleImageView.setOnClickListener {
      if (!isRunningModel) {
        stylesFragment.show(supportFragmentManager, "StylesFragment")
      }
    }

    progressBar.visibility = View.INVISIBLE
    lastSavedFile = getLastTakenPicture()
    setImageView(originalImageView, lastSavedFile)

    animateCameraButton()
    setupControls()
    enableControls(true)
 context = applicationContext
    btn_save.setOnClickListener {
      enableControls(false)
      saveMediaToStorage(imgsave)
      enableControls(true)
    }
    Log.d(TAG, "finished onCreate!!")
  }
  fun Toastmaker(s: String, i: Int){
    if(i==1){
      Toast.makeText(applicationContext, s, Toast.LENGTH_SHORT).show()
    }
    else{
      Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }
  }

  fun openGallery(view: View){

      view.setOnClickListener {
        Toastmaker("Feature in works",0)
      }

  }

  fun saveMediaToStorage(bitmap: Bitmap) {
    //file name format TL_Time,jpg
    val filename = "${System.currentTimeMillis()}.jpg"
    var fos: OutputStream? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      context?.contentResolver?.also { resolver ->
        val contentValues = ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
          put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
          put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val imageUri: Uri? =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        fos = imageUri?.let { resolver.openOutputStream(it) }
      }
    } else {
      val imagesDir =
              Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
      val image = File(imagesDir, filename)
      fos = FileOutputStream(image)
    }
    fos?.use {
      //Finally writing the bitmap to the output stream that we opened
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
      Toastmaker("Saved to Memory", 0)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
      menuInflater.inflate(R.menu.options_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when(item.itemId){
      R.id.switch_use_gpu_menu -> {
        item.setChecked(!item.isChecked)

        useGPU = item.isChecked

          if(useGPU){
            Toastmaker("GPU Boost Activated",0)
          }
        else{
            Toastmaker("GPU Boost Deactivated",0)
          }
          enableControls(false)
          mainScope.async(inferenceThread) {
            styleTransferModelExecutor.close()
            styleTransferModelExecutor = StyleTransferModelExecutor(this@MainActivity, useGPU)
            runOnUiThread { enableControls(true) }
        }
        true
      }

      R.id.theme_menu -> {
        item.setChecked(!item.isChecked)
        if(item.isChecked){
          Toastmaker("Dark Mode Activated",0)
          rootlayout.setBackgroundResource(R.drawable.blackback)
        }
        else{
          Toastmaker( "Light Mode Activated",0)
          rootlayout.setBackgroundResource(R.drawable.whiteback)
        }

        true
      }

      R.id.rerun_button_menu -> {
        Toastmaker("Re Running the Model",0)
        if (rerunButton == 1) {
          startRunningModel()
        }
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun animateCameraButton() {
    val animation = AnimationUtils.loadAnimation(this, R.anim.scale_anim)
    animation.interpolator = BounceInterpolator()
    captureButton.animation = animation
    captureButton.animation.start()
  }

  private fun setImageView(imageView: ImageView, image: Bitmap) {
    Glide.with(baseContext)
      .load(image)
      .override(512, 512)
      .fitCenter()
      .into(imageView)
  }

  private fun setImageView(imageView: ImageView, imagePath: String) {
    Glide.with(baseContext)
      .asBitmap()
      .load(imagePath)
      .override(512, 512)
      .apply(RequestOptions().transform(CropTop()))
      .into(imageView)
  }

  private fun updateUIWithResults(modelExecutionResult: ModelExecutionResult) {
    progressBar.visibility = View.INVISIBLE
    resultImageView.visibility = View.VISIBLE
    setImageView(resultImageView, modelExecutionResult.styledImage)
    enableControls(true)
    var msg = modelExecutionResult.executionLog
    imgsave = modelExecutionResult.styledImage
    horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
  }

  private fun enableControls(enable: Boolean) {
    isRunningModel = !enable
    if(enable){
      rerunButton = 1
    }else{
      rerunButton = 0
    }
    captureButton.isEnabled = enable
  }

  private fun setupControls() {
    captureButton.setOnClickListener {
      it.clearAnimation()
      cameraFragment.takePicture()
    }

    findViewById<ImageButton>(R.id.toggle_button).setOnClickListener {
      lensFacing = if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
        CameraCharacteristics.LENS_FACING_FRONT
      } else {
        CameraCharacteristics.LENS_FACING_BACK
      }
      cameraFragment.setFacingCamera(lensFacing)
      addCameraFragment()
    }
  }

  private fun addCameraFragment() {
    cameraFragment = CameraFragment.newInstance()
    cameraFragment.setFacingCamera(lensFacing)
    supportFragmentManager.popBackStack()
    supportFragmentManager.beginTransaction()
      .replace(R.id.view_finder, cameraFragment)
      .commit()
  }

  /**
   * Process result from permission request dialog box, has the request
   * been granted? If yes, start Camera. Otherwise display a toast
   */
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_CODE_PERMISSIONS) {
      if (allPermissionsGranted()) {
        addCameraFragment()
        viewFinder.post { setupControls() }
      } else {
        Toast.makeText(
          this,
          "Permissions not granted by the user.",
          Toast.LENGTH_SHORT
        ).show()
        finish()
      }
    }
  }

  private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
    checkPermission(
      it, Process.myPid(), Process.myUid()
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun onCaptureFinished(file: File) {
    val msg = "Photo capture succeeded: ${file.absolutePath}"
    Log.d(TAG, msg)

    lastSavedFile = file.absolutePath
    setImageView(originalImageView, lastSavedFile)
  }

  private fun getLastTakenPicture(): String {
    val directory = baseContext.filesDir // externalMediaDirs.first()
    var files =
      directory.listFiles()?.filter { file -> file.absolutePath.endsWith(".jpg") }?.sorted()
    if (files == null || files.isEmpty()) {
      Log.d(TAG, "there is no previous saved file")
      return ""
    }

    val file = files.last()
    Log.d(TAG, "lastsavedfile: " + file.absolutePath)
    return file.absolutePath
  }

  override fun onListFragmentInteraction(item: String) {
    Log.d(TAG, item)
    selectedStyle = item
    stylesFragment.dismiss()

    startRunningModel()
  }

  private fun getUriFromAssetThumb(thumb: String): String {
    return "file:///android_asset/thumbnails/$thumb"
  }

  private fun startRunningModel() {
    if (!isRunningModel && lastSavedFile.isNotEmpty() && selectedStyle.isNotEmpty()) {
      val chooseStyleLabel: TextView = findViewById(R.id.choose_style_text_view)
      chooseStyleLabel.visibility = View.GONE
      enableControls(false)
      setImageView(styleImageView, getUriFromAssetThumb(selectedStyle))
      resultImageView.visibility = View.INVISIBLE
      progressBar.visibility = View.VISIBLE
      viewModel.onApplyStyle(
        baseContext, lastSavedFile, selectedStyle, styleTransferModelExecutor,
        inferenceThread
      )
    } else {
      Toast.makeText(this, "Previous Model still running", Toast.LENGTH_SHORT).show()
    }
  }

  // this transformation is necessary to show the top square of the image as the model
  // will work on this part only, making the preview and the result show the same base
  class CropTop : BitmapTransformation() {
    override fun transform(
      pool: BitmapPool,
      toTransform: Bitmap,
      outWidth: Int,
      outHeight: Int
    ): Bitmap {
      return if (toTransform.width == outWidth && toTransform.height == outHeight) {
        toTransform
      } else ImageUtils.scaleBitmapAndKeepRatio(toTransform, outWidth, outHeight)
    }

    override fun equals(other: Any?): Boolean {
      return other is CropTop
    }

    override fun hashCode(): Int {
      return ID.hashCode()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
      messageDigest.update(ID_BYTES)
    }

    companion object {
      private const val ID = "com.rpa.CropTop"
      private val ID_BYTES = ID.toByteArray(Charset.forName("UTF-8"))

    }
  }

}
