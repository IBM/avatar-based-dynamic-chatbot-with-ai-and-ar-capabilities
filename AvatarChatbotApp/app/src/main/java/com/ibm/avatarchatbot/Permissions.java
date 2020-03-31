package com.ibm.avatarchatbot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.worklight.wlclient.api.WLAccessTokenListener;
import com.worklight.wlclient.api.WLAuthorizationManager;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.auth.AccessToken;

import java.util.ArrayList;
import java.util.List;

public class Permissions extends AppCompatActivity {

    // List of all permissions for the app
    String[] appPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
    };

    private static final int PERMISSIONS_REQUEST_CODE = 1240;

    public boolean mfpflag = false;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        progressBar = findViewById(R.id.progressBar2);

        if(checkAndRequestPermissions()){
            progressBar.setProgress(34, true);
            if(checkInternetConnection()){
                progressBar.setProgress(68, true);
                pingMfpServer();
            }
        }


    }

    private void LaunchActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    // Check and request required permissions

    private boolean checkAndRequestPermissions() {
        // Check which permissions are granted
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm: appPermissions)
        {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionsNeeded.add(perm);
            }
        }

        // Ask for non-granted permissions
        if (!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSIONS_REQUEST_CODE);

            return false;
        }

        // App has all permissions
        return true;
    }

    public boolean checkInternetConnection() {
        // get Connectivity Manager object to check connection
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        // Check for network connections
        if (isConnected){
            return true;
        }
        else {
            Toast.makeText(this, " No Internet Connection available ", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    // Connect To Mobile Foundation Server

    public boolean pingMfpServer() {

        WLClient client = WLClient.createInstance(this);

        WLAuthorizationManager.getInstance().obtainAccessToken(null, new WLAccessTokenListener() {
            @Override
            public void onSuccess(AccessToken token) {
                Log.d("Received the following access token value: ", String.valueOf(token));
                runOnUiThread(() -> {
                    progressBar.setProgress(100, true);
                    Toast toast = Toast.makeText(getApplicationContext(), "Connected to MobileFirst Server!", Toast.LENGTH_LONG);
                    toast.show();
                    mfpflag = true;
                    LaunchActivity();
                });
            }

            @Override
            public void onFailure(WLFailResponse wlFailResponse) {
                Log.d("Did not receive an access token from server: ", wlFailResponse.getErrorMsg());
                runOnUiThread(() -> {
                    Toast toast = Toast.makeText(getApplicationContext(), "Bummer... : Failed to connect to MobileFirst Server", Toast.LENGTH_LONG);
                    toast.show();
                    mfpflag = false;
                });
            }
        });

        return mfpflag;
    }
}
