package com.example.eyestracker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var tvBlinkCount: TextView
    private lateinit var tvStatus: TextView
    private var blinkCount = 0
    private var isEyeClosed = false
    private var faceLandmarker: FaceLandmarker? = null
    private var lastBlinkTime = System.currentTimeMillis()
    private var lastVoiceWarningTime = 0L // Untuk mengatur jeda suara

    // Inisialisasi Text To Speech
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        tvBlinkCount = findViewById(R.id.tvBlinkCount)
        tvStatus = findViewById(R.id.tvStatus)

        // Setup TTS (Bahasa Indonesia)
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.language = Locale("id", "ID")
            }
        }

        setupFaceLandmarker()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val params = android.app.PictureInPictureParams.Builder().build()
        enterPictureInPictureMode(params)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            tvBlinkCount.textSize = 14f
            tvStatus.textSize = 10f
        } else {
            tvBlinkCount.textSize = 32f
            tvStatus.textSize = 18f
        }
    }

    private fun setupFaceLandmarker() {
        val baseOptionsBuilder = com.google.mediapipe.tasks.core.BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")

        val optionsBuilder = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ -> processResult(result) }
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(this, optionsBuilder)
    }

    private fun processResult(result: FaceLandmarkerResult) {
        val landmarks = result.faceLandmarks()
        val currentTime = System.currentTimeMillis()
        val needsWarning = currentTime - lastBlinkTime > 10000

        if (landmarks.isNotEmpty()) {
            val face = landmarks[0]

            // --- LOGIKA EAR (EYE ASPECT RATIO) ---
            // Mata Kanan: Lebar (33-133), Tinggi (159-145)
            val rightWidth = Math.sqrt(Math.pow((face[33].x() - face[133].x()).toDouble(), 2.0) +
                    Math.pow((face[33].y() - face[133].y()).toDouble(), 2.0))
            val rightHeight = Math.abs(face[145].y() - face[159].y())
            val rightEAR = rightHeight / rightWidth

            // Mata Kiri: Lebar (362-263), Tinggi (386-374)
            val leftWidth = Math.sqrt(Math.pow((face[362].x() - face[263].x()).toDouble(), 2.0) +
                    Math.pow((face[362].y() - face[263].y()).toDouble(), 2.0))
            val leftHeight = Math.abs(face[374].y() - face[386].y())
            val leftEAR = leftHeight / leftWidth

            // Rata-rata EAR
            val averageEAR = (rightEAR + leftEAR) / 2.0

            // Log untuk memantau angka EAR di Logcat
            println("DEBUG_AI: EAR Value = $averageEAR")

            runOnUiThread {
                // Logika Suara Peringatan
                if (needsWarning && (currentTime - lastVoiceWarningTime > 5000)) {
                    tts.speak("Ayo berkedip sekarang", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                    lastVoiceWarningTime = currentTime
                }

                // Ambang batas (threshold) EAR biasanya di 0.20 - 0.25
                if (averageEAR < 0.22) {
                    if (!isEyeClosed) {
                        isEyeClosed = true
                        lastBlinkTime = currentTime // Reset timer kedipan
                        blinkCount++
                        tvBlinkCount.text = "Kedipan: $blinkCount"
                        tvStatus.text = "BERKEDIP!"
                        tvStatus.setTextColor(Color.YELLOW)
                    }
                } else {
                    isEyeClosed = false
                    if (needsWarning) {
                        tvStatus.text = "AYO KEDIP!"
                        tvStatus.setTextColor(Color.RED)
                    } else {
                        tvStatus.text = "Mata Terbuka"
                        tvStatus.setTextColor(Color.WHITE)
                    }
                }
            }
        } else {
            runOnUiThread {
                if (needsWarning) {
                    tvStatus.text = "AYO KEDIP!"
                    tvStatus.setTextColor(Color.RED)
                } else {
                    tvStatus.text = "Cari Wajah..."
                    tvStatus.setTextColor(Color.WHITE)
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        val frameTime = System.currentTimeMillis()
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val bitmap = imageProxy.toBitmap()

                        val matrix = android.graphics.Matrix().apply {
                            postRotate(rotationDegrees.toFloat())
                            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                        }

                        val rotatedBitmap = android.graphics.Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )

                        val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(rotatedBitmap).build()
                        faceLandmarker?.detectAsync(mpImage, frameTime)
                        imageProxy.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) { e.printStackTrace() }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    // Hancurkan TTS saat aplikasi ditutup agar tidak membebani memori
    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}