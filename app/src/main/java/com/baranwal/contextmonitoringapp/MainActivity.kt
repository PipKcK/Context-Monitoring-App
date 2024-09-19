package com.baranwal.contextmonitoringapp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.Manifest
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.ImageAnalysis
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.PermissionChecker
import com.baranwal.contextmonitoringapp.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

import android.hardware.Camera
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.CountDownTimer

import android.os.Environment

import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.withContext
import java.io.File

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

import android.graphics.Color
import android.health.connect.datatypes.RespiratoryRateRecord
import android.net.Uri


import android.view.View

import androidx.activity.enableEdgeToEdge

import androidx.camera.core.*
import androidx.camera.video.*
import androidx.lifecycle.lifecycleScope
//import com.google.android.filament.View
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min


class MainActivity : AppCompatActivity(), SensorEventListener {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var cameraControl: CameraControl

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null  // Handle nullable accelerometer

    private val accelValuesX = mutableListOf<Float>()
    private val accelValuesY = mutableListOf<Float>()
    private val accelValuesZ = mutableListOf<Float>()

    private var HEART: Int = 0
    private var RESPIRATORY: Int = 0

    private fun getHeart(): Int { return HEART }
    private fun setHeart(value: Int) { HEART = value }
    private fun getRespiratory(): Int { return RESPIRATORY }
    private fun setRespiratory(value: Int) { RESPIRATORY = value }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Check if the device has an accelerometer
        if (accelerometer == null) {
            Toast.makeText(this, "No accelerometer found on this device", Toast.LENGTH_LONG).show()
        } else {
            viewBinding.buttonRespiratoryRate.setOnClickListener {
                startRespiratoryRateSensing()
            }
        }

        viewBinding.buttonNext.setOnClickListener {
            Toast.makeText(this, "Heart and Respiratory Rate saved!", Toast.LENGTH_SHORT).show()
            val H = getHeart()
            Log.d("Heart", "Sending Heart Rate ULALALA: $H")
            val R = getRespiratory()
            Log.d("Resp", "Sending Resp Rate ULALALA: $R")

            val intent = Intent(this, SymptomsActivity::class.java).apply {
                putExtra("HEART_RATE", H.toFloat())
                putExtra("RESPIRATORY_RATE", R.toFloat())
            }
            startActivity(intent)
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        viewBinding.buttonHeartRate.setOnClickListener {
            Log.e("Button", "uji Button Pressed")
            captureVideo()
        }
    }

    private val respiratoryRates = mutableListOf<Float>()

