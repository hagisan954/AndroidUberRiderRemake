package jp.ac.hal.androiduberriderremake.ui.home;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import jp.ac.hal.androiduberriderremake.Callback.IFirebaseDriverInfoListener;
import jp.ac.hal.androiduberriderremake.Callback.IFirebaseFailedListener;
import jp.ac.hal.androiduberriderremake.Common.Common;
import jp.ac.hal.androiduberriderremake.Model.AnimationModel;
import jp.ac.hal.androiduberriderremake.Model.DriverGeoModel;
import jp.ac.hal.androiduberriderremake.Model.DriverInfoModel;
import jp.ac.hal.androiduberriderremake.Model.EventBus.SelectPlaceEvent;
import jp.ac.hal.androiduberriderremake.Model.GeoQueryModel;
import jp.ac.hal.androiduberriderremake.R;
import jp.ac.hal.androiduberriderremake.Remote.IGoogleAPI;
import jp.ac.hal.androiduberriderremake.Remote.RetrofitClient;
import jp.ac.hal.androiduberriderremake.RequestDriverActivity;

public class HomeFragment extends Fragment implements OnMapReadyCallback, IFirebaseFailedListener, IFirebaseDriverInfoListener {

//    @BindView(R.id.activity_main)
//    SlidingUpPanelLayout slidingUpPanelLayout;
//    @BindView(R.id.txt_welcome)
//    TextView txt_welcome;
//    @BindView(R.id.search_place_layout)
//    LinearLayout search_place_layout;
    @BindView(R.id.select_drone)
    Button select_drone;

    private AutocompleteSupportFragment autocompleteSupportFragment;

    private HomeViewModel homeViewModel;

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    //Location
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    //Load Driver
    private double distance = 1.0; //default in km
    private static final double LIMIT_RANGE = 10.0; //km
    private Location previousLocation, currentLocation; //Use to calculate distance

    private boolean firstTime = true;

    //Listener
    IFirebaseDriverInfoListener iFirebaseDriverInfoListener;
    IFirebaseFailedListener iFirebaseFailedListener;
    private String cityName;

    //
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IGoogleAPI iGoogleAPI;

    private boolean isNextLaunch =false;






    @Override
    public void onStart() {
        super.onStart();


    }

    @Override
    public void onStop() {
        compositeDisposable.clear();


        super.onStop();
    }

