package com.mechanicai.pro.data.remote.nhtsa

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * NHTSA vPIC API endpoints.
 */
interface NhtsaApiService {

    @GET("vehicles/DecodeVinValues/{vin}")
    suspend fun decodeVin(
        @Path("vin") vin: String,
        @Query("format") format: String = "json"
    ): NhtsaResponse<NhtsaVinResult>

    @GET("vehicles/GetMakesForVehicleType/car")
    suspend fun getMakesForVehicleType(
        @Query("format") format: String = "json",
        @Query("modelyear") modelYear: Int? = null
    ): NhtsaResponse<NhtsaMake>

    @GET("vehicles/GetModelsForMakeYear/make/{make}/modelyear/{year}")
    suspend fun getModelsForMakeYear(
        @Path("make") make: String,
        @Path("year") year: Int,
        @Query("format") format: String = "json"
    ): NhtsaResponse<NhtsaModel>
}

/**
 * Generic NHTSA API response wrapper.
 */
data class NhtsaResponse<T>(
    val Count: Int,
    val Message: String,
    val Results: List<T>
)

/**
 * VIN decode result item.
 */
data class NhtsaVinResult(
    val Make: String?,
    val Model: String?,
    val ModelYear: String?,
    val Trim: String?,
    val EngineModel: String?,
    val DisplacementL: String?,
    val Cylinders: String?,
    val FuelTypePrimary: String?,
    val BodyClass: String?,
    val VehicleType: String?,
    val ErrorCode: String?
)

/**
 * Make result item.
 */
data class NhtsaMake(
    val MakeId: Int,
    val MakeName: String
)

/**
 * Model result item.
 */
data class NhtsaModel(
    val Make_ID: Int,
    val Make_Name: String,
    val Model_ID: Int,
    val Model_Name: String
)
