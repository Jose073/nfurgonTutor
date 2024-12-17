package com.example.nfurgontutor.Remote


import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import io.reactivex.rxjava3.core.Observable

interface OSRMApi {
    @GET("route/v1/driving/{from};{to}")
    fun getRoute(
        @Path("from") from: String,
        @Path("to") to: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline"
    ): Observable<ResponseBody>
}