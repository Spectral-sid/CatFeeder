package com.sid.catfeeder

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.RadioGroup
import com.sid.catfeeder.network.FeedingHistoryItem

class WasFinishedDialog(
    private val context: Context,
    private val feedingItem: FeedingHistoryItem,
    private val onSave: (Int) -> Unit
) {

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_was_finished, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroup)

        // Устанавливаем текущее значение
        val currentValue = feedingItem.wasFinished ?: 100
        when (currentValue) {
            100 -> radioGroup.check(R.id.rbAll)
            75 -> radioGroup.check(R.id.rbMost)
            50 -> radioGroup.check(R.id.rbHalf)
            33 -> radioGroup.check(R.id.rbThird)
            10 -> radioGroup.check(R.id.rbLittle)
            0 -> radioGroup.check(R.id.rbNone)
            else -> radioGroup.check(R.id.rbAll)
        }

        // Добавляем информацию о кормлении
        val tvInfo = dialogView.findViewById<TextView>(R.id.tvInfo)
        tvInfo.text = "${feedingItem.foodName}\n${String.format("%.0f", feedingItem.amount)} г"

        AlertDialog.Builder(context)
            .setTitle("Сколько съедено?")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val selectedId = radioGroup.checkedRadioButtonId
                val wasFinished = when (selectedId) {
                    R.id.rbAll -> 100
                    R.id.rbMost -> 75
                    R.id.rbHalf -> 50
                    R.id.rbThird -> 33
                    R.id.rbLittle -> 10
                    R.id.rbNone -> 0
                    else -> 100
                }
                onSave(wasFinished)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}