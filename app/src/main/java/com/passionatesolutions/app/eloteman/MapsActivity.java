package com.passionatesolutions.app.eloteman;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.passionatesolutions.app.eloteman.HelpDialog.CustomerHelpDialog;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap eaterGoogleMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private static final String TAG = "MapsActivity";
    private Location lastLocation;
    private Marker currentUserLocationMarker;
    private LocationManager locationManager;
    private com.google.android.gms.location.LocationListener listener;
    private Toast exitAppBackToast;
    private Toast logoutToast;
    // Boolean to allow zoom to run only once when this activity first starts
    boolean cameraZoomToLocation = false;

    private static final int Request_User_Location_Code = 99;

    private long exitAppPressBack;
    private long logoutAppPressAgain;

    private AdView bannerAdView;
    private TextView warningConnectionError;


    // Display Logout Menu Item Button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logout_menu, menu);

        return true;
    }

    //Logout Menu Option Button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // If Logout Button is clicked within 2 Seconds log user out
            case R.id.logoutMenuOption:
                if (logoutAppPressAgain + 2000 > System.currentTimeMillis()) {
                    logoutToast.cancel();
                    removeAnonymousEater();
                    FirebaseAuth.getInstance().signOut();
                    Intent logoutIntent = new Intent(MapsActivity.this, WelcomeActivity.class);
                    startActivity(logoutIntent);
                    finish();
                } else {
                    logoutToast = Toast.makeText(getBaseContext(), R.string.press_button_again_to_logout, Toast.LENGTH_SHORT);
                    logoutToast.show();
                }

                logoutAppPressAgain = System.currentTimeMillis();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        warningConnectionError = findViewById(R.id.warning_error_text_maps);

        // Display & Connect AdView Banner
        // Replace testAppId with realAppId also in Manifest and use realAdId in activity_maps.XML
        // for final release
        MobileAds.initialize(this, getString(R.string.realAppId));
        bannerAdView = findViewById(R.id.banner_ad);

        AdRequest request = new AdRequest.Builder()
