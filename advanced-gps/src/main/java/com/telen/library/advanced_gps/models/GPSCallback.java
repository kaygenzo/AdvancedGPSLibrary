package com.telen.library.advanced_gps.models;

import android.app.PendingIntent;
import android.location.Location;
import android.support.annotation.Nullable;

/**
 * Created by karim on 26/03/2018.
 */

public interface GPSCallback {
    void onLocationChangeListener(Location location);
    void onLastKnownLocation(@Nullable Location location);
    void onLastKnownLocationFailed(String exception);
    void onNeedLocationPermissions(int code);
    void onLocationTurnedOff(int code, PendingIntent intent);
    void onLocationSuccessfullyStarted();
    void onLocationSuccessfullyStopped();
    void onConnected();
    void onDisconnected();
}
