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

class CustomCaptureActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var previewOverlay: FrameLayout
    private lateinit var tvStatus: TextView
    private lateinit var focusRect: View
    private lateinit var tvInstruction: TextView

    private var camera: Camera? = null
    private var isScanning = true
    private var autoFocusHandler = Handler(Looper.getMainLooper())

    // Автофокус каждые 2 секунды
    private val autoFocusRunnable = object : Runnable {
        override fun run() {
            if (camera != null && isScanning) {
                camera?.autoFocus { success, _ ->
                    if (success) {
                        autoFocusHandler.postDelayed(this, 2000)
                    } else {
                        // Если автофокус не удался, пробуем снова через 1 секунду
                        autoFocusHandler.postDelayed(this, 1000)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_capture)

        surfaceView = findViewById(R.id.surface_view)
        previewOverlay = findViewById(R.id.preview_overlay)
        tvStatus = findViewById(R.id.tv_status)
        focusRect = findViewById(R.id.focus_rect)
        tvInstruction = findViewById(R.id.tv_instruction)

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
            parameters?.let { params ->
                // Оптимальные настройки для распознавания штрихкодов
                params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                params.sceneMode = Camera.Parameters.SCENE_MODE_BARCODE

                // Устанавливаем максимальное разрешение превью
                val previewSizes = params.supportedPreviewSizes
                val optimalSize = getOptimalPreviewSize(previewSizes, 1280, 720)
                optimalSize?.let {
                    params.setPreviewSize(it.width, it.height)
                }

                // Настройки экспозиции
                if (params.isAutoExposureLockSupported) {
                    params.autoExposureLock = false
                }

                camera?.parameters = params
            }

            camera?.setPreviewDisplay(holder)
            camera?.startPreview()

            // Запускаем автофокус
            autoFocusHandler.post(autoFocusRunnable)

            // Запускаем непрерывное сканирование
            startContinuousScan()

        } catch (e: IOException) {
            Log.e("CustomCapture", "Ошибка камеры", e)
            finish()
        }
    }

    private fun startContinuousScan() {
        val reader = MultiFormatReader()
        val hints = mapOf<DecodeHintType, Any>(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_128
            )
        )
        reader.setHints(hints)

        Thread {
            var consecutiveSuccess = 0
            var lastResult = ""

            while (isScanning) {
                try {
                    camera?.setOneShotPreviewCallback { data, camera ->
                        val parameters = camera.parameters
                        val size = parameters.previewSize

                        // Создаем изображение для анализа
                        val source = PlanarYUVLuminanceSource(
                            data, size.width, size.height,
                            size.width / 4, size.height / 4,
                            size.width / 2, size.height / 2,
                            false
                        )

                        val binarizer = HybridBinarizer(source)
                        val bitmap = BinaryBitmap(binarizer)

                        try {
                            val result = reader.decode(bitmap)

                            runOnUiThread {
                                tvStatus.text = "✅ Распознано: ${result.text}"
                                tvStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                            }

                            // Проверка стабильности (3 одинаковых результата подряд)
                            if (result.text == lastResult) {
                                consecutiveSuccess++
                                if (consecutiveSuccess >= 3) {
                                    // Стабильный результат - завершаем сканирование
                                    runOnUiThread {
                                        tvStatus.text = "✅ Успешно: ${result.text}"
                                        val intent = android.content.Intent()
                                        intent.putExtra("SCAN_RESULT", result.text)
                                        setResult(RESULT_OK, intent)

                                        Handler(Looper.getMainLooper()).postDelayed({
                                            finish()
                                        }, 500)
                                    }
                                    isScanning = false
                                }
                            } else {
                                consecutiveSuccess = 1
                                lastResult = result.text
                            }

                        } catch (e: Exception) {
                            // Не найдено
                            runOnUiThread {
                                tvStatus.text = "🔍 Сканирование..."
                                tvStatus.setTextColor(resources.getColor(android.R.color.white))
                            }
                            consecutiveSuccess = 0
                            lastResult = ""
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