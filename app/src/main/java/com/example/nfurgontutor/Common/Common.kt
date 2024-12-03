package com.example.nfurgontutor.Common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.nfurgontutor.Model.AnimationModel
import com.example.nfurgontutor.Model.DriverGeoModel
import com.example.nfurgontutor.Model.TutoModel
import com.example.nfurgontutor.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.atan


object Common {
    val driversSubscribe: MutableMap<String, AnimationModel> = HashMap<String,AnimationModel>()
    val markerList: MutableMap<String,Marker> = HashMap<String,Marker>()
    val DRIVER_INFO_REFERECE: String = "DriverINFO"
    val driverFound: MutableSet<DriverGeoModel> = HashSet<DriverGeoModel>()
    val DRIVER_LOCATION_REFERENCE: String = "DriversLOCATION"
    val TOKEN_REFERENCE: String = "Token"
    var currentTutor: TutoModel?=null
    val TUTO_INFO_REFERENCE: String="Tutores"
    val NOTI_BODY: String = "body"
    val NOTI_TITLE: String = "title"


    fun showNotification(context: Context, id: Int, title: String?, body: String?, intent: Intent?){
        var pendingIntent : PendingIntent? = null
        if (intent != null)
            pendingIntent = PendingIntent.getActivity(context,id,intent!!, PendingIntent.FLAG_UPDATE_CURRENT)
        val NOTIFICATION_CHANNEL_ID = "Nfurgon"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,"Nfurgon",
                NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = "Nfurgon"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000)
            notificationChannel.enableVibration(true)

            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
        builder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.baseline_directions_car_24)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.drawable.baseline_directions_car_24))
        if (pendingIntent != null)
            builder.setContentIntent(pendingIntent!!)
        val notification = builder.build()
        notificationManager.notify(id,notification)
    }

    fun buildWelcomeMessage(): String{
        return StringBuilder("Bienvenido,")
            .append(currentTutor!!.firstName)
            .append(" ")
            .append(currentTutor!!.lastName)
            .toString()
    }

    fun buildName(firstName: String?, lastName: String?): String? {
        return java.lang.StringBuilder(firstName!!).append(lastName).toString()
    }

    //DECODE POLY
    fun decodePoly(encoded: String): List<LatLng> {
        val poly: MutableList<LatLng> = ArrayList()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
            lng += dlng

            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }

    //GET BEARING
    fun getBearing(begin: LatLng, end: LatLng): Float {
        val lat = abs(begin.latitude - end.latitude)
        val lng = abs(begin.longitude - end.longitude)

        if (begin.latitude < end.latitude && begin.longitude < end.longitude) return Math.toDegrees(
            atan(lng / lat)
        )
            .toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude) return ((90 - Math.toDegrees(
            atan(lng / lat)
        )) + 90).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude) return (Math.toDegrees(
            atan(lng / lat)
        ) + 180).toFloat()
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude) return ((90 - Math.toDegrees(
            atan(lng / lat)
        )) + 270).toFloat()
        return (-1).toFloat()
    }

    fun setWelcomeMessage(txtWelcome: TextView) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour >= 1 && hour <=12)
            txtWelcome.setText(java.lang.StringBuilder("Buenos dias!"))
        else
            txtWelcome.setText(java.lang.StringBuilder("Buenos tardes!"))
    }

    fun formatDuration(duration: String): CharSequence? {
        if (duration.contains("mins"))
            return  duration.substring(0,duration.length-1)
        else
            return  duration
    }

    fun formatAddress(startAddress: String): CharSequence? {
        val firstIndexComma = startAddress.indexOf(",")
        return  startAddress.substring(0,firstIndexComma)
    }
}
