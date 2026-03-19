package com.sid.catfeeder.models

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

data class FeedingHistory(
    @SerializedName("id") val id: Int,
    @SerializedName("pet_name") val petName: String,
    @SerializedName("food_name") val foodName: String,
    @SerializedName("amount_grams") val amountGrams: Double,
    @SerializedName("feeding_date") val feedingDate: String,
    @SerializedName("feeding_time") val feedingTime: String,
    @SerializedName("created_at") val createdAt: String
) {
    fun getFormattedDateTime(): String {
        return "$feedingDate $feedingTime"
    }
}