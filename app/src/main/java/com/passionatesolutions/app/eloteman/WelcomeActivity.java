package com.passionatesolutions.app.eloteman;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.passionatesolutions.app.eloteman.login.EatCornLoginActivity;
import com.passionatesolutions.app.eloteman.login.VendorLoginActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: figure out how to build the brand by giving away EloteMan stickers to add to carts of vendors


public class WelcomeActivity extends AppCompatActivity {

    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    ImageView welcomeLogo;
    private Button vendorButton;
    private Button eatCornButton;
    private String TAG = "tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        vendorButton = findViewById(R.id.vendor_button);
        eatCornButton = findViewById(R.id.eat_corn_button);
        welcomeLogo = findViewById(R.id.welcome_screen_logo);


        // Vendor Button
        vendorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, VendorLoginActivity.class));
                //checkAndRequestPermissions();
                new AsyncCheckUserPermission().execute();
            }
        });

        // Eat Corn Button
        eatCornButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, EatCornLoginActivity.class));
                //checkAndRequestPermissions();
                new AsyncCheckUserPermission().execute();
            }
        });
    }


    // When user selects Yes to GPS locator, runs GPS code
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "Permission callback called-------");
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {

                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "location services permission granted");
                        // process the normal flow
                        Intent i = new Intent(WelcomeActivity.this, WelcomeActivity.class);
                        startActivity(i);
                        //else any one or both the permissions are not granted
                    } else {
                        Log.d(TAG, "Some permissions are not granted ask again ");
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                            showDialogOK(getString(R.string.show_dialog_ok_service_permissions_required),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    //BEFORE ASYNC TASK
                                                    //checkAndRequestPermissions();
                                                    new AsyncCheckUserPermission();
                                                    break;
                                                case DialogInterface.BUTTON_NEGATIVE:
                                                    finish();
                                                    break;
                                            }
                                        }
                                    });
                        } else {
                            explain(getString(R.string.explain_go_to_app_settings));
                        }
                    }
                }
            }
        }

    }

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.explain_positive_button, okListener)
                .setNegativeButton(R.string.explain_negative_button, okListener)
                .create()
                .show();
    }

    private void explain(String msg) {
        final android.support.v7.app.AlertDialog.Builder dialog = new android.support.v7.app.AlertDialog.Builder(this);
        dialog.setMessage(msg)
                .setPositiveButton(R.string.explain_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.passionatesolutions.zzyzj.eloteroman")));
                    }
                })
                .setNegativeButton(R.string.explain_negative_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        finish();
                    }
                });
        dialog.show();
    }

    // Sends users to Privacy Policy
    public void privacyPolicy(View view) {

        Intent goToPrivacyPolicy = new Intent();
        goToPrivacyPolicy.setAction(Intent.ACTION_VIEW);
        goToPrivacyPolicy.addCategory(Intent.CATEGORY_BROWSABLE);
        goToPrivacyPolicy.setData(Uri.parse("https://www.freeprivacypolicy.com/privacy/view/499e3bd96777ca0e8c17f25274969968"));
        startActivity(goToPrivacyPolicy);

    }

    // Code to check permission for GPS

    public class AsyncCheckUserPermission extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            int permissionLocation = ContextCompat.checkSelfPermission(WelcomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
            List<String> listPermissionsNeeded = new ArrayList<>();

            if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(WelcomeActivity.this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
                return false;
            }
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}