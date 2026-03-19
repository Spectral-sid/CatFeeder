package com.sid.catfeeder



import android.app.Application
import android.content.Context
import android.util.Log
import java.io.File

class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication
            private set

        // Путь к папке с crash-логами
        fun getCrashLogsDir(context: Context): File {
            return android.content.ContextWrapper(context)
                .getDir("crashes", Context.MODE_PRIVATE)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Устанавливаем глобальный обработчик исключений
        setupUncaughtExceptionHandler()

        // Логируем запуск приложения
        Log.i("MyApplication", "✅ Приложение запущено")
        Log.i("MyApplication", "📁 Crash logs directory: ${getCrashLogsDir(this).absolutePath}")
    }

    private fun setupUncaughtExceptionHandler() {
        // Получаем текущий обработчик
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        // Устанавливаем наш обработчик
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("MyApplication", "==========================================")
                Log.e("MyApplication", "🚨 ГЛОБАЛЬНЫЙ ОБРАБОТЧИК ПЕРЕХВАТИЛ ИСКЛЮЧЕНИЕ")
                Log.e("MyApplication", "==========================================")

                // Создаем и запускаем наш обработчик
                val crashHandler = MyCrashHandler(this)
                crashHandler.uncaughtException(thread, throwable)

            } catch (e: Exception) {
                // Если наш обработчик упал, вызываем стандартный
                Log.e("MyApplication", "Ошибка в crash handler", e)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        Log.i("MyApplication", "✅ Глобальный обработчик исключений установлен")
    }
}