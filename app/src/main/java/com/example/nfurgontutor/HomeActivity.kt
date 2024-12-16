package com.example.nfurgontutor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.example.nfurgontutor.Common.Common
import com.example.nfurgontutor.Utils.UserUtils
import com.example.nfurgontutor.databinding.ActivityHomeBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHomeBinding

    private lateinit var navView:NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var img_avatar: ImageView
    private lateinit var waitingDialog: AlertDialog
    private lateinit var storageReference: StorageReference
    private var imageUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarHome.toolbar)

         drawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout
         navView = findViewById(R.id.nav_view) as NavigationView
         navController = findNavController(R.id.nav_host_fragment_content_home)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        initializeComponents()
    }

    private fun initializeComponents() {

        storageReference = FirebaseStorage.getInstance().getReference()

        waitingDialog = AlertDialog.Builder(this)
            .setMessage("waiting...")
            .setCancelable(false).create()

        navView.setNavigationItemSelectedListener { it ->
            if (it.itemId == R.id.nav_sign_out) {
                val builder = AlertDialog.Builder(this@HomeActivity)

                builder.setTitle("salir")
                    .setMessage("¿Estas seguro de que quieres salir?")
                    .setNegativeButton("CANCELAR",{ dialogInterface, _ -> dialogInterface.dismiss()})

                    .setPositiveButton("SALIR"){ dialogInterface, _ ->


                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this@HomeActivity,SplashScreen::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()

                    }.setCancelable(false)

                val dialog = builder.create()
                dialog.setOnShowListener{
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(ContextCompat.getColor(this@HomeActivity,android.R.color.darker_gray))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(ContextCompat.getColor(this@HomeActivity,R.color.orange))
                }
                dialog.show()
            }
            true
        }

        val headerView = navView.getHeaderView(0)
        val txt_name = headerView.findViewById<View>(R.id.txt_name) as TextView
        val txt_phone = headerView.findViewById<View>(R.id.txt_phone) as TextView
        img_avatar = headerView.findViewById<View>(R.id.img_avatar) as ImageView
        txt_name.setText(Common.buildWelcomeMessage())
        txt_phone.setText(Common.currentTutor?.phoneNumber)

        if (Common.currentTutor != null && Common.currentTutor!!.avatar != null && !TextUtils.isEmpty(
                Common.currentTutor!!.avatar))
        {
            Glide.with(this)
                .load(Common.currentTutor!!.avatar)
                .into(img_avatar)
        }

        img_avatar.setOnClickListener {
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent,"Selecionar Foto"),PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            if (data != null && data.data != null)
            {
                imageUri = data.data
                img_avatar.setImageURI(imageUri)

                //Log.d("HomeActivity", "imageUri: $imageUri")

                showDialogUpload()
            }
        }
    }

    private fun showDialogUpload() {
        val builder = AlertDialog.Builder(this@HomeActivity)

        builder.setTitle("Cambiar Avatar")
            .setMessage("¿De verdad quieres cambiar el avatar?")
            .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }

            .setPositiveButton("CAMBIAR"){ dialogInterface, _ ->

                if (imageUri != null)
                {
                    waitingDialog.show()
                    val avatarFolder = storageReference.child("avatars/"+FirebaseAuth.getInstance().currentUser!!.uid)

                    avatarFolder.putFile(imageUri!!)
                        .addOnFailureListener{ e: Exception ->
                            Snackbar.make(drawerLayout,e.message?: "Error",Snackbar.LENGTH_LONG).show()
                            waitingDialog.dismiss()
                        }.addOnCompleteListener{ task: Task<UploadTask.TaskSnapshot> ->
                            if (task.isSuccessful)
                            {
                                avatarFolder.downloadUrl.addOnSuccessListener{ uri: Uri->
                                    val updateData = HashMap<String, Any>()
                                    updateData["avatar"] = uri.toString()

                                    UserUtils.updateUser(drawerLayout,updateData)
                                }
                            }
                            waitingDialog.dismiss()
                        }.addOnProgressListener{ taskSnapshot: UploadTask.TaskSnapshot  ->
                            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                            waitingDialog.setMessage(StringBuilder("Uploading: ").append(progress).append("%"))
                        }
                }
            }.setCancelable(false)

        val dialog = builder.create()
        dialog.setOnShowListener{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this@HomeActivity,android.R.color.darker_gray))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this@HomeActivity,R.color.orange))
        }
        dialog.show()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object{
        val PICK_IMAGE_REQUEST = 7272
    }
}