package com.example.nfurgontutor

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.nfurgontutor.Common.Common
import com.example.nfurgontutor.Model.EventBus.SelectedPlaceEvent
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
import com.google.maps.android.ui.IconGenerator
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.lang.StringBuilder

class RequestDriverActivity : AppCompatActivity(), OnMapReadyCallback {

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

    val btnconfirm = findViewById<Button>(R.id.btn_confirm)
    val btnrefuse = findViewById<Button>(R.id.btn_refuse)
    val fillmap = findViewById<View>(R.id.fill_maps)
    //puede que alla un problema  con findingYourRide porque es un card
    val findingYourRide = findViewById<View>(R.id.finding_your_ride_layout)
    val mainlayout = findViewById<RelativeLayout>(R.id.main_layout)


    override fun onStart() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
        super.onStart()

    }

    override fun onStop() {
        compositeDisposable.clear()
        if (EventBus.getDefault().hasSubscriberForEvent(SelectedPlaceEvent::class.java))
            EventBus.getDefault().removeStickyEvent(SelectedPlaceEvent::class.java)
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectPlaceEvent(event: SelectedPlaceEvent) {
        selectedPlaceEvent = event
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initial()


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initial() {

        iGoogleAPI = RetrofitClient.instance!!.create(IGoogleAPI::class.java)


        //Event
        btnconfirm.setOnClickListener {
            btnrefuse.visibility = View.GONE

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
        btnconfirm.visibility = View.GONE
        fillmap.visibility = View.VISIBLE
        findingYourRide.visibility = View.VISIBLE

        originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker())
            .position(selectedPlaceEvent!!.origin))

        addPulsatingEffect(selectedPlaceEvent!!.origin)

    }

    private fun addPulsatingEffect(origin: LatLng) {
        if (lastPulseAnimator != null) lastPulseAnimator!!.cancel()
        if (lastUserCircle != null) lastUserCircle!!.center = origin
        lastPulseAnimator = Common.valueAnimate(duration,object :ValueAnimator.AnimatorUpdateListener{
            override fun onAnimationUpdate(p0: ValueAnimator) {
                if (lastUserCircle != null) lastUserCircle!!.radius = p0!!.animatedValue.toString().toDouble()
                else
                {
                    lastUserCircle = mMap.addCircle(CircleOptions()
                        .center(origin)
                        .radius(p0!!.animatedValue.toString().toDouble())
                        .strokeColor(android.graphics.Color.WHITE)
                        .fillColor(ContextCompat.getColor(this@RequestDriverActivity,R.color.map_darker)))
                }
            }

        })

        //Star roting camera
        startMapCameraSpinninAnimation(mMap.cameraPosition.target)


    }

    private fun startMapCameraSpinninAnimation(target: LatLng) {
        if (animator != null) animator!!.cancel()
        animator = ValueAnimator.ofFloat(0f,(DESIRED_NUM_OF_SPINS*360).toFloat())
        animator!!.duration = (DESIRED_NUM_OF_SPINS*DESIRE_SECONDS_PER_ONE_FULL_360_SPIN*1000).toLong()
        animator!!.interpolator = LinearInterpolator()
        animator!!.startDelay = (100)
        animator!!.addUpdateListener { valueAnimator ->
            val newBearingValue = valueAnimator.animatedValue as Float
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder()
                .target(target)
                .zoom(16f)
                .tilt(45f)
                .bearing(newBearingValue)
                .build()
            ))
        }
        animator!!.start()

        findNearbyDriver(target)
    }

    private fun findNearbyDriver(target: LatLng) {


        if (Common.driverFound.size > 0)
        {
            var min = 0f
            var foundDriver = Common.driverFound[Common.driverFound.keys.iterator().next()]
            val currentRiderLocation = Location("")
            currentRiderLocation.latitude = target!!.latitude
            currentRiderLocation.longitude = target!!.longitude

            for (key in Common.driverFound.keys)
            {
                val driverLocation = Location("")
                driverLocation.latitude = Common.driverFound[key]!!.geoLocation!!.latitude
                driverLocation.longitude = Common.driverFound[key]!!.geoLocation!!.longitude

                if (min == 0f)
                {
                    min = driverLocation.distanceTo(currentRiderLocation)
                    foundDriver = Common.driverFound[key]
                }
                else if(driverLocation.distanceTo(currentRiderLocation) < min)
                {
                    min = driverLocation.distanceTo(currentRiderLocation)
                    foundDriver = Common.driverFound[key]
                }
            }
            //este dicta la info del conductor cuando lo encuentra
//            Snackbar.make(mainlayout,StringBuilder("Conductor encontrado: ")
//                .append(foundDriver!!.driverInfoModel!!.phoneNumber),
//                       Snackbar.LENGTH_LONG).show()

            UserUtils.sendRequestToDriver(this@RequestDriverActivity,
                mainlayout,
                foundDriver,
                target)
        }
        else
        {
            Snackbar.make(mainlayout,getString(R.string.driver_not_found),
                Snackbar.LENGTH_LONG).show()
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
        //request api
        compositeDisposable.add(iGoogleAPI.getDirections(
            "driving",
            "less_driving",
            selectedPlaceEvent.originString, selectedPlaceEvent.destinationString,
            getString(R.string.google_api_key)
        )
        !!.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { returnResult ->
                Log.d("API_RETURN", returnResult)
                try {
                    val jsonObject = JSONObject(returnResult)
                    val jsonArray = jsonObject.getJSONArray("routes");

                    for (i in 0 until jsonArray.length())
                    {
                        val route = jsonArray.getJSONObject(i)
                        val poly = route.getJSONObject("overview_polyline")
                        val polyline = poly.getString("points")
                        polylineList = ArrayList(Common.decodePoly(polyline))
                    }

                    polylineOptions = PolylineOptions()
                    polylineOptions!!.color(android.graphics.Color.GRAY)
                    polylineOptions!!.width(12f)
                    polylineOptions!!.startCap(SquareCap())
                    polylineOptions!!.jointType(JointType.ROUND)
                    //LE AGREGE !!
                    polylineOptions!!.addAll(polylineList!!)
                    greyPolyline = mMap.addPolyline(polylineOptions!!)

                    blackPolylineOptions = PolylineOptions()
                    blackPolylineOptions!!.color(android.graphics.Color.BLACK)
                    blackPolylineOptions!!.width(12f)
                    blackPolylineOptions!!.startCap(SquareCap())
                    blackPolylineOptions!!.jointType(JointType.ROUND)
                    //LE AGREGUE!!
                    blackPolylineOptions!!.addAll(polylineList!!)
                    blackPolyline = mMap.addPolyline(blackPolylineOptions!!)

                    //ANIMATOR
                    val valueAnimator = ValueAnimator.ofInt(0,100)
                    valueAnimator.duration = 1100
                    valueAnimator.repeatCount = ValueAnimator.INFINITE
                    valueAnimator.interpolator = LinearInterpolator()
                    valueAnimator.addUpdateListener { value ->
                        val points = greyPolyline!!.points
                        val percentValue = value.animatedValue.toString().toInt()
                        val size = points.size
                        val newpoints = (size * (percentValue/100.0f)).toInt()
                        val p=points.subList(0,newpoints)
                        blackPolyline!!.points=(p)
                    }
                    valueAnimator.start()

                    val latLngBound = LatLngBounds.Builder().include(selectedPlaceEvent.origin)
                        .include(selectedPlaceEvent.destination)
                        .build()

                    //agrega el icono del auto para el origen
                    val objects = jsonArray.getJSONObject(0)
                    val legs = objects.getJSONArray("legs")
                    val legsObject = legs.getJSONObject(0)

                    val time = legsObject.getJSONObject("duration")
                    val duration = time.getString("text")

                    val start_address = legsObject.getString("start_address")
                    val end_address= legsObject.get("end_address")

                    addOriginMarker(duration,start_address)
                    //se le agrego tostring
                    addDestinationMarker(end_address.toString())

                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound,160))
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition!!.zoom-1))


                } catch (e: java.lang.Exception)
                {
                    Snackbar.make(mapFragment.requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
                }
            }
        )
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