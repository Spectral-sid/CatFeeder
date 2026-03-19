package com.sid.catfeeder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sid.catfeeder.R
import com.sid.catfeeder.network.FeedingHistoryItem

class FeedingAdapter(private var feedings: List<FeedingHistoryItem>) :
    RecyclerView.Adapter<FeedingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvFoodName: TextView = view.findViewById(R.id.tv_food_name)
        val tvAmount: TextView = view.findViewById(R.id.tv_amount)
        val tvPetName: TextView = view.findViewById(R.id.tv_pet_name)
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

        // Показываем заметки если есть
        if (!feeding.notes.isNullOrEmpty()) {
            holder.tvNotes.text = feeding.notes
            holder.tvNotes.visibility = View.VISIBLE
        } else {
            holder.tvNotes.visibility = View.GONE
        }
    }

    override fun getItemCount() = feedings.size

    fun updateData(newFeedings: List<FeedingHistoryItem>) {
        feedings = newFeedings
        notifyDataSetChanged()
    }
}