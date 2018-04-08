package com.telen.library.advanced_gps.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.telen.library.advanced_gps.Constants;
import com.telen.library.advanced_gps.GoogleFusedInstance;
import com.telen.library.advanced_gps.R;
import com.telen.library.IFollomiGPS;
import com.telen.library.IFollomiGPSListener;

/**
 * @author superman
 */
public abstract class AdvancedGPSService extends Service {

    private static final String TAG = "AdvancedGPSService";
    private final FollomiLocationManager iGPS = new FollomiLocationManager();
    private IFollomiGPSListener iListener;
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if(locationResult==null)
                return;
            for (Location location : locationResult.getLocations()) {
                onNewPosition(location);
                if(iListener!=null) {
                    try {
                        iListener.onLocationChanged(location);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        Log.e(TAG, "", e);
                    }
                }
            }
        }
    };

    abstract protected void onNewPosition(@Nullable Location location);

    abstract protected void onLocationStarted();

    abstract protected void onLocationStopped();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            //TODO
            NotificationChannel channel = getNotificationChannel();
            notificationManager.createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, channel.getId())
                    .setSmallIcon(R.drawable.ic_location_on_black_24dp)
                    .build();
            startForeground(startId, notification);
        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"onUnbind");
        iListener = null;
        return super.onUnbind(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iGPS;
    }

    public class FollomiLocationManager extends IFollomiGPS.Stub {

        @Override
        public void startLocationListener(int powerPlan) throws RemoteException {
            Log.d(TAG,"startLocationListener powerPlan="+powerPlan);
            startLocationsUpdates(powerPlan);
        }

        @Override
        public void stopLocationListener() throws RemoteException {
            Log.d(TAG,"startLocationListener");
            stopLocationUpdates();
        }

        @Override
        public void getLastKnownLocation() throws RemoteException, SecurityException {
            Log.d(TAG, "getLastKnowLocation");
            getLastKnownLocationInternal();
        }

        @Override
        public void registerListener(IFollomiGPSListener listener) throws RemoteException {
            Log.d(TAG, "registerListener");
            iListener = listener;
        }

        @Override
        public void unregisterListener() throws RemoteException {
            Log.d(TAG, "unregisterListener");
            iListener = null;
        }
    }

    private void stopLocationUpdates() {
        GoogleFusedInstance.getInstance(AdvancedGPSService.this).getFusedLocationProviderClient().removeLocationUpdates(mLocationCallback)
                .addOnSuccessListener(aVoid -> {
                    onLocationStopped();
                    if(iListener!=null) {
                        try {
                            iListener.onLocationSuccessfullyStopped();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.e(TAG,"",e);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG,"",e));
    }

    @SuppressLint("MissingPermission")
    private void startLocationsUpdates(int powerPlan) {
        Log.d(TAG,"startLocationUpdates");
        final LocationRequest locationRequest = createLocationRequest(powerPlan);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
                .addOnSuccessListener(locationSettingsResponse -> {
                    if(!checkPermissions(Constants.REQUEST_START_LOCATION)) {
                        return;
                    }
                    GoogleFusedInstance.getInstance(AdvancedGPSService.this)
                            .getFusedLocationProviderClient()
                            .requestLocationUpdates(locationRequest, mLocationCallback, null)
                            .addOnSuccessListener(aVoid -> {
                                onLocationStarted();
                                if(iListener!=null) {
                                    try {
                                        iListener.onLocationSuccessfullyStarted();
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                        Log.e(TAG,"",e);
                                    }
                                }
                            })
                            .addOnFailureListener(e -> Log.e(TAG,"",e));
                })
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        if(iListener!=null) {
                            try {
                                iListener.onLocationTurnedOff(Constants.REQUEST_START_LOCATION, resolvable.getResolution());
                            } catch (RemoteException e1) {
                                e1.printStackTrace();
                                Log.e(TAG,"",e);
                            }
                        }
                    }
                });
    }

    private boolean checkPermissions(int code) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(iListener!=null) {
                try {
                    iListener.onNeedLocationPermissions(code);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e(TAG, "", e);
                }
            }
            return false;
        }
        return true;
    }

    private LocationRequest createLocationRequest(int powerPlan) {
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(powerPlan);
        switch (powerPlan) {
            case LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY:
                mLocationRequest.setFastestInterval(60000);
                mLocationRequest.setInterval(3600000);
                break;
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                mLocationRequest.setInterval(5000);
                mLocationRequest.setFastestInterval(3000);
                break;
            case LocationRequest.PRIORITY_LOW_POWER:
                mLocationRequest.setFastestInterval(60000);
                mLocationRequest.setInterval(3600000);
                break;
            case LocationRequest.PRIORITY_NO_POWER:
                break;
        }

        Log.d(TAG,"powerPlan="+powerPlan);
        return mLocationRequest;
    }

    @SuppressLint("MissingPermission")
    private void getLastKnownLocationInternal() {
        if(!checkPermissions(Constants.REQUEST_GET_LAST_KNOWN_LOCATION)) {
            return;
        }
        GoogleFusedInstance.getInstance(this).getFusedLocationProviderClient().getLastLocation().addOnSuccessListener(location -> {
            if (iListener != null) {
                try {
                    iListener.onLastKnownLocation(location);
                } catch (RemoteException e) {
                    Log.e(TAG, "", e);
                    e.printStackTrace();
                }
            }
        }).addOnFailureListener(e -> {
            if (iListener != null) {
                try {
                    iListener.onLastKnownLocationFailed(e.getMessage());
                } catch (RemoteException error) {
                    Log.e(TAG, "", e);
                    error.printStackTrace();
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.O)
    protected NotificationChannel getNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = getString(R.string.gps_notification_channel_id);
            NotificationChannel channel = new NotificationChannel(channelId, "Locations listener", NotificationManager.IMPORTANCE_DEFAULT);
            return channel;
        }
        else
            return null;
    }
}