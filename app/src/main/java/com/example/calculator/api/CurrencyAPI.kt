package com.example.calculator.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApi {
    @GET("latest")
    fun getLatestRates(@Query("access_key") apiKey: String): Call<CurrencyResponse>
}
