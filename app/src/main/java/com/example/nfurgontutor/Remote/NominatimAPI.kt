package com.example.nfurgontutor.Remote

import com.example.nfurgontutor.Model.NominatimLocation
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimAPI {
    @GET("search")
    fun searchLocation(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 1
    ): Call<List<NominatimLocation>>
}