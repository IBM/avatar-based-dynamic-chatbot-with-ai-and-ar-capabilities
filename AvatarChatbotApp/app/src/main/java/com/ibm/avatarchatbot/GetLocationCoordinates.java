package com.ibm.avatarchatbot;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

public class GetLocationCoordinates implements LocationListener {
    public String queryparams;
    @Override
    public void onLocationChanged(Location location) {
        double latitude = 0;
        double longitude = 0;
        if(location!= null){
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        this.queryparams = "?lat="+ latitude+ "&lon="+ longitude;
        Log.d("LOCATION DETAILS ->", queryparams);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
