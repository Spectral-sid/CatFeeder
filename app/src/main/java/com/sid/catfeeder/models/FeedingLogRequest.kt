package com.sid.catfeeder.models

import com.google.gson.annotations.SerializedName

data class FeedingLogRequest(
    @SerializedName("pet_id") val petId: Int,
    @SerializedName("food_id") val foodId: Int,
    @SerializedName("amount_grams") val amountGrams: Double,
    @SerializedName("feeding_date") val feedingDate: String,
    @SerializedName("feeding_time") val feedingTime: String
)