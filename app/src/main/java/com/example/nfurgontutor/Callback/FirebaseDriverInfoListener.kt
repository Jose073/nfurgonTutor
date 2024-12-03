package com.example.nfurgontutor.Callback

import com.example.nfurgontutor.Model.DriverGeoModel

interface FirebaseDriverInfoListener {
    fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel?)
}