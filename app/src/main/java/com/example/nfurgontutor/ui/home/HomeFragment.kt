package com.example.nfurgontutor.ui.home

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.nfurgontutor.Callback.FirebaseDriverInfoListener
import com.example.nfurgontutor.Callback.FirebaseFailedListener
import com.example.nfurgontutor.Common.Common
import com.example.nfurgontutor.Model.AnimationModel
import com.example.nfurgontutor.Model.DriverGeoModel
import com.example.nfurgontutor.Model.DriverInfoModel
import com.example.nfurgontutor.Model.EventBus.SelectedPlaceEvent
import com.example.nfurgontutor.Model.GeoQueryModel
import com.example.nfurgontutor.R
import com.example.nfurgontutor.Remote.IGoogleAPI
import com.example.nfurgontutor.Remote.OSRMApi
import com.example.nfurgontutor.Remote.RetrofitClient
import com.example.nfurgontutor.RequestDriverActivity
//import com.example.nfurgontutor.databinding.ActivityMapsBinding
import com.example.nfurgontutor.databinding.FragmentHomeBinding
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.Arrays
import java.util.Locale

class HomeFragment : Fragment(), OnMapReadyCallback, FirebaseDriverInfoListener {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private var _binding: FragmentHomeBinding? = null

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment:SupportMapFragment

    private val binding get() = _binding!!

    private lateinit var slidingUpPanelLayout: SlidingUpPanelLayout
    private lateinit var txt_welcome:TextView
    private lateinit var autocompleteSupportFragment: AutocompleteSupportFragment

    //location
    var locationRequest: LocationRequest?=null
    var locationCallback: LocationCallback?=null
    var fusedLocationProviderClient: FusedLocationProviderClient?=null

    var distance = 5.0
    val LIMIT_RANGE = 1.0
    var previousLocation : Location? = null
    var currentLocation : Location? = null

    var firstTime = true

    //Listener
    lateinit var iFirebaseDriverInfoListener: FirebaseDriverInfoListener
    lateinit var iFirebaseFailedListener: FirebaseFailedListener

    var cityName = ""


    val compositeDisposable = CompositeDisposable()
    lateinit var iGoogleAPI: IGoogleAPI
    lateinit var osrmApi:OSRMApi

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onDestroy() {
        locationCallback?.let { fusedLocationProviderClient!!.removeLocationUpdates(it) }
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        osrmApi = RetrofitClient.retrofitOSRM.create(OSRMApi::class.java)

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        initViews(root)
        init()


        return root
    }

