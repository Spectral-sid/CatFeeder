package com.sid.catfeeder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.sid.catfeeder.models.FeedingHistory
import com.sid.catfeeder.R
import com.sid.catfeeder.network.FeedingHistoryItem
import androidx.core.content.ContextCompat

class FeedingHistoryAdapter(private var items: List<FeedingHistoryItem>) :
    RecyclerView.Adapter<FeedingHistoryAdapter.HistoryViewHolder>() {

    private var onItemClickListener: ((FeedingHistoryItem) -> Unit)? = null

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val petName: TextView = itemView.findViewById(R.id.tv_history_pet_name)
        val foodName: TextView = itemView.findViewById(R.id.tv_history_food_name)
        val amount: TextView = itemView.findViewById(R.id.tv_history_amount)
        val dateTime: TextView = itemView.findViewById(R.id.tv_history_datetime)
        val notes: TextView = itemView.findViewById(R.id.tv_history_notes)
        val cardView: CardView = itemView.findViewById(R.id.card_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feeding_history_card, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = items[position]

        // Отображаем имя питомца если есть
        if (item.petName != null) {
            holder.petName.text = item.petName
            holder.petName.visibility = View.VISIBLE
        } else {
            holder.petName.visibility = View.GONE
        }

        // Форматируем дату и время
        val dateTimeStr = if (item.time != null && item.time.isNotEmpty()) {
            "${item.date} ${item.time.substring(0, 5)}"
        } else {
            item.date
        }
        holder.dateTime.text = dateTimeStr
        holder.foodName.text = item.foodName
        holder.amount.text = "${String.format("%.1f", item.amount)} г"

        // Отображаем заметки если есть
        if (!item.notes.isNullOrEmpty()) {
            holder.notes.text = item.notes
            holder.notes.visibility = View.VISIBLE
        } else {
            holder.notes.visibility = View.GONE
        }

        holder.cardView.setOnClickListener {
            onItemClickListener?.invoke(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<FeedingHistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (FeedingHistoryItem) -> Unit) {
        onItemClickListener = listener
    }
}