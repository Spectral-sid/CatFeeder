package com.sid.catfeeder.models

import com.google.gson.annotations.SerializedName

data class Pet(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("current_weight") val currentWeight: Double?,
    @SerializedName("target_weight") val targetWeight: Double?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("profile_photo_path") val photoPath: String?
)