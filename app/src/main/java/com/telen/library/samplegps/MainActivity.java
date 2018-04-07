package com.telen.library.samplegps;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.telen.library.advanced_gps.Constants;
import com.telen.library.advanced_gps.manager.GPSManager;
import com.telen.library.advanced_gps.models.GPSCallback;

public class MainActivity extends AppCompatActivity implements GPSCallback {

    private TextView outputGetLastKnownLocation;
    private TextView outputLocationChanged;
    private TextView mBindingStatus;
    private TextView mLocationsStatus;

    private GPSManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = new GPSManager(this);
        manager.addCallback(this);

        final Button btnGetLastKnownLocation = findViewById(R.id.btn_last_known_location);
        outputGetLastKnownLocation = findViewById(R.id.output_get_last_known_location);
        final Button btnStartLocation = findViewById(R.id.btn_start_location);
        outputLocationChanged = findViewById(R.id.output_location_changed);

        btnGetLastKnownLocation.setOnClickListener(view -> manager.getLastKnownLocation());
        btnStartLocation.setOnClickListener(view -> manager.startLocationListener());

        final Button btnStopLocationUpdates = findViewById(R.id.btn_stop_location);
        btnStopLocationUpdates.setOnClickListener(view -> manager.stopLocationListener());

        mBindingStatus = findViewById(R.id.binding_status);
        mLocationsStatus = findViewById(R.id.location_status);

        final Button btnBind = findViewById(R.id.btn_bind);
        btnBind.setOnClickListener(view -> manager.bindService());
        final Button btnUnbind = findViewById(R.id.btn_unbind);
        btnUnbind.setOnClickListener(view -> manager.unbindService());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(manager!=null) {
            if (manager.isBound())
                mBindingStatus.setBackgroundColor(Color.GREEN);
            else
                mBindingStatus.setBackgroundColor(Color.RED);
        }

    }

    @Override
    protected void onDestroy() {
        if(manager!=null)
            manager.destroy();
        super.onDestroy();
    }

    @Override
    public void onLocationChangeListener(Location location) {
        runOnUiThread(() -> outputLocationChanged.setText(String.valueOf(location)));
    }

    @Override
    public void onLastKnownLocation(@Nullable Location location) {
        if(location!=null) {
            runOnUiThread(() -> outputGetLastKnownLocation.setText(location.toString()));
        }
        else
            runOnUiThread(() -> outputGetLastKnownLocation.setText("No location found"));
    }

    @Override
    public void onLastKnownLocationFailed(String exception) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, exception, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onNeedLocationPermissions(int code) {
        ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION
        }, code);
    }

    @Override
    public void onLocationTurnedOff(int code, PendingIntent intent) {
        try {
            startIntentSenderForResult(intent.getIntentSender(), code, null,0,0,0);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
            Log.e(MainActivity.class.getSimpleName(),"onLocationTurnedOff");
        }
    }

    @Override
    public void onLocationSuccessfullyStarted() {
        runOnUiThread(() -> mLocationsStatus.setBackgroundColor(Color.GREEN));
    }

    @Override
    public void onLocationSuccessfullyStopped() {
        runOnUiThread(() -> {
            mLocationsStatus.setBackgroundColor(Color.RED);
            outputLocationChanged.setText("Disabled");
        });
    }

    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            mBindingStatus.setBackgroundColor(Color.GREEN);
            Toast.makeText(MainActivity.this, "Bound to the gps service", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, "Unbound to the gps service", Toast.LENGTH_SHORT).show();
            mBindingStatus.setBackgroundColor(Color.RED);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.REQUEST_GET_LAST_KNOWN_LOCATION:
                for (int result : grantResults) {
                    if(result == PackageManager.PERMISSION_DENIED) {
                        return;
                    }
                }
                manager.getLastKnownLocation();
                break;
            case Constants.REQUEST_START_LOCATION:
                for (int result : grantResults) {
                    if(result == PackageManager.PERMISSION_DENIED) {
                        return;
                    }
                }
                manager.startLocationListener();
                break;
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_START_LOCATION:
                if(resultCode== Activity.RESULT_OK) {
                    manager.startLocationListener();
                }
                break;
            default:
        }
    }
}
