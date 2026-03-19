package com.sid.catfeeder

import android.content.Context
import android.graphics.Rect
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.IOException

class CaptureActivityPortrait : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var preview: FrameLayout
    private lateinit var tvStatus: TextView
    private lateinit var focusRect: View

    private var camera: Camera? = null
    private var isScanning = true
    private var autoFocusHandler = Handler(Looper.getMainLooper())
    private var autoFocusRunnable = object : Runnable {
        override fun run() {
            if (camera != null && isScanning) {
                camera?.autoFocus { success, _ ->
                    if (success) {
                        autoFocusHandler.postDelayed(this, 2000)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_portrait)

        surfaceView = findViewById(R.id.surface_view)
        preview = findViewById(R.id.preview)
        tvStatus = findViewById(R.id.tv_status)
        focusRect = findViewById(R.id.focus_rect)

        setupCamera()
    }

    private fun setupCamera() {
        val holder = surfaceView.holder
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                initCamera(holder)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // Не нужно
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releaseCamera()
            }
        })
    }

    private fun initCamera(holder: SurfaceHolder) {
        try {
            camera = Camera.open()
            val parameters = camera?.parameters

            // Улучшаем настройки камеры
            parameters?.let { params ->
                params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                params.sceneMode = Camera.Parameters.SCENE_MODE_BARCODE
                params.zoom = 0

                // Оптимальный размер превью
                val previewSize = getOptimalPreviewSize(params.supportedPreviewSizes, 800, 600)
                if (previewSize != null) {
                    params.setPreviewSize(previewSize.width, previewSize.height)
                }

                camera?.parameters = params
            }

            camera?.setPreviewDisplay(holder)
            camera?.startPreview()

            // Запускаем автофокус
            autoFocusHandler.post(autoFocusRunnable)

            // Запускаем поток распознавания
            startScanning()

        } catch (e: IOException) {
            Log.e("CaptureActivity", "Ошибка инициализации камеры", e)
            finish()
        }
    }

    private fun startScanning() {
        Thread {
            val reader = MultiFormatReader()

            while (isScanning) {
                try {
                    camera?.setOneShotPreviewCallback { data, camera ->
                        val parameters = camera.parameters
                        val size = parameters.previewSize

                        // Создаем изображение для распознавания
                        val source = PlanarYUVLuminanceSource(
                            data, size.width, size.height,
                            0, 0, size.width, size.height, false
                        )

                        val binarizer = HybridBinarizer(source)
                        val bitmap = BinaryBitmap(binarizer)

                        try {
                            val result = reader.decode(bitmap)

                            runOnUiThread {
                                if (isScanning) {
                                    isScanning = false
                                    tvStatus.text = "✅ Найден: ${result.text}"

                                    // Возвращаем результат
                                    val intent = android.content.Intent()
                                    intent.putExtra("SCAN_RESULT", result.text)
                                    setResult(RESULT_OK, intent)

                                    // Небольшая задержка перед закрытием
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        finish()
                                    }, 1000)
                                }
                            }
                        } catch (e: Exception) {
                            // Не найдено, продолжаем
                        }
                    }

                    Thread.sleep(100)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun getOptimalPreviewSize(sizes: List<Camera.Size>?, width: Int, height: Int): Camera.Size? {
        if (sizes == null) return null

        val targetRatio = height.toDouble() / width
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(ratio - targetRatio)
            }
        }

        return optimalSize
    }

    private fun releaseCamera() {
        isScanning = false
        autoFocusHandler.removeCallbacks(autoFocusRunnable)
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
    }
}