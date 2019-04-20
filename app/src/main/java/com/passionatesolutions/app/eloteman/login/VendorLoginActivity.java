package com.passionatesolutions.app.eloteman.login;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.passionatesolutions.app.eloteman.VendorMapsActivity;
import com.passionatesolutions.app.eloteman.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class VendorLoginActivity extends AppCompatActivity {

    private ProgressBar circProgressBar;

    private FirebaseAuth firebaseAuth;

    private Button startNowButton;


    private TextView loadingMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vendor_login);

        startNowButton = findViewById(R.id.vendor_begin);

        circProgressBar = findViewById(R.id.progress_circular_bar);

        firebaseAuth = FirebaseAuth.getInstance();

        loadingMessage = findViewById(R.id.loading_message);
    }

    // Allows users to use the app anonymously with any registration for ease of access and privacy
    public void sellCornAnonymously(View view) {

        circProgressBar.setVisibility(View.VISIBLE);
        startNowButton.setVisibility(View.INVISIBLE);
        loadingMessage.setVisibility(View.VISIBLE);

            firebaseAuth.signInAnonymously().addOnSuccessListener(VendorLoginActivity.this, new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {

                    loadingMessage.setVisibility(View.INVISIBLE);
                    Toast.makeText(VendorLoginActivity.this, R.string.success_login, Toast.LENGTH_LONG).show();
                    String client_id = firebaseAuth.getCurrentUser().getUid();
                    DatabaseReference client_db = FirebaseDatabase.getInstance().getReference().child("Vendor").child(client_id);
                    client_db.setValue(true);
                    circProgressBar.setVisibility(View.INVISIBLE);
                    startNowButton.setVisibility(View.INVISIBLE);

                    // Sends anonymous user to VendorMapsActivity
                    startActivity(new Intent(VendorLoginActivity.this, VendorMapsActivity.class));

                }
            })
                    .addOnFailureListener(VendorLoginActivity.this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    loadingMessage.setVisibility(View.INVISIBLE);
                    Toast.makeText(VendorLoginActivity.this, R.string.login_failed_toast, Toast.LENGTH_LONG).show();
                    circProgressBar.setVisibility(View.INVISIBLE);
                    startNowButton.setVisibility(View.VISIBLE);

                }
            });


    }

}