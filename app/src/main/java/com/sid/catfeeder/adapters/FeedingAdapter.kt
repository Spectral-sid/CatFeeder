package com.sid.catfeeder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sid.catfeeder.R
import com.sid.catfeeder.network.FeedingHistoryItem

class FeedingAdapter(
    private var feedings: List<FeedingHistoryItem>,
    private val onItemClick: (FeedingHistoryItem) -> Unit  // Добавляем колбэк для клика
) : RecyclerView.Adapter<FeedingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.card_view)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvFoodName: TextView = view.findViewById(R.id.tv_food_name)
        val tvAmount: TextView = view.findViewById(R.id.tv_amount)
        val tvPetName: TextView = view.findViewById(R.id.tv_pet_name)
        val tvWasFinished: TextView = view.findViewById(R.id.tv_was_finished) // Новый TextView для статуса
        val tvNotes: TextView = view.findViewById(R.id.tv_notes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feeding_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feeding = feedings[position]

        // Форматируем дату и время
        val dateTimeStr = if (!feeding.time.isNullOrEmpty()) {
            "${feeding.date} ${feeding.time.substring(0, 5)}"
        } else {
            feeding.date
        }

        holder.tvDate.text = dateTimeStr
        holder.tvFoodName.text = feeding.foodName
        holder.tvAmount.text = "${String.format("%.1f", feeding.amount)} г"

        // Показываем имя питомца
        holder.tvPetName.text = feeding.petName ?: "Питомец"
        holder.tvPetName.visibility = View.VISIBLE

        // НОВЫЙ КОД: Отображаем статус съеденного
        val wasFinished = feeding.wasFinished ?: 100
        val statusText = when {
            wasFinished >= 100 -> "✓ Всё съедено"
            wasFinished >= 75 -> "⚡ Большая часть"
            wasFinished >= 50 -> "◔ Половина"
            wasFinished >= 25 -> "◷ Треть"
            wasFinished > 0 -> "◐ Почти ничего"
            else -> "✗ Не тронуто"
        }
        holder.tvWasFinished.text = statusText
        holder.tvWasFinished.visibility = View.VISIBLE

        // Устанавливаем цвет в зависимости от статуса
        val context = holder.itemView.context
        val color = when {
            wasFinished >= 100 -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
            wasFinished >= 50 -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
        }
        holder.tvWasFinished.setTextColor(color)

        // Показываем заметки если есть
        if (!feeding.notes.isNullOrEmpty()) {
            holder.tvNotes.text = feeding.notes
            holder.tvNotes.visibility = View.VISIBLE
        } else {
            holder.tvNotes.visibility = View.GONE
        }

        // Добавляем обработчик клика на всю карточку
        holder.itemView.setOnClickListener {
            onItemClick(feeding)
        }
    }

    override fun getItemCount() = feedings.size

    fun updateData(newFeedings: List<FeedingHistoryItem>) {
        feedings = newFeedings
        notifyDataSetChanged()
    }
}