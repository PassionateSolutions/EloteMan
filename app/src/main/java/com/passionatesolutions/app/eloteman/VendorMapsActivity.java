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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.passionatesolutions.app.eloteman.HelpDialog.VendorHelpDialog;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
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

public class VendorMapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private static final String TAG = "MapsActivity";
    private Location lastLocation;
    private Marker currentUserLocationMarker;
    private LocationManager locationManager;
    private com.google.android.gms.location.LocationListener listener;
    private Toast exitAppBackToast;
    private Toast logoutToast;
    private Button helpButton;
    private FirebaseAuth firebaseAuth;
    private TextView warningConnectionError;
    private DatabaseReference vendorOnlineIdReference;


    private static final int Request_User_Location_Code = 99;

    private long exitAppPressBack;
    private long logoutAppPressAgain;

    private AdView bannerAdView;


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

                    // Remove VendorOnline Child before logging out to prevent crash
                    removeVendorOnline();

                    removeAnonymousVendorUser();
                    FirebaseAuth.getInstance().signOut();
                    Intent logoutIntent = new Intent(VendorMapsActivity.this, WelcomeActivity.class);
                    startActivity(logoutIntent);
                    finish();
                } else {
                    logoutToast = Toast.makeText(getBaseContext(), R.string.press_button_again_to_logout, Toast.LENGTH_LONG);
                    logoutToast.show();
                }

                logoutAppPressAgain = System.currentTimeMillis();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_maps);

        firebaseAuth = FirebaseAuth.getInstance();

        warningConnectionError = findViewById(R.id.warning_error_text_maps);


        vendorOnlineIdReference = FirebaseDatabase.getInstance().getReference().child(("VendorOnline"));

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

        helpButton = findViewById(R.id.vendorHelpButton);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            new AsyncCheckUserPermission().execute();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapVendor);
        mapFragment.getMapAsync(this);

        checkLocation();

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in current user location and move the camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
            // Call current location of user
            mMap.setMyLocationEnabled(true);
        }

    }

    // Help Button
    public void helpButton(View view) {
        helpDialogWindow();
    }

    // Dialog Help Window
    private void helpDialogWindow() {

        VendorHelpDialog helpDialog = new VendorHelpDialog();
        helpDialog.show(getSupportFragmentManager(), "Vendor Help Dialog");

    }


    // ASYNC Task to check if User allows GPS Permission, if not, ask for it again

    public class AsyncCheckUserPermission extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            if (ContextCompat.checkSelfPermission(VendorMapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(VendorMapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(VendorMapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Request_User_Location_Code);
                } else {
                    ActivityCompat.requestPermissions(VendorMapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Request_User_Location_Code);
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
                        mMap.setMyLocationEnabled(true);
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

        // When Vendor is not on this screen activity it will remove their data from Firebase
        // allowing them to come back and appear online again without needing to logout.
        removeVendorOnline();

        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }


    @Override
    public void onLocationChanged(Location location) {

        lastLocation = location;

        if (currentUserLocationMarker != null) {
            currentUserLocationMarker.remove();
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.corn_icon));

        currentUserLocationMarker = mMap.addMarker(markerOptions);

        float zoom = 17.0f;

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Adds VendorOnline Child to Firebase when Vendor is On This Activity
        addVendorOnline();
        isOnline();
        checkIfOnlineVendorWasCreated();

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

    // Alert Window to show if the User does not have Permission for Location enabled

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

    // Checks to see if GPS is Enabled

    private boolean isLocationEnabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // Prevent user from going to unwanted Activities when back button is pressed, instead user logout

    @Override
    public void onBackPressed() {
        if (exitAppPressBack + 2000 > System.currentTimeMillis()) {
            exitAppBackToast.cancel();
            super.onBackPressed();

            // Remove VendorOnline Child before logging out to prevent crash
            removeVendorOnline();
            removeAnonymousVendorUser();
            FirebaseAuth.getInstance().signOut();
            Intent logoutIntent = new Intent(VendorMapsActivity.this, WelcomeActivity.class);
            startActivity(logoutIntent);
            finish();
        } else {
            exitAppBackToast = Toast.makeText(getBaseContext(), R.string.press_button_again_to_logout, Toast.LENGTH_LONG);
            exitAppBackToast.show();
        }

        exitAppPressBack = System.currentTimeMillis();
    }

    // When user swipe closes app it logs them out

    @Override
    protected void onDestroy() {
        removeAnonymousVendorUser();
        super.onDestroy();
        FirebaseAuth.getInstance().signOut();
        finish();
    }

    // Code to store Vendor data when a Vendor is Online in Firebase
    // Creates new Child within Firebase Database "VendorOnline" when a Vendor is Online
    // Uses Geofire to update location and update Vendor Online Data in Firebase

    private void addVendorOnline(){

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

            String vendorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference vendorIdReference = FirebaseDatabase.getInstance().getReference("VendorOnline");
            GeoFire vendorGeoFire = new GeoFire(vendorIdReference);
            vendorGeoFire.setLocation(vendorId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
        }

    }

    // Removes Vendor from VendorOnline Child in Firebase when they leave the screen and/or logout.

    private void removeVendorOnline() {

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

            String vendorId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference vendorIdReference = FirebaseDatabase.getInstance().getReference("VendorOnline");
            GeoFire vendorGeoFire = new GeoFire(vendorIdReference);
            vendorGeoFire.removeLocation(vendorId);
        }
    }


    // Removes Anonymous "Vendor" user from Auth and Realtime Database to reduce memory footprint when they logout
    private void removeAnonymousVendorUser() {


        DatabaseReference vendorIdReference = FirebaseDatabase.getInstance().getReference("Vendor");
        vendorIdReference.removeValue();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            user.delete();
        }

    }

    // Check to see if vendor created a "VendorOnline" child, if not, display warning text.
    private void checkIfOnlineVendorWasCreated() {

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

        final String user = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference vendorIdReference = FirebaseDatabase.getInstance().getReference("VendorOnline");
        GeoFire vendorGeoFire = new GeoFire(vendorIdReference);

        vendorGeoFire.getLocation(user, new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                if (key.compareTo(user) == 0) {
                    if (location == null) {

                        warningConnectionError.setVisibility(View.VISIBLE);

                    }
                } else warningConnectionError.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
    }


}