    private fun initViews(root: View?) {
        slidingUpPanelLayout = root!!.findViewById(R.id.activity_main) as SlidingUpPanelLayout
        txt_welcome = root!!.findViewById(R.id.txt_welcome) as TextView

        Common.setWelcomeMessage(txt_welcome)

        // Configurar botón para marcar lugar fijo
        val btnschool: Button = root.findViewById(R.id.btn_go_to_school)
        btnschool.setOnClickListener {
            markFixedPlace()
        }
    }
    private fun markFixedPlace() {
        // Coordenadas del lugar fijo
        val fixedLatLng = LatLng(-36.212374, -71.603723) // Coordenadas del colegio Camelias



        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationProviderClient!!.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val origin = LatLng(location.latitude, location.longitude)

                // Inicia actividad o proceso para dirigirse al colegio
                startActivity(Intent(requireContext(), RequestDriverActivity::class.java))
                EventBus.getDefault().postSticky(SelectedPlaceEvent(origin, fixedLatLng))
            } else {
                Snackbar.make(requireView(), "No se pudo obtener la ubicación actual", Snackbar.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Snackbar.make(requireView(), "Error al obtener la ubicación: ${it.message}", Snackbar.LENGTH_LONG).show()
        }
        // Mover la cámara para enfocar el marcador
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(fixedLatLng, 15f))
    }

    //permisos
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestLocationPermission() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun init() {


        Places.initialize(requireContext(),getString(R.string.google_api_key))
        autocompleteSupportFragment = childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteSupportFragment.setPlaceFields(
            listOf(Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.NAME))

        autocompleteSupportFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {

                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }
            override fun onError(status: Status) {
                Snackbar.make(requireView(), status.statusMessage ?: "Error desconocido", Snackbar.LENGTH_LONG).show()
            }

        })

        // Inicializar iFirebaseFailedListener
        iFirebaseFailedListener = object : FirebaseFailedListener {
            override fun onFirebaseFailed(message: String) {
                Log.e("FIREBASE_ERROR", message)
                Snackbar.make(requireView(), "Error: $message", Snackbar.LENGTH_LONG).show()
            }
        }

        iGoogleAPI = RetrofitClient.instance!!.create(IGoogleAPI::class.java)
        iFirebaseDriverInfoListener = this

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        buildLocationRequest()
        buildLocationCallback()
        updateLocation()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Verificar permisos y solicitar actualizaciones de ubicación
        if (hasLocationPermission()) {
            fusedLocationProviderClient!!.requestLocationUpdates(locationRequest!!, locationCallback!!, Looper.myLooper())
        } else {
            requestLocationPermission()
        }

        loadDriver()
    }

    private fun buildLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val newPos = LatLng(
                    locationResult.lastLocation!!.latitude,
                    locationResult.lastLocation!!.longitude
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))

                if (firstTime) {
                    previousLocation = locationResult.lastLocation
                    currentLocation = locationResult.lastLocation

                    //setRestrictPlacesInCountry(locationResult.lastLocation)
                    getCityName(locationResult.lastLocation!!)
                    firstTime = false
                } else {
                    previousLocation = currentLocation
                    currentLocation = locationResult.lastLocation
                }
                if (previousLocation != null && currentLocation != null) {
                    if (previousLocation!!.distanceTo(currentLocation!!) / 1000 <= LIMIT_RANGE) {
                        loadDriver()
                    }
                }
            }
        }
    }

    private fun buildLocationRequest() {
        if(locationRequest==null)
        {
            /*locationRequest = LocationRequest()
            locationRequest!!.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            locationRequest!!.setFastestInterval(3000)
            locationRequest!!.setSmallestDisplacement(10f)
            locationRequest!!.interval = 5000*/
            locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, // Establece la prioridad de ubicación
                3000 // Intervalo en milisegundos
            ).apply {
                setWaitForAccurateLocation(true) // Esperar una ubicación más precisa si es posible
                setMinUpdateIntervalMillis(1000) // Intervalo mínimo de actualización
                setMinUpdateDistanceMeters(5f)  // Desplazamiento mínimo entre actualizaciones
            }.build()
        }
    }

    private fun updateLocation() {
        if (fusedLocationProviderClient == null)
        {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ){
                return
            }
            fusedLocationProviderClient!!.requestLocationUpdates(
                locationRequest!!,
                locationCallback!!,
                Looper.myLooper()
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes iniciar las actualizaciones de ubicación
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                fusedLocationProviderClient!!.requestLocationUpdates(locationRequest!!, locationCallback!!, Looper.myLooper())
            } else {
                // Permiso denegado, muestra un mensaje al usuario
                Snackbar.make(requireView(), "Se necesita permiso de ubicación para continuar", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun getCityName(location: Location): String? {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses!!.isNotEmpty()) addresses[0].locality else null
        } catch (e: IOException) {
            Log.e("GET_CITY_NAME", "Error al obtener el nombre de la ciudad: ${e.message}")
            null
        }
    }

    private fun loadDriver() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(requireView(),getString(R.string.permission_require),Snackbar.LENGTH_SHORT).show()
            return
        }
        fusedLocationProviderClient!!.lastLocation
            .addOnFailureListener { e ->
                Snackbar.make(requireView(),e.message!!,Snackbar.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { location ->
                //load driver

                val geoCoder = Geocoder(requireContext(), Locale.getDefault())
                var addressList : List<Address>?

                try {

                    addressList = geoCoder.getFromLocation(location.latitude,location.longitude,1)!!
                    cityName = addressList[0].locality

                    //query
                    if (!TextUtils.isEmpty(cityName)){
                    val driver_location_ref = FirebaseDatabase.getInstance()
                        .getReference(Common.DRIVER_LOCATION_REFERENCE)
                        .child(cityName)

                    val gf = GeoFire(driver_location_ref)
                    val geoQuery = gf.queryAtLocation(GeoLocation(location.latitude,location.longitude),distance)
                    geoQuery.removeAllListeners()

                    geoQuery.addGeoQueryEventListener(object:GeoQueryEventListener{
                        override fun onKeyEntered(key: String?, location: GeoLocation?) {
                            //Common.driverFound.add(DriverGeoModel(key!!,location!!))
                            if (!Common.driverFound.containsKey(key))
                                Common.driverFound[key!!] = DriverGeoModel(key,location)
                        }
                        override fun onKeyExited(key: String?) {
                            Log.d("LOAD_DRIVER", "Conductor salió del rango: $key")
                        }

                        override fun onKeyMoved(key: String?, location: GeoLocation?) {
                            Log.d("LOAD_DRIVER", "Conductor movido: $key a $location")
                        }

                        override fun onGeoQueryReady() {
                            Log.d("LOAD_DRIVER", "Conductores encontrados: ${Common.driverFound.size}")
                            if (distance <= LIMIT_RANGE)
                            {
                                distance++
                                loadDriver()
                            }
                            else{
                                distance = 0.0
                                addDriverMarker()
                            }
                        }

                        override fun onGeoQueryError(error: DatabaseError?) {
                            Snackbar.make(requireView(),error!!.message!!,Snackbar.LENGTH_SHORT).show()
                            Log.e("LOAD_DRIVER", "Error al cargar conductores: ${error.message}")
                        }

                    })

                    driver_location_ref.addChildEventListener(object: ChildEventListener{

                        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                            val geoQueryModel = snapshot.getValue(GeoQueryModel::class.java)
                            val geoLocation = GeoLocation(geoQueryModel!!.l!![0],geoQueryModel!!.l!![1])
                            val driverGeoModel = DriverGeoModel(snapshot.key,geoLocation)
                            val newDriverlocation = Location("")
                            newDriverlocation.latitude = geoLocation.latitude
                            newDriverlocation.longitude = geoLocation.longitude
                            val newDistance = location.distanceTo(newDriverlocation)/1000 //in km
                            if (newDistance <= LIMIT_RANGE)
                                findDriverByKey(driverGeoModel)

                            Log.d("LOAD_DRIVER", "Lugar del conductor: ${snapshot.value}")
                        }


                        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {

                        }

                        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

                        }

                        override fun onCancelled(error: DatabaseError) {
                            Snackbar.make(requireView(),error.message,Snackbar.LENGTH_SHORT).show()

                        }

                    })}
                    else
                        Snackbar.make(requireView(),getString(R.string.ciudad_no_encontrada),Snackbar.LENGTH_SHORT).show()
                }catch (e:IOException) {
                    Snackbar.make(requireView(),e.message!!,Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    private fun addDriverMarker() {
        if (Common.driverFound.size > 0)
        {
            Observable.fromIterable(Common.driverFound.keys)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { key:String? ->
                        findDriverByKey(Common.driverFound[key!!])
                    },
                    {
                        t: Throwable? -> Snackbar.make(requireView(),t!!.message!!,Snackbar.LENGTH_SHORT).show()
                    }
                )
        }
        else
        {
            Snackbar.make(requireView(),getString(R.string.driver_not_found),Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun findDriverByKey(driverGeomodel: DriverGeoModel?) {
        FirebaseDatabase.getInstance()
            .getReference(Common.DRIVER_INFO_REFERECE)
            .child(driverGeomodel!!.key!!)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.hasChildren())
                    {
                        driverGeomodel.driverInfoModel = (p0.getValue(DriverInfoModel::class.java))
                        Common.driverFound[driverGeomodel.key]?.driverInfoModel= (p0.getValue(DriverInfoModel::class.java))
                        iFirebaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeomodel)
                        Log.d("FIND_DRIVER", "ID CONDUCTOR: ${driverGeomodel.key}")
                    }
                    else
                        iFirebaseFailedListener.onFirebaseFailed(getString(R.string.key_not_found)+driverGeomodel.key)
                }

                override fun onCancelled(error: DatabaseError) {
                    iFirebaseFailedListener.onFirebaseFailed(error.message)
                }

            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0!!

        /*Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            .withListener(object : PermissionListener, MultiplePermissionsListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {

                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {

                    } else{
                        Snackbar.make(requireView(), "Location permission is required", Snackbar.LENGTH_SHORT).show()
                    }

                    mMap.setOnMyLocationButtonClickListener {
                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener { e ->
                                Snackbar.make(requireView(), e.message ?: "Error getting location", Snackbar.LENGTH_LONG).show()
                            }
                            .addOnSuccessListener { location ->
                                if(location != null){
                                    val userLatLng = LatLng(location.latitude, location.longitude)
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))
                                } else{
                                    Snackbar.make(requireView(), "No location found. Turn on location services.", Snackbar.LENGTH_LONG).show()
                                }
                            }
                        true
                    }

                    //layout button location
                    val locationButton = (mapFragment.requireView()
                        .findViewById<View>("1".toInt())
                        .parent as View).findViewById<View>("2".toInt())

                    //ajustar la posición
                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_TOP,0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE)
                    params.bottomMargin = 250
                    locationButton.layoutParams = params
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Snackbar.make(requireView(),p0!!.permissionName+"needed for run app",
                        Snackbar.LENGTH_LONG).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                }

                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    //implementar logica
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    //implementar logica
                }
            })
            .check() *///aquii!!

        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report != null && report.areAllPermissionsGranted()) {
                        mMap.isMyLocationEnabled = true
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

                        mMap.setOnMyLocationButtonClickListener {
                            if (ActivityCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {

                            }
                            fusedLocationProviderClient!!.lastLocation
                                .addOnSuccessListener { location ->
                                    if (location != null) {
                                        val userLatLng = LatLng(location.latitude, location.longitude)
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))
                                    } else {
                                        Snackbar.make(
                                            requireView(),
                                            "No location found. Turn on location services.",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            true
                        }

                        // Ajustar la posición del botón de ubicación
                        val locationButton = (mapFragment.requireView()
                            .findViewById<View>("1".toInt())
                            .parent as View).findViewById<View>("2".toInt())
                        val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                        params.bottomMargin = 250
                        locationButton.layoutParams = params

                        buildLocationRequest()
                        buildLocationCallback()
                        updateLocation()


                    } else {
                        Snackbar.make(
                            requireView(),
                            "Permissions are required to run the app",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()



        //Enable zoom
        mMap.uiSettings.isZoomControlsEnabled = true

        try {
            val success = p0!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),R.raw.maps_style))
            if (!success)
                Snackbar.make(requireView(),"Load map style failed",
                    Snackbar.LENGTH_LONG).show()
        }catch (e:Exception)
        {
            Snackbar.make(requireView(),""+e.message,
                Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel?) {
        if (driverGeoModel != null) {
            if(!Common.markerList.containsKey(driverGeoModel.key))
                mMap.addMarker(MarkerOptions()
                    .position(LatLng(driverGeoModel!!.geoLocation!!.latitude,driverGeoModel!!.geoLocation!!.longitude))
                    .flat(true)
                    .title(Common.buildName(driverGeoModel.driverInfoModel!!.primer_nombre,driverGeoModel.driverInfoModel!!.apellido))
                    .snippet(driverGeoModel.driverInfoModel!!.numerocelular)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.school_icon)))?.let {
                    Common.markerList.put(driverGeoModel!!.key!!,
                        it
                    )
                }//!!
        }

        if (!TextUtils.isEmpty(cityName))
        {
            //llamando "driverLocation/cityname/{lat+lng}"
            val driverLocation = FirebaseDatabase.getInstance()
                .getReference(Common.DRIVER_LOCATION_REFERENCE)
                .child(cityName)
                .child(driverGeoModel!!.key!!)

            driverLocation.addValueEventListener(object: ValueEventListener{
                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(requireView(),error.message,Snackbar.LENGTH_SHORT).show()
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.hasChildren())
                    {
                        if (Common.markerList.get(driverGeoModel!!.key!!) != null)
                        {
                            val marker = Common.markerList.get(driverGeoModel!!.key!!)
                            marker!!.remove()//remove marker from map
                            Common.markerList.remove(driverGeoModel!!.key!!)//Remove marker info
                            Common.driversSubscribe.remove(driverGeoModel.key!!) //Remove driver info
                            if (Common.driverFound!=null && Common.driverFound[driverGeoModel!!.key!!]!=null)
                                Common.driverFound.remove(driverGeoModel!!.key!!)
                            driverLocation.removeEventListener(this)
                        }
                    }
                    else{
                        if (Common.markerList.get(driverGeoModel!!.key!!) != null)
                        {
                            val geoQueryModel = snapshot!!.getValue(GeoQueryModel::class.java)
                            val animationModel = AnimationModel(false,geoQueryModel!!)
                            if(Common.driversSubscribe.get(driverGeoModel.key!!) !=null)
                            {
                                val marker = Common.markerList.get(driverGeoModel!!.key!!)
                                val oldPosition = Common.driversSubscribe.get(driverGeoModel.key!!)

                                val from = StringBuilder()
                                    .append(oldPosition!!.geoQueryModel!!.l?.get(0))
                                    .append(",")
                                    .append(oldPosition!!.geoQueryModel!!.l?.get(1))
                                    .toString()

                                val to = StringBuilder()
                                    .append(animationModel.geoQueryModel!!.l?.get(0))
                                    .append(",")
                                    .append(animationModel.geoQueryModel!!.l?.get(1))
                                    .toString()

                                moveMarkerAnimation(driverGeoModel.key!!,animationModel,marker,from,to)

                            }
                            else
                                Common.driversSubscribe.put(driverGeoModel.key!!,animationModel) //first location init

                        }
                    }

                }
            })
        }
    }




    private fun moveMarkerAnimation(
        key: String,
        newData: AnimationModel,
        marker: Marker?,
        from: String,
        to: String
    ) {
        if(!newData.isRun)
        {
            //request api
            compositeDisposable.add(iGoogleAPI.getDirections("driving",
                "less_driving",
                from,to,
                getString(R.string.google_api_key))
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
                            //polylineList = Common.decodePoly(polyline)
                            newData.polylineList = Common.decodePoly(polyline)

                        }

                        //Moving
                        newData.index = -1
                        newData.next = 1

                        val runnable = object:Runnable{
                            override fun run(){
                                if(newData.polylineList != null && newData.polylineList!!.size > 1)
                                {
                                    if(newData.index > newData.polylineList!!.size -2)
                                    {
                                        newData.index++
                                        newData.next = newData.index+1
                                        newData.start = newData.polylineList!![newData.index]!!
                                        newData.end = newData.polylineList!![newData.next]!!
                                    }
                                    val valueAnimator = ValueAnimator.ofInt(0,1)
                                    valueAnimator.duration = 3000
                                    valueAnimator.interpolator = LinearInterpolator()
                                    valueAnimator.addUpdateListener { value ->
                                        newData.v = value.animatedFraction
                                        newData.lat = newData.v*newData.end!!.latitude + (1-newData.v)*newData.start!!.latitude
                                        newData.lng = newData.v*newData.end!!.longitude + (1-newData.v)*newData.start!!.longitude
                                        val newPos = LatLng(newData.lat,newData.lng)
                                        marker!!.position = newPos
                                        marker!!.setAnchor(0.5f,0.5f)
                                        marker!!.rotation = Common.getBearing(newData.start!!,newPos)
                                    }
                                    valueAnimator.start()
                                    if (newData.index < newData.polylineList!!.size -2)
                                        newData.handler!!.postDelayed(this,1500)
                                    else if (newData.index < newData.polylineList!!.size - 1)
                                    {
                                        newData.isRun = false
                                        Common.driversSubscribe.put(key,newData) //update
                                    }
                                }
                            }
                        }

                        newData.handler!!.postDelayed(runnable,1500)


                    }catch (e:java.lang.Exception)
                    {
                        Snackbar.make(requireView(),e.message!!,Snackbar.LENGTH_LONG).show()
                    }
                }
            )
        }
    }



}