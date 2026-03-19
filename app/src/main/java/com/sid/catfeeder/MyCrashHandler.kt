package com.sid.catfeeder



import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class MyCrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "CrashHandler"
    }

    // Сохраняем стандартный обработчик, чтобы вызвать его после нашего
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 1. Логируем ошибку в Logcat
        Log.e(TAG, "==========================================")
        Log.e(TAG, "🚨 ПРИЛОЖЕНИЕ УПАЛО! 🚨")
        Log.e(TAG, "Thread: ${thread.name} (${thread.id})")
        Log.e(TAG, "Exception: ${throwable.javaClass.name}")
        Log.e(TAG, "Message: ${throwable.message}")
        Log.e(TAG, "==========================================")

        // 2. Получаем полный stack trace
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        // Печатаем в лог
        Log.e(TAG, stackTrace)

        // 3. Сохраняем в файл для последующего анализа
        saveCrashToFile(throwable, stackTrace)

        // 4. Показываем диалог с ошибкой (в главном потоке)
        showErrorDialog(throwable, stackTrace)

        // 5. Небольшая задержка, чтобы пользователь увидел диалог
        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            // Игнорируем
        }

        // 6. Передаем стандартному обработчику (приложение закроется)
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashToFile(throwable: Throwable, stackTrace: String) {
        try {
            // Создаем папку для crash-логов
            val crashDir = android.content.ContextWrapper(context)
                .getDir("crashes", Context.MODE_PRIVATE)

            // Создаем файл с именем crash_YYYYMMDD_HHMMSS.log
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val fileName = "crash_$timestamp.log"
            val crashFile = java.io.File(crashDir, fileName)

            // Записываем информацию об ошибке
            val writer = java.io.PrintWriter(crashFile)
            writer.println("=== CRASH REPORT ===")
            writer.println("Date: ${Date()}")
            writer.println("App: CatFeeder")
            writer.println("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            writer.println("Android: ${android.os.Build.VERSION.SDK_INT}")
            writer.println("==================================")
            writer.println("Exception: ${throwable.javaClass.name}")
            writer.println("Message: ${throwable.message}")
            writer.println("==================================")
            writer.println("Stack Trace:")
            writer.println(stackTrace)
            writer.println("==================================")
            writer.println("Cause:")

            var cause = throwable.cause
            var causeIndex = 1
            while (cause != null) {
                writer.println("  Cause #$causeIndex: ${cause.javaClass.name}: ${cause.message}")
                cause = cause.cause
                causeIndex++
            }

            writer.close()

            Log.i(TAG, "Crash log saved: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }
    }

    private fun showErrorDialog(throwable: Throwable, stackTrace: String) {
        // Создаем Handler для главного потока
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            try {
                // Формируем понятное сообщение об ошибке
                val errorMessage = when (throwable) {
                    is java.net.ConnectException ->
                        "Не удалось подключиться к серверу\nПроверьте интернет и адрес сервера"
                    is java.net.SocketTimeoutException ->
                        "Превышено время ожидания ответа от сервера"
                    is java.net.UnknownHostException ->
                        "Сервер не найден\nПроверьте IP-адрес в настройках"
                    is retrofit2.HttpException ->
                        "Ошибка сервера: ${throwable.code()}"
                    is com.google.gson.JsonSyntaxException ->
                        "Ошибка формата данных от сервера"
                    is java.lang.NullPointerException ->
                        "Отсутствуют необходимые данные:\n${throwable.message}"
                    else ->
                        throwable.message ?: "Неизвестная ошибка"
                }

                // Показываем диалог с ошибкой
                val dialog = AlertDialog.Builder(context)
                    .setTitle("❌ Приложение упало")
                    .setMessage("""
                        Что случилось:
                        $errorMessage
                        
                        Stack trace скопирован в буфер обмена.
                        
                        Хотите сохранить отчет об ошибке?
                    """.trimIndent())
                    .setPositiveButton("💾 Сохранить") { _, _ ->
                        // Копируем в буфер обмена
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as ClipboardManager
                        val clip = ClipData.newPlainText("Crash Report", stackTrace)
                        clipboard.setPrimaryClip(clip)

                        // Показываем где сохранился файл
                        val crashDir = android.content.ContextWrapper(context)
                            .getDir("crashes", Context.MODE_PRIVATE)
                        Toast.makeText(
                            context,
                            "Отчет сохранен в: ${crashDir.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .setNegativeButton("❌ Закрыть", null)
                    .setNeutralButton("📋 Копировать") { _, _ ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as ClipboardManager
                        val clip = ClipData.newPlainText("Error Message", errorMessage)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            "Сообщение скопировано",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setCancelable(false)
                    .create()

                // Показываем диалог
                dialog.show()

                // Логируем что показали диалог
                Log.i(TAG, "Error dialog shown")

            } catch (e: Exception) {
                // Если диалог не показался, хотя бы Toast
                Log.e(TAG, "Failed to show error dialog", e)
                Toast.makeText(
                    context,
                    "Критическая ошибка: ${throwable.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}