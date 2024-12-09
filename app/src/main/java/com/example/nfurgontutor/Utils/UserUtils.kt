package com.example.nfurgontutor.Utils
import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import com.example.nfurgontutor.Common.Common
import com.example.nfurgontutor.Model.DriverGeoModel
import com.example.nfurgontutor.Model.FCMSendData
import com.example.nfurgontutor.Model.TokenModel
import com.example.nfurgontutor.R
import com.example.nfurgontutor.Remote.IFCMService
import com.example.nfurgontutor.Remote.RetrofitFCMClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

object UserUtils {
    fun updateUser(
        view: View?,
        updateData:Map<String,Any>
    ){
        FirebaseDatabase.getInstance()
            .getReference(Common.TUTO_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener { e->
                Snackbar.make(view!!,e.message!!, Snackbar.LENGTH_LONG).show()
            }.addOnSuccessListener {
                Snackbar.make(view!!,"Informacion Actualizada Correctamente", Snackbar.LENGTH_LONG).show()
            }
    }

    fun updateToken(context: Context, token: String){
        val tokenModel = TokenModel()
        tokenModel.token = token;

        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(tokenModel)
            .addOnFailureListener { e-> Toast.makeText(context,e.message,Toast.LENGTH_LONG).show() }
            .addOnSuccessListener {  }
    }

    fun sendRequestToDriver(
        Context: Context,
        mainlayout: RelativeLayout?,
        foundDriver: DriverGeoModel?,
        target: LatLng
    ) {
        val compositeDisponsable = CompositeDisposable()
        val ifcmService = RetrofitFCMClient.instance!!.create(IFCMService::class.java)

        //get token
        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REFERENCE)
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(datasnapshot: DataSnapshot) {
                    if (datasnapshot.exists())
                    {
                        val tokenModel = datasnapshot.getValue(TokenModel::class.java)

                        val notificationData:MutableMap<String,String> = HashMap()
                        notificationData.put(Common.NOTI_TITLE,Common.REQUEST_DRIVER_TITLE)
                        notificationData.put(Common.NOTI_BODY,"Este mensaje representa una solicitud del conductor")
                        notificationData.put(Common.PICKUP_LOCATION,StringBuilder()
                            .append(target.latitude)
                            .append(",")
                            .append(target.longitude)
                            .toString())

                        val fcmSendData = FCMSendData(tokenModel!!.token,notificationData)


                        compositeDisponsable.add(ifcmService.sendNotification(fcmSendData)!!
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                       if (fcmResponse.success==0)
                                       {
                                           Snackbar.make(mainlayout!!,Context.getString(R.string.send_request_driver_failed)
                                               ,Snackbar.LENGTH_LONG).show()
                                       }

                            },{t:Throwable? ->

                                compositeDisponsable.clear()
                                Snackbar.make(mainlayout!!,t!!.message!!,Snackbar.LENGTH_LONG).show()
                            }))

                    }
                    else
                    {
                        Snackbar.make(mainlayout!!,Context.getString(R.string.token_not_found)
                            ,Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(dataBaseError: DatabaseError) {
                    Snackbar.make(mainlayout!!,dataBaseError.message
                        ,Snackbar.LENGTH_LONG).show()
                }

            })

    }
}