    private fun startRespiratoryRateSensing() {
        viewBinding.textViewStatus.text = "Calculating..." // Show calculating status
        viewBinding.buttonNext.visibility = android.view.View.GONE // Hide the save button while calculating

        // Register accelerometer listener
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        // Start countdown for 45 seconds
        startRCountdown()

        // Stop sensing after 45 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            stopRespiratoryRateSensing()
        }, 45000) // 45000 milliseconds = 45 seconds
    }

    private fun startRCountdown() {
        object : CountDownTimer(45000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                viewBinding.buttonRespiratoryRate.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                viewBinding.buttonRespiratoryRate.apply {
                    text = getString(R.string.Breathe)
                    isEnabled = true
                }
            }
        }.start()
    }

    private fun stopRespiratoryRateSensing() {
        // Unregister the listener to stop sensing
        sensorManager.unregisterListener(this)
        viewBinding.textViewStatus.text = "Health App"
        viewBinding.buttonNext.visibility = android.view.View.VISIBLE

        // Calculate the average respiratory rate
        val aRespiratoryRate = respiratoryRates.average().toInt()
        setRespiratory(aRespiratoryRate)
        viewBinding.textViewRespiratory.text = "Respiratory Rate: $aRespiratoryRate"

        // Clear the respiratory rates list
        respiratoryRates.clear()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accelValuesX.add(event.values[0])
            accelValuesY.add(event.values[1])
            accelValuesZ.add(event.values[2])

            if (accelValuesY.size >= 50) {
                val respiratoryRate = respiratoryRateCalculator(accelValuesX, accelValuesY, accelValuesZ)

                // Add the respiratory rate to the list
                respiratoryRates.add(respiratoryRate.toFloat())

                // Clear the sensor data
                accelValuesX.clear()
                accelValuesY.clear()
                accelValuesZ.clear()
            }
        }
    }
    //_________________________________________________

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun respiratoryRateCalculator(
        accelValuesX: MutableList<Float>,
        accelValuesY: MutableList<Float>,
        accelValuesZ: MutableList<Float>,
    ): Int {
        var previousValue = 10f
        var k = 0
        for (i in 11 until accelValuesY.size) {
            val currentValue = sqrt(
                accelValuesZ[i].toDouble().pow(2.0) +
                        accelValuesX[i].toDouble().pow(2.0) +
                        accelValuesY[i].toDouble().pow(2.0)
            ).toFloat()
            if (abs(previousValue - currentValue) > 0.15) {
                k++
            }
            previousValue = currentValue
        }
        val ret = k.toDouble() / 45.0
        return (ret * 30).toInt()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun captureVideo() {
        viewBinding.textViewStatus.text = "Calculating..." // Show calculating status
        viewBinding.buttonNext.visibility = android.view.View.GONE // Hide the save button while calculating
        val videoCapture = this.videoCapture ?: return

        cameraControl.enableTorch(true)

        viewBinding.buttonHeartRate.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@MainActivity,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        viewBinding.buttonHeartRate.apply {
                            text = "45"
                            isEnabled = true
                        }
                        startCountdown()
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)

                            // Print the URI to the log
                            val uri = recordEvent.outputResults.outputUri
                            val path = getRealPathFromURI(uri) // Function to convert URI to actual file path

                            Log.d(TAG, "Video stored at URI [UJ]: $uri")
                            Log.d(TAG, "Actual file path [UJ]: $path")

                            // Call heartRateCalculator with the URI
                            lifecycleScope.launch {
                                val heartRate = heartRateCalculator(recordEvent.outputResults.outputUri, contentResolver)
                                setHeart(heartRate)
                                viewBinding.buttonNext.visibility = android.view.View.VISIBLE

                                viewBinding.textViewHeart.text = "Heart Rate: $heartRate"

                                viewBinding.textViewStatus.text = "Health App" // Show calculating status
                            }
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }

                        cameraControl.enableTorch(false)

                        viewBinding.buttonHeartRate.apply {
                            text = getString(R.string.Heart)
                            isEnabled = true
                        }
                    }
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun heartRateCalculator(uri: Uri, contentResolver: ContentResolver): Int {
        return withContext(Dispatchers.IO) {
            // Log the URI passed into the function
            Log.d("HeartRateCalculator", "Received URI: $uri")

            val result: Int
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(uri, proj, null, null, null)
            val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()
            val path = cursor?.getString(columnIndex ?: 0)
            cursor?.close()

            val sdCardPath = path?.replace("/storage/emulated/0", "/sdcard")


            Log.d("HeartRateCalculator", "EXTRACTED SDCARD Video Path: $sdCardPath")

            val retriever = MediaMetadataRetriever()
            val frameList = ArrayList<Bitmap>()
            try {

                val file = File(path)
                if (!file.exists()) {
                    Log.e("HeartRateCalculator", "File does not exist at path: $path")
                }

                retriever.setDataSource(path)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                val frameDuration = min(duration!!.toInt(), 425)
                var i = 10
                while (i < frameDuration) {
                    val bitmap = retriever.getFrameAtIndex(i)
                    bitmap?.let { frameList.add(it) }
                    i += 15
                }
            } catch (e: Exception) {
                Log.e("HeartRateCalculator", "Error extracting frames: ${e.message}", e)
            } finally {
                retriever.release()
                if (frameList.isEmpty()) {
                    Log.e("MainActivity", "No frames extracted from the video.")
                    return@withContext 0 // Return 0 or any default value if no frames are extracted
                }
                var redBucket: Long
                var pixelCount: Long = 0
                val a = mutableListOf<Long>()
                for (i in frameList) {
                    redBucket = 0
                    for (y in 350 until 450) {
                        for (x in 350 until 450) {
                            val c: Int = i.getPixel(x, y)
                            pixelCount++
                            redBucket += Color.red(c) + Color.blue(c) + Color.green(c)
                        }
                    }
                    a.add(redBucket)
                }
                if (a.size < 5) {
                    Log.e("MainActivity", "Not enough data points for heart rate calculation.")
                    return@withContext 0 // Return 0 or any default value if not enough data points
                }
                val b = mutableListOf<Long>()
                for (i in 0 until a.lastIndex - 5) {
                    val temp = (a.elementAt(i) + a.elementAt(i + 1) + a.elementAt(i + 2) + a.elementAt(i + 3) + a.elementAt(i + 4)) / 4
                    b.add(temp)
                }
                var x = b.elementAt(0)
                var count = 0
                for (i in 1 until b.lastIndex) {
                    val p = b.elementAt(i)
                    if ((p - x) > 3500) {
                        count += 1
                    }
                    x = b.elementAt(i)
                }
                val rate = ((count.toFloat()) * 60).toInt()
                result = (rate / 4)
            }
            result
        }
    }



    private fun startCountdown() {
        object : CountDownTimer(45000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                viewBinding.buttonHeartRate.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                recording?.stop()
                viewBinding.buttonHeartRate.apply {
                    text = getString(R.string.Heart)
                    isEnabled = true
                }
            }
        }.start()
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
                cameraControl = camera.cameraControl
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    fun getRealPathFromURI(uri: Uri): String? {
        var realPath: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex("_data")
                realPath = it.getString(index)
            }
        }
        return realPath
    }
}
