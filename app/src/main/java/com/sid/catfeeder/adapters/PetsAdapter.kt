package com.sid.catfeeder.adapters

//package com.example.catfeeder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.sid.catfeeder.R
import com.sid.catfeeder.models.Pet

class PetsAdapter(private val pets: List<Pet>) :
    RecyclerView.Adapter<PetsAdapter.PetViewHolder>() {

    class PetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val petName: TextView = view.findViewById(R.id.tv_pet_name)
        val petWeight: TextView = view.findViewById(R.id.tv_pet_weight)
        val petImage: ImageView = view.findViewById(R.id.iv_pet_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = pets[position]

        holder.petName.text = pet.name
        holder.petWeight.text = "Вес: ${pet.currentWeight ?: "?"} кг"

        // Загружаем изображение если есть
        if (!pet.photoPath.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(pet.photoPath)
                .transform(CircleCrop())
                .placeholder(R.drawable.ic_cat_default)
                .into(holder.petImage)
        } else {
            holder.petImage.setImageResource(R.drawable.ic_cat_default)
        }
    }

    override fun getItemCount() = pets.size
}