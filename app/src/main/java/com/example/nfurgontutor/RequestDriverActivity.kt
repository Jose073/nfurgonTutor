package com.example.nfurgontutor

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.nfurgontutor.Common.Common
import com.example.nfurgontutor.Model.DriverGeoModel
import com.example.nfurgontutor.Model.EventBus.DeclineRequestFromDriver
import com.example.nfurgontutor.Model.EventBus.DriverAcceptTripEvent
import com.example.nfurgontutor.Model.EventBus.SelectedPlaceEvent
import com.example.nfurgontutor.Model.TripPlanModel
import com.example.nfurgontutor.Remote.IGoogleAPI
import com.example.nfurgontutor.Remote.RetrofitClient
import com.example.nfurgontutor.Utils.UserUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.nfurgontutor.databinding.ActivityRequestDriverBinding
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.SquareCap
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.ui.IconGenerator
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Runnable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.lang.StringBuilder

class RequestDriverActivity : AppCompatActivity(), OnMapReadyCallback {

    private var driverOldPosition: String = ""
    private var handler:Handler?=null
    private var v=0f
    private var lat=0.0
    private var lng=0.0
    private var index =0
    private var next =0
    private var start:LatLng?=null
    private var end:LatLng?=null

    //animacion giratoria
    var animator: ValueAnimator?=null
    private val DESIRED_NUM_OF_SPINS = 5
    private val DESIRE_SECONDS_PER_ONE_FULL_360_SPIN = 40

    //Efectos
    var lastUserCircle: Circle?=null
    val duration=1000
    var lastPulseAnimator: ValueAnimator?=null


    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityRequestDriverBinding



    private var selectedPlaceEvent: SelectedPlaceEvent? = null

    private lateinit var mapFragment: SupportMapFragment;

    //Rutas
    private val compositeDisposable = CompositeDisposable()
    private lateinit var iGoogleAPI: IGoogleAPI
    private var blackPolyline: Polyline? = null
    private var greyPolyline: Polyline? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolylineOptions :PolylineOptions?=null
    private var polylineList: ArrayList<LatLng>?=null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null

    private var lastDriverCall:DriverGeoModel?=null




    private lateinit var btnConfirm: Button
    private lateinit var fillMap: View
    private lateinit var findingYourRide: View
    private lateinit var mainLayout: RelativeLayout

    private lateinit var img_Driver:ImageView
    private lateinit var txtDriverName:TextView
    private lateinit var driverInfoLayout: View