    @Override
    public void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isNextLaunch)
        {
            loadAvailableDrivers();
        }
        else
            isNextLaunch = true;
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);


        mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initViews(root);
        init();
        return root;
    }

    private void initViews(View root) {
        ButterKnife.bind(this, root);
        //Common.setWelcomeMessage(txt_welcome);
    }

    private void init() {

        Places.initialize(getContext(), getString(R.string.google_maps_key));
//        autocompleteSupportFragment = (AutocompleteSupportFragment) getChildFragmentManager()
//                .findFragmentById(R.id.autocomplete_fragment);
//        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.ADDRESS, Place.Field.NAME, Place.Field.LAT_LNG));
//        autocompleteSupportFragment.setHint(getString(R.string.where_to));
//        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//            @Override
//            public void onPlaceSelected(@NonNull Place place) {
//                //Snackbar.make(getView(),"" + place.getLatLng(),Snackbar.LENGTH_LONG).show();
//                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                        && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    Snackbar.make(getView(),getString(R.string.permission_require),Snackbar.LENGTH_LONG).show();
//                    return;
//                }
//                fusedLocationProviderClient.getLastLocation()
//                        .addOnSuccessListener(location -> {
//
//                            LatLng origin = new LatLng(location.getLatitude(),location.getLongitude());
//                            LatLng destination = new LatLng(place.getLatLng().latitude,place.getLatLng().longitude);
//
//                            startActivity(new Intent(getContext(), RequestDriverActivity.class));
//                            EventBus.getDefault().postSticky(new SelectPlaceEvent(origin,destination,place.getAddress()));
//                        });
//            }
//
//            @Override
//            public void onError(@NonNull Status status) {
//                Snackbar.make(getView(),"" + status.getStatusMessage(),Snackbar.LENGTH_LONG).show();
//            }
//        });


//クリックイベント追加
        select_drone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(getView(),getString(R.string.permission_require),Snackbar.LENGTH_LONG).show();
                    return;
                }
                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                            LatLng destination = new LatLng(38, 38);

                            startActivity(new Intent(getContext(), RequestDriverActivity.class));
                            EventBus.getDefault().postSticky(new SelectPlaceEvent(origin, destination, "dokoka"));
                        });

            }
        });



        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class);

        iFirebaseFailedListener = this;
        iFirebaseDriverInfoListener = this;

        //Check permission
        if(ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Snackbar.make(mapFragment.getView(),getString(R.string.permission_require),Snackbar.LENGTH_SHORT).show();
            return;
        }

        buildLocationRequest();
        buildLocationCallback();
        updateLocation();

        //Add at end of init()
        loadAvailableDrivers();
    }

    private void updateLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if(ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
    }

    private void buildLocationCallback() {
        if(locationCallback == null)
        {
            locationCallback = new LocationCallback() {
                //Ctrl+O

                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);

                    LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition,18f));

                    //If user has change location, calculate and load driver again
                    if(firstTime)
                    {
                        previousLocation = currentLocation = locationResult.getLastLocation();
                        firstTime = false;
                        //setRestrictPlacesInCountry(locationResult.getLastLocation()); //Always set again,not just first time


                    }
                    else
                    {
                        previousLocation = currentLocation;
                        currentLocation = locationResult.getLastLocation();
                    }



                    if(previousLocation.distanceTo(currentLocation)/1000 <= LIMIT_RANGE) //Not over range
                        loadAvailableDrivers();
                    else
                    {
                        //Do nothing
                    }

                }
            };
        }
    }

    private void buildLocationRequest() {
        if(locationRequest == null)
        {
            locationRequest = new LocationRequest();
            locationRequest.setSmallestDisplacement(10f);
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(3000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    private void setRestrictPlacesInCountry(Location location) {
        try {
            Geocoder geocoder = new Geocoder(getContext(),Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
//            if(addressList.size() > 0)
//                autocompleteSupportFragment.setCountry(addressList.get(0).getCountryCode());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void loadAvailableDrivers() {

        if(ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(getView(),getString(R.string.permission_require),Snackbar.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> Snackbar.make(getView(),e.getMessage(),Snackbar.LENGTH_SHORT).show())
                .addOnSuccessListener(location -> {

                    Geocoder geocoder = new Geocoder(getContext(),Locale.getDefault());
                    List<Address> addressList;
                    try {
                            addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
                            if(addressList.size() > 0)
                                cityName = addressList.get(0).getLocality();
                        if(!TextUtils.isEmpty(cityName)) {
                            //Query
                            DatabaseReference driver_location_ref = FirebaseDatabase.getInstance()
                                    .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                                    .child(cityName);
                            GeoFire gf = new GeoFire(driver_location_ref);

                            GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(location.getLatitude(),
                                    location.getLongitude()), distance);

                            geoQuery.removeAllListeners();

                            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                                @Override
                                public void onKeyEntered(String key, GeoLocation location) {
                                    //Common.driversFound.add(new DriverGeoModel(key, location));
                                    if(!Common.driversFound.containsKey(key))
                                        Common.driversFound.put(key,new DriverGeoModel(key,location)); //Add if not exists
                                }

                                @Override
                                public void onKeyExited(String key) {

                                }

                                @Override
                                public void onKeyMoved(String key, GeoLocation location) {

                                }

                                @Override
                                public void onGeoQueryReady() {

                                    if (distance <= LIMIT_RANGE) {
                                        distance++;
                                        loadAvailableDrivers(); //Continue search in new distance
                                    } else {
                                        distance = 1.0; //Reset it
                                        addDriverMarker();
                                    }
                                }

                                @Override
                                public void onGeoQueryError(DatabaseError error) {
                                    Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_SHORT).show();

                                }
                            });

                            //Listen to new driver in city and range
                            driver_location_ref.addChildEventListener(new ChildEventListener() {
                                @Override
                                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                                    //Have new driver
                                    GeoQueryModel geoQeryModel = dataSnapshot.getValue(GeoQueryModel.class);
                                    GeoLocation geoLocation = new GeoLocation(geoQeryModel.getL().get(0),
                                            geoQeryModel.getL().get(1));
                                    DriverGeoModel driverGeoModel = new DriverGeoModel(dataSnapshot.getKey(),
                                            geoLocation);
                                    Location newDriverLocation = new Location("");
                                    newDriverLocation.setLatitude(geoLocation.latitude);
                                    newDriverLocation.setLongitude(geoLocation.longitude);
                                    float newDistance = location.distanceTo(newDriverLocation) / 1000; //in km
                                    if (newDistance <= LIMIT_RANGE)
                                        findDriverByKey(driverGeoModel); //If driver in range, add to map
                                }

                                @Override
                                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                                }

                                @Override
                                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                                }

                                @Override
                                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                        }
                        else
                            Snackbar.make(getView(),getString(R.string.city_name_empty),Snackbar.LENGTH_LONG).show();

                    } catch (IOException e) {
                        e.printStackTrace();
                            Snackbar.make(mapFragment.getView(),e.getMessage(),Snackbar.LENGTH_LONG).show();
                        }

                    });
    }

    private void addDriverMarker() {
        if(Common.driversFound.size() > 0)
        {
            Observable.fromIterable(Common.driversFound.keySet())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(key -> {
                        //On next
                        findDriverByKey(Common.driversFound.get(key));
                    },throwable -> {
                        Snackbar.make(getView(),throwable.getMessage(),Snackbar.LENGTH_SHORT).show();
                    },()->{


                    });
        }
        else
        {
            Snackbar.make(getView(),getString(R.string.drivers_not_found),Snackbar.LENGTH_SHORT).show();
        }
    }

    private void findDriverByKey(DriverGeoModel driverGeoModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.DRIVER_INFO_REFERENCE)
                .child(driverGeoModel.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChildren())
                        {
                            driverGeoModel.setDriverInfoModel(dataSnapshot.getValue(DriverInfoModel.class));
                            Common.driversFound.get(driverGeoModel.getKey()).setDriverInfoModel(dataSnapshot.getValue(DriverInfoModel.class));
                            iFirebaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeoModel);
                        }
                        else
                            iFirebaseFailedListener.onFirebaseLoadFailed(getString(R.string.not_found_key) + driverGeoModel.getKey());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        iFirebaseFailedListener.onFirebaseLoadFailed(databaseError.getMessage());
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;//Don't forget it
        //Check permission
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        if(ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Snackbar.make(mapFragment.getView(),getString(R.string.permission_require),Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        mMap.setOnMyLocationButtonClickListener(() -> {

                            if(ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return false;
                            }
                            fusedLocationProviderClient.getLastLocation()
                                    .addOnFailureListener(e -> Snackbar.make(getView(),e.getMessage(), Snackbar.LENGTH_SHORT).show())
                                    .addOnSuccessListener(location -> {
                                        LatLng userLatLng = new LatLng(location.getLatitude(),location.getLongitude());
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng,18f));
                                    });
                            return true;
                        });

                        //Layout button
                        View locationButton = ((View)mapFragment.getView().findViewById(Integer.parseInt("1")).getParent())
                                .findViewById(Integer.parseInt("2"));
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                        View locationButton2 =(View)mapFragment.getView().findViewById(Integer.parseInt("1"));
                        RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) locationButton2.getLayoutParams();
                        params2.addRule(RelativeLayout.ALIGN_PARENT_TOP,0);
                        params2.addRule(RelativeLayout.ALIGN_PARENT_TOP,RelativeLayout.TRUE);
                        params2.setMargins(0,100,0,0);

                        //Right bottom
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP,RelativeLayout.TRUE);
                        params.setMargins(0,0,0,300); // Move view to see Zoom control

                        buildLocationRequest();
                        buildLocationCallback();
                        updateLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Snackbar.make(getView(),permissionDeniedResponse.getPermissionName() + "need enable",
                                Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                })
        .check();//Don't forget 'check()' method

        mMap.getUiSettings().setZoomControlsEnabled(true);


        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(),R.raw.uber_maps_style));
            if(!success)
                Snackbar.make(getView(),"Load map style failed",
                        Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Snackbar.make(getView(),e.getMessage(),
                    Snackbar.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onFirebaseLoadFailed(String message) {
        Snackbar.make(getView(),message,Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel) {
        //If already have marker with this key,doesn't set again
        if (!Common.markerList.containsKey(driverGeoModel.getKey())) {
            Common.markerList.put(driverGeoModel.getKey(),
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(driverGeoModel.getGeoLocation().latitude,
                                    driverGeoModel.getGeoLocation().longitude))
                            .flat(true)
                            .title(Common.buildName(driverGeoModel.getDriverInfoModel().getFirstname(),
                                    driverGeoModel.getDriverInfoModel().getLastname()))
                            .snippet(driverGeoModel.getDriverInfoModel().getPhoneNumber())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.drone3))));//改善部分、ドローン画像に変更

            //改善部分、マーカーのクリックリスナーを作る。
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    //search_place_layout.setVisibility(View.GONE);
                    //slidingUpPanelLayout.setPanelHeight(110);
                    select_drone.setVisibility(View.VISIBLE);
                    return false;
                }
            });
        }
        if(!TextUtils.isEmpty(cityName))
        {
            DatabaseReference driverLocation = FirebaseDatabase.getInstance()
                    .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                    .child(cityName)
                    .child(driverGeoModel.getKey());
            driverLocation.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.hasChildren())
                    {
                        if(Common.markerList.get(driverGeoModel.getKey()) != null)
                            Common.markerList.get(driverGeoModel.getKey()).remove(); //Remove marker
                        Common.markerList.remove(driverGeoModel.getKey());  //Remove marker info from hash map
                        Common.driverLocationSubscribe.remove(driverGeoModel.getKey()); //Remove driver information too
                        if(Common.driversFound != null && Common.driversFound.size() > 0)  //Remove local infomation of Driver
                            Common.driversFound.remove(driverGeoModel.getKey());
                        driverLocation.removeEventListener(this);  //Remove event listener

                    }
                    else
                    {
                        if(Common.markerList.get(driverGeoModel.getKey()) != null)
                        {
                            GeoQueryModel geoQueryModel = dataSnapshot.getValue(GeoQueryModel.class);
                            AnimationModel animationModel = new AnimationModel(false,geoQueryModel);
                            if(Common.driverLocationSubscribe.get(driverGeoModel.getKey()) != null)
                            {
                                Marker currentMarker = Common.markerList.get(driverGeoModel.getKey());
                                AnimationModel oldPosition = Common.driverLocationSubscribe.get(driverGeoModel.getKey());

                                String from = new StringBuilder()
                                        .append(oldPosition.getGeoQueryModel().getL().get(0))
                                        .append(",")
                                        .append(oldPosition.getGeoQueryModel().getL().get(1))
                                        .toString();

                                String to = new StringBuilder()
                                        .append(animationModel.getGeoQueryModel().getL().get(0))
                                        .append(",")
                                        .append(animationModel.getGeoQueryModel().getL().get(1))
                                        .toString();

                                moveMarkerAnimation(driverGeoModel.getKey(),animationModel,currentMarker,from,to);
                            }
                            else
                            {
                                //First location init
                                Common.driverLocationSubscribe.put(driverGeoModel.getKey(),animationModel);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                    Snackbar.make(getView(),databaseError.getMessage(),Snackbar.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void moveMarkerAnimation(String key, AnimationModel animationModel, Marker currentMarker, String from, String to) {
        if(!animationModel.isRun())
        {
            //Request API
            compositeDisposable.add(iGoogleAPI.getDirections("driving","less_driving",
                    from,to,
                    getActivity().getString(R.string.google_api_key))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(returnResult -> {
                Log.d("API_RETURN",returnResult);

                try {
                    //Parse JSON
                    JSONObject jsonObject = new JSONObject(returnResult);
                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                    for (int i = 0;i < jsonArray.length();i++)
                    {
                        JSONObject route = jsonArray.getJSONObject(i);
                        JSONObject poly = route.getJSONObject("overview_polyline");
                        String polyline = poly.getString("points");
                        //polylineList = Common.decodePoly(polyline);
                        animationModel.setPolylineList(Common.decodePoly(polyline));
                    }

                    //Moving
                    //handler = new Handler();
                    //index = -1;
                    //next = 1;
                    animationModel.setIndex(-1);
                    animationModel.setNext(1);

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            if(animationModel.getPolylineList() != null && animationModel.getPolylineList().size() > 1)
                            {
                                if(animationModel.getIndex() < animationModel.getPolylineList().size() -2){
                                    //index++;
                                    animationModel.setIndex(animationModel.getIndex() + 1);
                                    //next = index+1;
                                    animationModel.setNext(animationModel.getIndex() + 1);
                                    //start = polylineList.get(index);
                                    animationModel.setStart(animationModel.getPolylineList().get(animationModel.getIndex()));
                                    //end = polylineList.get(next);
                                    animationModel.setEnd(animationModel.getPolylineList().get(animationModel.getNext()));
                                }

                                ValueAnimator valueAnimator = ValueAnimator.ofInt(0,1);
                                valueAnimator.setDuration(3000);
                                valueAnimator.setInterpolator(new LinearInterpolator());
                                valueAnimator.addUpdateListener(value -> {
                                    //v = value.getAnimatedFraction();
                                    animationModel.setV(value.getAnimatedFraction());
                                    //lat = v*end.latitude + (1-v)*start.latitude;
                                    animationModel.setLat(animationModel.getV()*animationModel.getEnd().latitude+
                                            (1-animationModel.getV())
                                    *animationModel.getStart().latitude);
                                    //lng = v*end.longitude + (1-v)*start.longitude;
                                    animationModel.setLng(animationModel.getV()*animationModel.getEnd().longitude+
                                            (1-animationModel.getV())
                                                    *animationModel.getStart().longitude);
                                    LatLng newPos = new LatLng(animationModel.getLat(),animationModel.getLng());
                                    currentMarker.setPosition(newPos);
                                    currentMarker.setAnchor(0.5f,0.5f);
                                    currentMarker.setRotation(Common.getBearing(animationModel.getStart(),newPos));
                                });

                                valueAnimator.start();
                                if(animationModel.getIndex() < animationModel.getPolylineList().size() -2) //Reach destination
                                    animationModel.getHandler().postDelayed(this,1500);
                                else if(animationModel.getIndex() < animationModel.getPolylineList().size() -1) //Done
                                {
                                    animationModel.setRun(false);
                                    Common.driverLocationSubscribe.put(key,animationModel);  //Update data

                                }
                            }
                        }
                    };

                    //Run handler
                    animationModel.getHandler().postDelayed(runnable,1500);

                } catch (Exception e)
                {
                    Snackbar.make(getView(),e.getMessage(),Snackbar.LENGTH_LONG).show();
                }
            })
            );
        }
    }
}
