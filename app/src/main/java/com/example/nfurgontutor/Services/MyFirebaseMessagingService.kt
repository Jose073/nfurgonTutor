package com.example.nfurgontutor.Services


import com.example.nfurgontutor.Common.Common
import com.example.nfurgontutor.Model.EventBus.DeclineRequestFromDriver
import com.example.nfurgontutor.Model.EventBus.DriverAcceptTripEvent
import com.example.nfurgontutor.Utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (FirebaseAuth.getInstance().currentUser != null)
            UserUtils.updateToken(this,token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        if (data != null)
        {
            if (data[Common.NOTI_TITLE] != null) {
                if (data[Common.NOTI_TITLE].equals(Common.REQUEST_DRIVER_DECLINE))
                {
                    EventBus.getDefault().postSticky(DeclineRequestFromDriver())
                }else if (data[Common.NOTI_TITLE].equals(Common.REQUEST_DRIVER_ACCEPT))
                {
                    EventBus.getDefault().postSticky(DriverAcceptTripEvent(data[Common.TRIP_KEY]!!))
                }


                else
                    Common.showNotification(
                    this, Random.nextInt(),
                    data[Common.NOTI_TITLE],
                    data[Common.NOTI_BODY],
                    null
                )
            }
        }
    }


}