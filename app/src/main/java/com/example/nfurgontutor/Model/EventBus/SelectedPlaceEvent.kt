package com.example.nfurgontutor.Model.EventBus

import com.google.android.gms.maps.model.LatLng
import java.lang.StringBuilder

class SelectedPlaceEvent(var origin:LatLng,var destination:LatLng) {
    val originString:String
        get() = StringBuilder()
            .append(origin.latitude)
            .append(",")
            .append(origin.longitude)
            .toString()

    val destinationString:String
        get() = StringBuilder()
        .append(destination.latitude)
        .append(",")
        .append(destination.longitude)
        .toString()

    companion object {
        // Coordenadas de un lugar fijo (ejemplo: Colegio Santiago)
        val SCHOOL_LOCATION = LatLng(-33.4489, -70.6693)

        fun createFromSchoolToDestination(destination: LatLng): SelectedPlaceEvent {
            return SelectedPlaceEvent(SCHOOL_LOCATION, destination)
        }
    }
}