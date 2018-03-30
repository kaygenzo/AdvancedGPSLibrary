package com.telen.library.advanced_gps;

import android.content.Context;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

/**
 * Created by karim on 25/03/2018.
 */
public class GoogleFusedInstance {
    private static GoogleFusedInstance mInstance;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    public static GoogleFusedInstance getInstance(Context context) {
        if(mInstance==null)
            mInstance = new GoogleFusedInstance(context);
        return mInstance;
    }

    private GoogleFusedInstance(Context context) {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public FusedLocationProviderClient getFusedLocationProviderClient() {
        return mFusedLocationProviderClient;
    }
}
