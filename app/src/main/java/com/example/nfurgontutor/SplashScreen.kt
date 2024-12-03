package com.example.nfurgontutor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.nfurgontutor.Common.Common
import com.example.nfurgontutor.Model.TutoModel
import com.example.nfurgontutor.ui.theme.NfurgonTutorTheme
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import kotlinx.coroutines.delay
import java.util.Arrays
import java.util.concurrent.TimeUnit
import android.app.AlertDialog
import android.text.TextUtils
import android.widget.ProgressBar
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.nfurgontutor.Utils.UserUtils
import com.example.nfurgontutor.ui.home.HomeFragment
import com.google.firebase.messaging.FirebaseMessaging


class SplashScreen : ComponentActivity() {

    companion object{
        private val LOGIN_REQUEST_CODE = 7171
    }

    lateinit var provider:List<AuthUI.IdpConfig>
    lateinit var firebaseAuth:FirebaseAuth
    lateinit var listener:FirebaseAuth.AuthStateListener

    private lateinit var database:FirebaseDatabase
    private lateinit var tutoInforef:DatabaseReference

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(listener)
    }

    private fun delaySplashScreen() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            firebaseAuth.addAuthStateListener(listener)
        }, 3000) //Espera de 3 segundos
    }

    override fun onStop() {
        if (::firebaseAuth.isInitialized && ::listener.isInitialized) {
            firebaseAuth.removeAuthStateListener(listener)
        }
        super.onStop()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        init()
        //agregado recien
        delaySplashScreen()
    }

    private fun init() {

        database = FirebaseDatabase.getInstance()
        tutoInforef = database.getReference(Common.TUTO_INFO_REFERENCE)
        provider = listOf(//Arrays.asList
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user = myFirebaseAuth.currentUser
            if(user != null)
            {
                FirebaseMessaging.getInstance()
                    .token
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this@SplashScreen,
                            e.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnSuccessListener { token ->
                        Log.d("TOKEN", token)
                        UserUtils.updateToken(this@SplashScreen, token)
                    }
                checkUserFromFirebase()
            }
            else
            {
                showLoginLayout()
            }
        }
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_singin)
            .setGoogleButtonId(R.id.btn_google_singin)
            .build()

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(provider)
                .setIsSmartLockEnabled(false)
                .build(), LOGIN_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == LOGIN_REQUEST_CODE)
        {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK)
            {
                //val user = FirebaseAuth.getInstance().currentUser
                FirebaseAuth.getInstance().currentUser!!.uid
            }
            else
            {
                //AGREGADO RECIEN
                Toast.makeText(this@SplashScreen,response!!.error!!.message,Toast.LENGTH_LONG).show()
            }

        }
    }


        private fun checkUserFromFirebase() {
        tutoInforef.child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    if(p0.exists())
                    {
                        val model = p0.getValue(TutoModel::class.java)
                        goToHomeActivity(model)
                    }
                    else
                    {
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@SplashScreen,p0.message,Toast.LENGTH_LONG).show()
                }

            })
    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)

        val edtFirstName = itemView.findViewById<TextInputEditText>(R.id.edt_first_name)
        val edtLastName = itemView.findViewById<TextInputEditText>(R.id.edt_last_name)
        val edtPhoneNumber = itemView.findViewById<TextInputEditText>(R.id.edt_phone_number)
        val btnContinue = itemView.findViewById<Button>(R.id.btn_register)

        //set data
        if(FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
                !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
            edtPhoneNumber.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)
            //edt_phone_number
        //view
        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        //event
        btnContinue.setOnClickListener {
            when {
                edtFirstName.text.isNullOrBlank() -> {
                    Toast.makeText(this, "Ingrese el nombre", Toast.LENGTH_SHORT).show()
                }
                edtLastName.text.isNullOrBlank() -> {
                    Toast.makeText(this, "Ingrese el apellido", Toast.LENGTH_SHORT).show()
                }
                edtPhoneNumber.text.isNullOrBlank() -> {
                    Toast.makeText(this, "Ingrese el numero telefono", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val model = TutoModel()
                        model.firstName = edtFirstName.text.toString()
                        model.lastName = edtLastName.text.toString()
                        model.phoneNumber = edtPhoneNumber.text.toString()


                tutoInforef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model).addOnFailureListener {e->
                        Toast.makeText(this@SplashScreen,""+e.message,Toast.LENGTH_SHORT).show()
                        dialog.dismiss()

                    }.addOnSuccessListener {
                        Toast.makeText(this@SplashScreen,"Se ha registrado correctamente",Toast.LENGTH_SHORT).show()
                        dialog.dismiss()

                        goToHomeActivity(model)

                    }

            }
        }
    }
    }
    private fun goToHomeActivity(model: TutoModel?) {
        Common.currentTutor = model
        startActivity(Intent(this,HomeActivity::class.java))
        finish()
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NfurgonTutorTheme {
        Greeting("Android")
    }
}
}