    override fun onStart() {
        super.onStart()

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)


    }

    override fun onStop() {
        compositeDisposable.clear()
        if (EventBus.getDefault().hasSubscriberForEvent(SelectedPlaceEvent::class.java))
            EventBus.getDefault().removeStickyEvent(SelectedPlaceEvent::class.java)
        if (EventBus.getDefault().hasSubscriberForEvent(DeclineRequestFromDriver::class.java))
            EventBus.getDefault().removeStickyEvent(DeclineRequestFromDriver::class.java)
        if (EventBus.getDefault().hasSubscriberForEvent(DriverAcceptTripEvent::class.java))
            EventBus.getDefault().removeStickyEvent(DriverAcceptTripEvent::class.java)

        EventBus.getDefault().unregister(this)
        super.onStop()
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onDriveAcceptTripEvent(event:DriverAcceptTripEvent)
    {

        driverInfoLayout=findViewById(R.id.driver_info_layout)
        img_Driver = findViewById(R.id.img_driver)
        txtDriverName=findViewById(R.id.txt_driver_name)
        FirebaseDatabase.getInstance().getReference(Common.TRIP)
            .child(event.tripId)
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists())
                    {
                        val tripPlanModel = snapshot.getValue(TripPlanModel::class.java)
                        mMap.clear()
                        fillMap.visibility=View.GONE
                        if(animator != null) animator!!.end()
                        val cameraPos =CameraPosition.Builder().target(mMap.cameraPosition.target)
                            .tilt(0f).zoom(mMap.cameraPosition.zoom).build()
                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPos))

                        //get routes
                        val driverLocation = StringBuilder()
                            .append(tripPlanModel!!.currentLat)
                            .append(",")
                            .append(tripPlanModel!!.currentLat)
                            .toString()

                        compositeDisposable.add(
                            iGoogleAPI.getDirections("driving",
                                "less_driving",
                                tripPlanModel!!.origin,driverLocation,
                                getString(R.string.google_api_key))!!
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { returnResult ->

                                    var blackPolylineOptions:PolylineOptions?=null
                                    var polylineList:List<LatLng?>?=null
                                    var blackPolyline:Polyline?=null
                                    try {
                                        if (returnResult.isNotEmpty()) {
                                            val jsonObject = JSONObject(returnResult)
                                            val jsonArray = jsonObject.getJSONArray("routes")

                                            for (i in 0 until jsonArray.length()) {
                                                val route = jsonArray.getJSONObject(i)
                                                val poly = route.getJSONObject("overview_polyline")
                                                val polyline = poly.getString("points")
                                                polylineList = ArrayList(Common.decodePoly(polyline))
                                            }

                                            polylineList?.let {
                                                polylineOptions = PolylineOptions().apply {
                                                    color(android.graphics.Color.GRAY)
                                                    width(12f)
                                                    startCap(SquareCap())
                                                    jointType(JointType.ROUND)
                                                    addAll(it)
                                                }
                                                greyPolyline = mMap?.addPolyline(polylineOptions!!)

                                                blackPolylineOptions = PolylineOptions().apply {
                                                    color(android.graphics.Color.BLACK)
                                                    width(12f)
                                                    startCap(SquareCap())
                                                    jointType(JointType.ROUND)
                                                    addAll(it)
                                                }
                                                blackPolyline = mMap?.addPolyline(blackPolylineOptions!!)

                                                // ANIMATOR
                                                val valueAnimator = ValueAnimator.ofInt(0, 100).apply {
                                                    duration = 1100
                                                    repeatCount = ValueAnimator.INFINITE
                                                    interpolator = LinearInterpolator()
                                                    addUpdateListener { value ->
                                                        greyPolyline?.let { grey ->
                                                            val points = grey.points
                                                            val percentValue = value.animatedValue.toString().toInt()
                                                            val size = points.size
                                                            val newPoints = (size * (percentValue / 100.0f)).toInt()
                                                            blackPolyline?.points = points.subList(0, newPoints)
                                                        }
                                                    }
                                                }
                                                valueAnimator.start()
                                            }

                                            val latLngBound = LatLngBounds.Builder()
                                                .include(selectedPlaceEvent!!.origin)
                                                .include(selectedPlaceEvent!!.destination)
                                                .build()

                                            val objects = jsonArray.getJSONObject(0)
                                            val legs = objects.getJSONArray("legs")
                                            val legsObject = legs.getJSONObject(0)

                                            val time = legsObject.getJSONObject("duration")
                                            val duration = time.getString("text")


                                            val origin = LatLng(
                                                tripPlanModel!!.origin!!.split(",").get(0).toDouble(),
                                                tripPlanModel!!.origin!!.split(",").get(1).toDouble())
                                            val destination = LatLng(tripPlanModel.currentLat,tripPlanModel.currentLng)

                                            val latLngBounds = LatLngBounds.Builder()
                                                .include(origin)
                                                .include(destination)
                                                .build()

                                            addPickupMarkerWithDuration(duration,origin)
                                            addDriverMarker(destination)


                                            mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
                                            mMap?.moveCamera(CameraUpdateFactory.zoomTo(mMap?.cameraPosition?.zoom?.minus(1) ?: 0f))

                                            initDriverForMoving(event.tripId,tripPlanModel)

                                            //load driver avatar
                                            Glide.with(this@RequestDriverActivity)
                                                .load(tripPlanModel!!.driverInfoModel!!.avatar)
                                                .into(img_Driver)
                                            txtDriverName.setText(tripPlanModel.driverInfoModel!!.primer_nombre)

                                            btnConfirm.visibility=View.GONE
                                            driverInfoLayout.visibility=View.VISIBLE

                                        } else {
                                            Log.e("Error", "Respuesta de la API vacía")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Error", "Error procesando datos: ${e.message}")
                                        mapFragment?.view?.let {
                                            Snackbar.make(it, e.message ?: "Error desconocido", Snackbar.LENGTH_LONG).show()
                                        }}
                                }
                        )




                    }
                    else
                        Snackbar.make(mainLayout,getString(R.string.trip_not_found),Snackbar.LENGTH_LONG).show()

                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(mainLayout,error.message,Snackbar.LENGTH_LONG).show()
                }

            })
    }

    private fun initDriverForMoving(tripId: String, tripPlanModel: TripPlanModel) {
        driverOldPosition = StringBuilder().append(tripPlanModel.currentLat)
            .append(",").append(tripPlanModel.currentLat).toString()

        FirebaseDatabase.getInstance().getReference(Common.TRIP)
            .child(tripId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newData= snapshot.getValue(TripPlanModel::class.java)
                    val driverNewPosition = StringBuilder().append(newData?.currentLat)
                        .append(",").append(newData?.currentLat).toString()
                    if (!driverOldPosition.equals(driverNewPosition))
                        moveMarkerAnimation(destinationMarker!!,driverOldPosition,driverNewPosition)
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(mainLayout,error.message,Snackbar.LENGTH_LONG).show()
                }
            })
    }

    private fun moveMarkerAnimation(marker: Marker, from: String, to: String) {
        compositeDisposable.add(
            iGoogleAPI.getDirections("driving",
                "less_driving",
                from,to,
                getString(R.string.google_api_key))!!
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { returnResult ->

                    try {
                        if (returnResult.isNotEmpty()) {
                            val jsonObject = JSONObject(returnResult)
                            val jsonArray = jsonObject.getJSONArray("routes")

                            for (i in 0 until jsonArray.length()) {
                                val route = jsonArray.getJSONObject(i)
                                val poly = route.getJSONObject("overview_polyline")
                                val polyline = poly.getString("points")
                                polylineList = ArrayList(Common.decodePoly(polyline))
                            }

                            polylineList?.let {
                                polylineOptions = PolylineOptions().apply {
                                    color(android.graphics.Color.GRAY)
                                    width(12f)
                                    startCap(SquareCap())
                                    jointType(JointType.ROUND)
                                    addAll(it)
                                }
                                greyPolyline = mMap?.addPolyline(polylineOptions!!)

                                blackPolylineOptions = PolylineOptions().apply {
                                    color(android.graphics.Color.BLACK)
                                    width(12f)
                                    startCap(SquareCap())
                                    jointType(JointType.ROUND)
                                    addAll(it)
                                }
                                blackPolyline = mMap?.addPolyline(blackPolylineOptions!!)

                                // ANIMATOR
                                val valueAnimator = ValueAnimator.ofInt(0, 100).apply {
                                    duration = 1100
                                    repeatCount = ValueAnimator.INFINITE
                                    interpolator = LinearInterpolator()
                                    addUpdateListener { value ->
                                        greyPolyline?.let { grey ->
                                            val points = grey.points
                                            val percentValue = value.animatedValue.toString().toInt()
                                            val size = points.size
                                            val newPoints = (size * (percentValue / 100.0f)).toInt()
                                            blackPolyline?.points = points.subList(0, newPoints)
                                        }
                                    }
                                }
                                valueAnimator.start()
                            }

                            val latLngBound = LatLngBounds.Builder()
                                .include(selectedPlaceEvent!!.origin)
                                .include(selectedPlaceEvent!!.destination)
                                .build()

                            val objects = jsonArray.getJSONObject(0)
                            val legs = objects.getJSONArray("legs")
                            val legsObject = legs.getJSONObject(0)

                            val time = legsObject.getJSONObject("duration")
                            val duration = time.getString("text")

                            val bitmap = Common.creaateIconWithDuration(this@RequestDriverActivity,duration)
                            originMarker!!.setIcon(bitmap?.let {
                                BitmapDescriptorFactory.fromBitmap(
                                    it
                                )
                            })

                            //moving
                            val runnable= object : Runnable {
                                override fun run() {
                                    if (index < polylineList!!.size -2)
                                    {
                                        index++
                                        next = index+1
                                        start = polylineList!![index]
                                        end = polylineList!![next]
                                    }
                                    val valueAnimator = ValueAnimator.ofInt(0,1)
                                    valueAnimator.duration=1500
                                    valueAnimator.interpolator = LinearInterpolator()
                                    valueAnimator.addUpdateListener { valueAnimatorNew ->
                                        v = valueAnimatorNew.animatedFraction
                                        lat = v*end!!.latitude+(1-v)*start!!.latitude
                                        lng = v*end!!.latitude+(1-v)*start!!.latitude
                                        val newPos = LatLng(lat,lng)
                                        marker.position =newPos
                                        marker.setAnchor(0.5f,0.5f)
                                        marker.rotation = Common.getBearing(start!!,newPos)
                                        mMap.moveCamera(CameraUpdateFactory.newLatLng(newPos))
                                    }
                                    valueAnimator.start()
                                    if (index < polylineList!!.size -2) handler!!.postDelayed(this,1500)
                                }

                            }
                            handler = Handler()
                            index = -1
                            next=1
                            handler!!.postDelayed(runnable,1500)
                            driverOldPosition = to //actulizar driver position

                        } else {
                            Log.e("Error", "Respuesta de la API vacía")
                        }
                    } catch (e: Exception) {
                        Log.e("Error", "Error procesando datos: ${e.message}")
                        mapFragment?.view?.let {
                            Snackbar.make(it, e.message ?: "Error desconocido", Snackbar.LENGTH_LONG).show()
                        }}
                })
    }

    private fun addDriverMarker(destination: LatLng) {
        destinationMarker=mMap.addMarker(MarkerOptions().position(destination).flat(true)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.school_icon)))

    }

    private fun addPickupMarkerWithDuration(duration: String, origin: LatLng) {
        val icon= Common.creaateIconWithDuration(this@RequestDriverActivity,duration)!!
        originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon)).position(origin))

    }

    /*@Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onDeclineReceived(event: DeclineRequestFromDriver)
    {
        if(lastDriverCall != null)
        {
            Common.driverFound.get(lastDriverCall!!.key!!)!!.isDecline=true
            findNearbyDriver(selectedPlaceEvent!!.origin!!)
        }
    }*/

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectPlaceEvent(event: SelectedPlaceEvent) {
        selectedPlaceEvent = event

        drawPath(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRequestDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initial()


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map2) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initial() {

        btnConfirm = findViewById(R.id.btn_confirm)
        mainLayout = findViewById(R.id.main_layout)
        iGoogleAPI = RetrofitClient.instance!!.create(IGoogleAPI::class.java)


        //Event
        btnConfirm.setOnClickListener {

            if (mMap == null) return@setOnClickListener
            if (selectedPlaceEvent == null) return@setOnClickListener

            //clear map
            mMap.clear()

            //tilt
            val cameraPos = CameraPosition.builder().target(selectedPlaceEvent!!.origin)
                .tilt(45f)
                .zoom(16f)
                .build()
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPos))

            //start Animation
            addMarkerWithPulseAnimation()
        }
    }

    private fun addMarkerWithPulseAnimation() {

        fillMap = findViewById(R.id.fill_maps)
        findingYourRide = findViewById(R.id.finding_your_ride_layout)

        btnConfirm.visibility=View.GONE
        fillMap.visibility = View.GONE
        findingYourRide.visibility = View.VISIBLE

        originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker())
            .position(selectedPlaceEvent!!.origin))

        addPulsatingEffect(selectedPlaceEvent!!)

    }

    private fun addPulsatingEffect(selectedPlaceEvent: SelectedPlaceEvent) {
        if (lastPulseAnimator != null) lastPulseAnimator!!.cancel()
        if (lastUserCircle != null) lastUserCircle!!.center = selectedPlaceEvent.origin
        lastPulseAnimator = Common.valueAnimate(duration,object :ValueAnimator.AnimatorUpdateListener{
            override fun onAnimationUpdate(p0: ValueAnimator) {
                if (lastUserCircle != null) lastUserCircle!!.radius = p0!!.animatedValue.toString().toDouble()
                else
                {
                    lastUserCircle = mMap.addCircle(CircleOptions()
                        .center(selectedPlaceEvent.origin)
                        .radius(p0?.animatedValue.toString().toDouble())
                        .strokeColor(android.graphics.Color.WHITE)
                        .fillColor(ContextCompat.getColor(this@RequestDriverActivity,R.color.map_darker)))
                }
            }

        })

        //Star roting camera
        startMapCameraSpinninAnimation(selectedPlaceEvent)


    }

    private fun startMapCameraSpinninAnimation(selectedPlaceEvent: SelectedPlaceEvent) {
        if (animator != null) animator!!.cancel()
        animator = ValueAnimator.ofFloat(0f,(DESIRED_NUM_OF_SPINS*360).toFloat())
        animator!!.duration = (DESIRED_NUM_OF_SPINS*DESIRE_SECONDS_PER_ONE_FULL_360_SPIN*1000).toLong()
        animator!!.interpolator = LinearInterpolator()
        animator!!.startDelay = (100)
        animator!!.addUpdateListener { valueAnimator ->
            val newBearingValue = valueAnimator.animatedValue as Float
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder()
                .target(selectedPlaceEvent.origin)
                .zoom(16f)
                .tilt(45f)
                .bearing(newBearingValue)
                .build()
            ))
        }
        animator!!.start()

        findNearbyDriver(selectedPlaceEvent)
    }

    private fun findNearbyDriver(selectedPlaceEvent: SelectedPlaceEvent) {

        if (Common.driverFound.size > 0)
        {
            var min = 0f
            var foundDriver :DriverGeoModel?=null
            val currentRiderLocation = Location("")
            currentRiderLocation.latitude = selectedPlaceEvent.origin.latitude//puede qu alguno tenga q tener null !!
            currentRiderLocation.longitude = selectedPlaceEvent.origin.longitude

            for (key in Common.driverFound.keys)
            {
                val driverLocation = Location("")
                driverLocation.latitude = Common.driverFound[key]?.geoLocation!!.latitude
                driverLocation.longitude = Common.driverFound[key]?.geoLocation!!.longitude

                if (min == 0f)
                {
                    min = driverLocation.distanceTo(currentRiderLocation)
                    if (!Common.driverFound[key]!!.isDecline)
                    {
                        foundDriver = Common.driverFound[key]
                        break;
                    }else
                        continue;

                }
                else if(driverLocation.distanceTo(currentRiderLocation) < min)
                {
                    min = driverLocation.distanceTo(currentRiderLocation)
                    if (!Common.driverFound[key]!!.isDecline)
                    {
                        foundDriver = Common.driverFound[key]
                        break;
                    }else
                        continue;
                }
            }


            if(foundDriver!=null)
            {
                UserUtils.sendRequestToDriver(this@RequestDriverActivity,
                    mainLayout,
                    foundDriver,
                    selectedPlaceEvent)
                lastDriverCall=foundDriver
            }
            else
            {
                Toast.makeText(this,getString(R.string.no_drive_accept),Toast.LENGTH_SHORT).show()
            }
        }
        else
        {
            Snackbar.make(mainLayout,getString(R.string.driver_not_found),
                Snackbar.LENGTH_LONG).show()
            lastDriverCall=null
            finish()
        }
    }

    override fun onDestroy() {
        if (animator != null) animator!!.end()
        super.onDestroy()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.setOnMyLocationClickListener {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedPlaceEvent!!.origin, 18f))
            true
        }

        drawPath(selectedPlaceEvent!!)

        //layout button
        val locationButton = (findViewById<View>("1".toInt()).parent as View)
            .findViewById<View>("2".toInt())

        //ajustar la posición
        val params = locationButton.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_TOP, 0)
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        params.bottomMargin = 250
        locationButton.layoutParams = params

        mMap.uiSettings.isZoomControlsEnabled = true
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.maps_style
                )
            )
            if (!success)
                Snackbar.make(
                    mapFragment.requireView(),
                    "Load map style failed",
                    Snackbar.LENGTH_LONG
                ).show()
        } catch (e: Exception) {
            Snackbar.make(mapFragment.requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun drawPath(selectedPlaceEvent: SelectedPlaceEvent) {
        if (selectedPlaceEvent.originString.isNullOrEmpty() || selectedPlaceEvent.destinationString.isNullOrEmpty()) {
            Log.e("Error", "Origen o destino no válidos")
            return
        }

        iGoogleAPI.getDirections(
            "driving",
            "less_driving",
            selectedPlaceEvent.originString,
            selectedPlaceEvent.destinationString,
            getString(R.string.google_api_key)
        )
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())?.let {
                compositeDisposable.add(
                it
                    .subscribe({ returnResult ->
                        try {
                            if (returnResult.isNotEmpty()) {
                                val jsonObject = JSONObject(returnResult)
                                val jsonArray = jsonObject.getJSONArray("routes")

                                for (i in 0 until jsonArray.length()) {
                                    val route = jsonArray.getJSONObject(i)
                                    val poly = route.getJSONObject("overview_polyline")
                                    val polyline = poly.getString("points")
                                    polylineList = ArrayList(Common.decodePoly(polyline))
                                }

                                polylineList?.let {
                                    polylineOptions = PolylineOptions().apply {
                                        color(android.graphics.Color.GRAY)
                                        width(12f)
                                        startCap(SquareCap())
                                        jointType(JointType.ROUND)
                                        addAll(it)
                                    }
                                    greyPolyline = mMap?.addPolyline(polylineOptions!!)

                                    blackPolylineOptions = PolylineOptions().apply {
                                        color(android.graphics.Color.BLACK)
                                        width(12f)
                                        startCap(SquareCap())
                                        jointType(JointType.ROUND)
                                        addAll(it)
                                    }
                                    blackPolyline = mMap?.addPolyline(blackPolylineOptions!!)

                                    // ANIMATOR
                                    val valueAnimator = ValueAnimator.ofInt(0, 100).apply {
                                        duration = 1100
                                        repeatCount = ValueAnimator.INFINITE
                                        interpolator = LinearInterpolator()
                                        addUpdateListener { value ->
                                            greyPolyline?.let { grey ->
                                                val points = grey.points
                                                val percentValue = value.animatedValue.toString().toInt()
                                                val size = points.size
                                                val newPoints = (size * (percentValue / 100.0f)).toInt()
                                                blackPolyline?.points = points.subList(0, newPoints)
                                            }
                                        }
                                    }
                                    valueAnimator.start()
                                }

                                val latLngBound = LatLngBounds.Builder()
                                    .include(selectedPlaceEvent.origin)
                                    .include(selectedPlaceEvent.destination)
                                    .build()

                                val objects = jsonArray.getJSONObject(0)
                                val legs = objects.getJSONArray("legs")
                                val legsObject = legs.getJSONObject(0)

                                val time = legsObject.getJSONObject("duration")
                                val duration = time.getString("text")

                                val startAddress = legsObject.getString("start_address")
                                val endAddress = legsObject.getString("end_address")

                                addOriginMarker(duration, startAddress)
                                addDestinationMarker(endAddress)

                                mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
                                mMap?.moveCamera(CameraUpdateFactory.zoomTo(mMap?.cameraPosition?.zoom?.minus(1) ?: 0f))
                            } else {
                                Log.e("Error", "Respuesta de la API vacía")
                            }
                        } catch (e: Exception) {
                            Log.e("Error", "Error procesando datos: ${e.message}")
                            mapFragment?.view?.let {
                                Snackbar.make(it, e.message ?: "Error desconocido", Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }, { error ->
                        Log.e("Error", "Error en la API: ${error.message}")
                        mapFragment?.view?.let {
                            Snackbar.make(it, error.message ?: "Error desconocido", Snackbar.LENGTH_LONG).show()
                        }
                    })
            )
            }
    }


    private fun addDestinationMarker(endAddress: String) {
        val view = layoutInflater.inflate(R.layout.destination_info_windows,null)

        val txt_destination = view.findViewById<View>(R.id.txt_destination) as TextView
        txt_destination.text = Common.formatAddress(endAddress)

        val generator = IconGenerator (this)
        generator.setContentView(view)
        //generator.setBackground(ColorDrawable(Color.Transparent))
        val icon = generator.makeIcon()

        destinationMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon))
            .position(selectedPlaceEvent!!.destination))

    }

    private fun addOriginMarker(duration: String, startAddress: String) {
        val view = layoutInflater.inflate(R.layout.origin_info_windows, null)

        val txt_time = view.findViewById<View>(R.id.txt_time) as TextView
        val txt_origin = view.findViewById<View>(R.id.txt_origin) as TextView

        txt_time.text = Common.formatDuration(duration)
        txt_origin.text = Common.formatAddress(startAddress)

        val generator = IconGenerator (this)
        generator.setContentView(view)
        //generator.setBackground(ColorDrawable(Color.Transparent))
        val icon = generator.makeIcon()

        originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon))
            .position(selectedPlaceEvent!!.origin))


    }
}