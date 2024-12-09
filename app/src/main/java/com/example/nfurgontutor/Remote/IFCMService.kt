package com.example.nfurgontutor.Remote

import com.example.nfurgontutor.Model.FCMResponse
import com.example.nfurgontutor.Model.FCMSendData
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IFCMService {

    @Headers(
        "Content-Type:application/json",
        "Authorization:key=YOUR-KEY"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: FCMSendData?):Observable<FCMResponse>?
}