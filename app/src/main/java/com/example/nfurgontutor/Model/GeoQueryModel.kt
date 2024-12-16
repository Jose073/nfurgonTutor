package com.example.nfurgontutor.Model

import java.util.ArrayList

class GeoQueryModel {
    var g: String? = null
    var l: ArrayList<Double>? = null

    constructor()

    constructor(g: String?, l: ArrayList<Double>?) {
        this.g = g
        this.l = l
    }
}

