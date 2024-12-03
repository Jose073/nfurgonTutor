package com.example.nfurgontutor.Model

import android.os.Handler
import com.google.android.gms.maps.model.LatLng

class AnimationModel (var isRun:Boolean,var geoQueryModel: GeoQueryModel){

    //moving marker
    var polylineList:List<LatLng>? = null
    var handler: Handler? = null
    var index:Int=0
    var next:Int=0
    var v:Float=0.0f
    var lat:Double=0.0
    var lng:Double=0.0
    var start: LatLng?=null
    var end: LatLng?=null

    init {
        handler = Handler()
    }
}