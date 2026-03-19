package com.sid.catfeeder.models

import com.google.gson.annotations.SerializedName

data class Food(
    @SerializedName("id") val id: Int,
    @SerializedName("barcode") val barcode: String,
    @SerializedName("name") val name: String,
    @SerializedName("weight_grams") val weightGrams: Double?,
    @SerializedName("manufacturer_id") val manufacturerId: Int,
    @SerializedName("food_type_id") val foodTypeId: Int,
    @SerializedName("photo_path") val photoPath: String?
)