//                .addTestDevice("SomeString")  // An passionatesolutions device ID, remove before final release
                .build();
        bannerAdView.loadAd(request);

        bannerAdView.setAdListener(new AdListener(){

            @Override
            public void onAdLoaded() {
                bannerAdView.isShown();
                super.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {

            }
        });

        // Display App Toolbar
        android.support.v7.widget.Toolbar apptoolbar = findViewById(R.id.appToolBar);
        setSupportActionBar(apptoolbar);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Before Async Task
            //checkUserLocationPermission();
            new AsyncCheckUserPermission().execute();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        checkLocation();

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.eaterGoogleMap = googleMap;

        // Add a marker in current user location and move the camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
            // Call current location of user
            this.eaterGoogleMap.setMyLocationEnabled(true);
        }

    }

    // Help Button
    public void helpButton(View view) {
        helpDialogWindow();
    }

    // Dialog Help Window
    private void helpDialogWindow() {

        CustomerHelpDialog helpDialog = new CustomerHelpDialog();
        helpDialog.show(getSupportFragmentManager(), "Vendor Help Dialog");

    }


    // ASYNC Task to check if User allows GPS Permission, if not, ask for it again

    public class AsyncCheckUserPermission extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Request_User_Location_Code);
                } else {
                    ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Request_User_Location_Code);
                }
                return false;
            } else {
                return true;
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Request_User_Location_Code:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (googleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        eaterGoogleMap.setMyLocationEnabled(true);
                    }

                } else {
                    Toast.makeText(this, R.string.on_request_permission_gps_not_located, Toast.LENGTH_LONG).show();
                }
                return;
        }
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {


            // Constantly Update User location every 1.1 seconds!
            locationRequest = new LocationRequest();
            locationRequest.setInterval(1100);
            locationRequest.setFastestInterval(1100);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
                return;
            }

            Log.d("reque", "check here");
        }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }

        // When the user leaves this activity, it sets cameraZoomToLocation to False
        // so when they return back to the activity it will zoom to their current location
        // if they had moved or if they had scrolled through the map beforehand.
        if(cameraZoomToLocation = true){
            cameraZoomToLocation = false;
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (currentUserLocationMarker != null) {
            currentUserLocationMarker.remove();
        }

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(getString(R.string.user_current_location_marker_title));
        markerOptions.visible(false);

        currentUserLocationMarker = eaterGoogleMap.addMarker(markerOptions);

        float zoom = 17.0f;

        eaterGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        eaterGoogleMap.getUiSettings().setAllGesturesEnabled(true);
        eaterGoogleMap.getUiSettings().setZoomControlsEnabled(true);

        // If cameraZoomToLocation is False, sets it to true in order to zoom in only once when activity first starts
        if(!cameraZoomToLocation){
            eaterGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
            cameraZoomToLocation = true;
        }

        // Begin to locate all online Vendors to display on map
        locateOnlineVendors();
        isOnline();

    }

    // Check to see if Internet connection is available
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            warningConnectionError.setVisibility(View.INVISIBLE);
            return true;
        } else {
            warningConnectionError.setVisibility(View.VISIBLE);
            return false;
        }
    }

    private boolean checkLocation() {
        if (!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.show_alert_title_enable_location)
                .setMessage(getString(R.string.show_alert_location_settings_off_1) +
                        getString(R.string.show_alert_location_settings_off_2))
                .setPositiveButton(R.string.show_alert_positive_button_location_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton(R.string.explain_negative_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // Prevent user from going to unwanted Activities when back button is pressed, instead logout
    @Override
    public void onBackPressed() {
        if (exitAppPressBack + 2000 > System.currentTimeMillis()) {
            exitAppBackToast.cancel();
            super.onBackPressed();
            removeAnonymousEater();
            FirebaseAuth.getInstance().signOut();
            Intent logoutIntent = new Intent(MapsActivity.this, WelcomeActivity.class);
            startActivity(logoutIntent);
            finish();
        } else {
            exitAppBackToast = Toast.makeText(getBaseContext(), R.string.press_button_again_to_logout, Toast.LENGTH_SHORT);
            exitAppBackToast.show();
        }

        exitAppPressBack = System.currentTimeMillis();
    }

    // When User Swipe Closes App, it will log them out

    @Override
    protected void onDestroy() {
        removeAnonymousEater();
        super.onDestroy();
        FirebaseAuth.getInstance().signOut();
        finish();
    }


    // ALL THE CODE TO DISPLAY ONLINE VENDORS

    // Creates a list to store all the UID's for each Vendor that is Online, removes them from the list
    // when they are no longer on their VendorMapsActivity
    List<Marker> markerVendorsOnlineList = new ArrayList<>();


    private void locateOnlineVendors() {


        // First locates the Vendor Longitude and Latitude from Firebase
        DatabaseReference vendorsOnlineReference = FirebaseDatabase.getInstance().getReference().child(("VendorOnline"));

        GeoFire findOnlineVendorGeofire = new GeoFire(vendorsOnlineReference);

        // Does the search within a 50 mile radius of the person who wants to eat corn.

        GeoQuery findOnlineVendorGeoQuery = findOnlineVendorGeofire.queryAtLocation(new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), 80.4672);

        findOnlineVendorGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                // Go through the list of available OnlineVendors in the Firebase database child "VendorsOnline"
                // Making sure that no two keys passed in are the same to create duplicates
                for (Marker markerVendorsOnlineListIterator : markerVendorsOnlineList){

                    if (markerVendorsOnlineListIterator.getTag().equals(key))

                        return;
                }

                // Code to create the location markers for each Vendor that is online and add each marker to the
                // markerVendorsOnlineList
                LatLng vendorLocation = new LatLng(location.latitude, location.longitude);

                Marker onlineVendorMarker = eaterGoogleMap.addMarker(new MarkerOptions()
                        .position(vendorLocation)
                        .title(getString(R.string.onlineVendorMarker_title))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.corn_icon)));

                onlineVendorMarker.setTag(key);
                markerVendorsOnlineList.add(onlineVendorMarker);

            }

            @Override
            public void onKeyExited(String key) {

                // Removes marker from markerVendorsOnlineList and markerVendorsOnlineListIterator when Vendor is not online
                for (Marker markerVendorsOnlineListIterator : markerVendorsOnlineList){

                    if (markerVendorsOnlineListIterator.getTag().equals(key)){

                        markerVendorsOnlineListIterator.remove();
                        markerVendorsOnlineList.remove(markerVendorsOnlineListIterator);

                        return;
                    }
                }

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

                // Updates Online Vendors position when they move
                for (Marker markerVendorsOnlineListIterator : markerVendorsOnlineList){
                    if (markerVendorsOnlineListIterator.getTag().equals(key)) {

                        markerVendorsOnlineListIterator.setPosition(new LatLng(location.latitude, location.longitude));

                    }
                }

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    // Removes Anonymous Eat Corn user from Database to reduce memory footprint
    private void removeAnonymousEater() {

        DatabaseReference eaterUidReference = FirebaseDatabase.getInstance().getReference("Eater");
        eaterUidReference.removeValue();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            user.delete();
        }
    }
}