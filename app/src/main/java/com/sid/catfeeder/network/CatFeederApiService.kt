package com.sid.catfeeder.network


import com.sid.catfeeder.models.Flavor
import com.sid.catfeeder.models.FoodType
import com.sid.catfeeder.models.Manufacturer
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface CatFeederApiService {

    // Health check
    @GET("health")
    suspend fun checkHealth(): Response<ApiResponse<HealthResponse>>

    // Питомцы
    @GET("pets")
    suspend fun getPets(): Response<ApiResponse<List<Pet>>>

    @GET("pets/{id}")
    suspend fun getPetById(@Path("id") petId: Int): Response<ApiResponse<Pet>>

    @POST("pets/weight")
    suspend fun addWeight(@Body weightLog: WeightLog): Response<ApiResponse<IdResponse>>

    @GET("pets/{id}/weight-history")
    suspend fun getWeightHistory(@Path("id") petId: Int): Response<ApiResponse<List<WeightHistory>>>

    // Корма
    @GET("foods/barcode/{barcode}")
    suspend fun getFoodByBarcode(@Path("barcode") barcode: String): Response<ApiResponse<Food>>

    @POST("foods")
    suspend fun createFood(@Body food: FoodCreate): Response<ApiResponse<IdResponse>>
    //suspend fun createFood(@Body food: FoodCreate): ApiResponse<Map<String, Int>>

    @GET("foods")
    suspend fun getAllFoods(): Response<ApiResponse<List<Food>>>

    // Кормления - ВАЖНО: ИСТОРИЯ КОРМЛЕНИЙ
    @POST("feeding")
    suspend fun logFeeding(@Body feeding: FeedingLog): Response<ApiResponse<IdResponse>>

    @POST("feeding")
    fun logFeedingCall(@Body feeding: FeedingLog): Call<ApiResponse<IdResponse>>

    @GET("feeding/history/{petId}")
    suspend fun getFeedingHistory(
        @Path("petId") petId: Int,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<List<FeedingHistoryItem>>>

    @GET("feeding/history")
    suspend fun getAllFeedingHistory(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<List<FeedingHistoryItem>>>

    @GET("feeding/{id}")
    suspend fun getFeedingById(@Path("id") feedingId: Int): Response<ApiResponse<FeedingHistoryItem>>

    @PUT("feeding/{id}/was-finished")
    suspend fun updateWasFinished(
        @Path("id") id: Int,
        @Body request: WasFinishedRequest
    ): Response<ApiResponse<Unit>>

    // Статистика
    @GET("pets/{id}/stats")
    suspend fun getPetStats(
        @Path("id") petId: Int,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ApiResponse<PetStats>>

    @GET("manufacturers")
    suspend fun getManufacturers(): Response<ApiResponse<List<Any>>>

    @GET("food-types")
    suspend fun getFoodTypes(): Response<ApiResponse<List<Any>>>

    @GET("flavors")
    suspend fun getFlavors(): Response<ApiResponse<List<Any>>>

    @POST("manufacturers")
    suspend fun createManufacturer(@Body manufacturer: Map<String, String>): Response<ApiResponse<IdResponse>>

    @POST("flavors")
    suspend fun createFlavor(@Body flavor: Map<String, String>): Response<ApiResponse<IdResponse>>
}

// Модели данных
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null,
    val timestamp: String? = null
)

data class HealthResponse(
    val status: String,
    val timestamp: String,
    val server: String,
    val php_version: String? = null
)

data class Pet(
    val id: Int,
    val name: String,
    val breed: String,
    val gender: String,
    val birthDate: String? = null,
    val currentWeight: Double? = null,
    val targetWeight: Double? = null,
    val profilePhoto: String? = null,
    val isActive: Boolean = true
)

data class Food(
    val id: Int,
    val barcode: String,
    val name: String,
    val manufacturer: String,
    val type: String? = null,
    val flavor: String? = null,
    val weight: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val calories: Double? = null,
    val photo: String? = null
)

data class FoodCreate(
    val barcode: String,
    val name: String,
    val manufacturerId: Int = 1,
    val foodTypeId: Int = 1,
    val flavorId: Int? = null,
    val weight: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val calories: Double? = null
)

data class FeedingLog(
    val petId: Int,
    val amount: Double,
    val barcode: String? = null,
    val foodId: Int? = null,
    val foodName: String? = null,
    val feedingDate: String? = null,
    val feedingTime: String? = null,
    val calories: Double? = null,
    val wasFinished: Int? = 100,
    val notes: String? = null
)

data class IdResponse(
    val id: Int
)

data class WeightLog(
    val petId: Int,
    val weight: Double,
    val date: String? = null,
    val notes: String? = null
)

data class WeightHistory(
    val id: Int,
    val petId: Int,
    val weight: Double,
    val measurementDate: String,
    val notes: String? = null,
    val createdAt: String
)

data class FeedingHistoryItem(
    val id: Int,
    val date: String,
    val time: String? = null,
    val foodName: String,
    val barcode: String? = null,
    val manufacturer: String? = null,
    val type: String? = null,
    val flavor: String? = null,
    val amount: Double,
    val calories: Double? = null,
    val wasFinished: Int? = 100,
    val notes: String? = null,
    val petId: Int? = null,
    val petName: String? = null
)
data class WasFinishedRequest(
    val wasFinished: Int
)
data class PetStats(
    val feedingCount: Int,
    val totalFood: Double,
    val avgAmount: Double,
    val firstDate: String? = null,
    val lastDate: String? = null
)