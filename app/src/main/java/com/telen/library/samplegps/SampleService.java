package com.telen.library.samplegps;

import android.app.NotificationManager;
import android.content.Context;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.telen.library.advanced_gps.services.AdvancedGPSService;

/**
 * Created by karim on 29/03/2018.
 */

public class SampleService extends AdvancedGPSService {

    @Override
    protected void onNewPosition(Location location) {

    }

    @Override
    protected void onLocationStarted() {
        NotificationCompat.Builder mBuilder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mBuilder = new NotificationCompat.Builder(this, getNotificationChannel().getId());

        }
        else {
            mBuilder = new NotificationCompat.Builder(this, "");
        }
        mBuilder.setSmallIcon(R.drawable.ic_location_on_black_24dp)
                .setContentTitle("Running")
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());
    }

    @Override
    protected void onLocationStopped() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);
    }
}
