package com.telen.library.advanced_gps.manager;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.telen.library.advanced_gps.Constants;
import com.telen.library.advanced_gps.R;
import com.telen.library.advanced_gps.models.GPSCallback;
import com.telen.library.advanced_gps.models.QueueModel;
import com.telen.library.IFollomiGPS;
import com.telen.library.IFollomiGPSListener;
import com.telen.library.advanced_gps.services.AdvancedGPSService;

import java.util.ArrayList;
import java.util.List;

/**
 *@author superman
 */

public class GPSManager {

	private static final boolean DEBUG = true;
	private static final String TAG = GPSManager.class.getName();

	static final Object callbacksLock = new Object();

	private final Context context;

	private ConnectionManager iCManager=new ConnectionManager();

	private ArrayList<GPSCallback> callbacks=new ArrayList<>();
	private ArrayList<QueueModel.QUEUE_ACTION> waitingQueue=new ArrayList<>();

	private IFollomiGPS igps;

	private final Intent intent;

	private class ConnectionManager extends IFollomiGPSListener.Stub implements ServiceConnection
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if(DEBUG) Log.d(TAG,"onServiceConnected");
			igps = IFollomiGPS.Stub.asInterface(service);
			try {
				igps.registerListener(iCManager);
			} catch (RemoteException e) {
				Log.e(TAG,"",e);
			}
			for (QueueModel.QUEUE_ACTION action : waitingQueue) {
				switch (action) {
					case LAUNCH_LOCATION_LISTENER:
						startLocationListener();
						break;
					case STOP_LOCATION_LISTENER:
						stopLocationListener();
						break;
					case LAST_KNOW_LOCATION:
						getLastKnownLocation();
						break;
				}
			}
			waitingQueue.clear();
			synchronized (callbacksLock) {
				for (GPSCallback callback : callbacks) {
					callback.onConnected();
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			if(DEBUG) Log.d(TAG,"onServiceDisconnected");
			igps=null;
			synchronized (callbacksLock) {
				for (GPSCallback callback : callbacks) {
					callback.onDisconnected();
				}
			}
		}

		@Override
		public void onBindingDied(ComponentName name) {
			if(DEBUG) Log.d(TAG,"onBindingDied");
		}

		@Override
		public void onLocationChanged(Location location) throws RemoteException {
			if(DEBUG) Log.d(TAG,"GPSManager: onLocationChanged from listener callback.size="+callbacks.size());
			synchronized (callbacksLock) {
				for (GPSCallback callback : callbacks) {
					callback.onLocationChangeListener(location);
				}
			}
		}

		@Override
		public void onLastKnownLocation(Location location) throws RemoteException {
			if(DEBUG) Log.d(TAG,"GPSManager: onLastKnowLocation from listener");
			synchronized (callbacksLock) {
				for (GPSCallback callback : callbacks) {
					callback.onLastKnownLocation(location);
				}
			}
		}

		@Override
		public void onLastKnownLocationFailed(String exception) throws RemoteException {
			if(DEBUG) Log.d(TAG,"GPSManager: onLastKnownLocationFailed from listener e="+exception);
			synchronized (callbacksLock) {
				for (GPSCallback callback : callbacks) {
					callback.onLastKnownLocationFailed(exception);
				}
			}
		}

		@Override
		public void onNeedLocationPermissions(int code) throws RemoteException {
			if(DEBUG) Log.e(TAG,"GPSManager: onNeedLocationPermissions");
			synchronized (callbacksLock) {
				for (GPSCallback callback : callbacks) {
					callback.onNeedLocationPermissions(code);
				}
			}
		}

		@Override
		public void onLocationTurnedOff(int code, PendingIntent resolvingIntent) throws RemoteException {
			if(DEBUG) Log.e(TAG,"GPSManager: onLocationTurnedOff");
			synchronized (callbacksLock) {
				for (GPSCallback callback : callbacks) {
					callback.onLocationTurnedOff(code, resolvingIntent);
				}
			}
		}

		@Override
		public void onLocationSuccessfullyStarted() throws RemoteException {
			if(DEBUG) Log.d(TAG,"GPSManager: onLocationSuccessfullyStarted");

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				GPSManager.this.context.startForegroundService(intent);
			else
				GPSManager.this.context.startService(intent);

			synchronized (callbacksLock) {
				for (GPSCallback callback : callbacks) {
					callback.onLocationSuccessfullyStarted();
				}
			}
		}

		@Override
		public void onLocationSuccessfullyStopped() throws RemoteException {
			GPSManager.this.context.stopService(intent);
			if(DEBUG) Log.d(TAG,"GPSManager: onLocationSuccessfullyStopped");
			synchronized (callbacksLock) {
				for (GPSCallback callback : callbacks) {
					callback.onLocationSuccessfullyStopped();
				}
			}
		}
	}

	public GPSManager(Context context) throws IllegalStateException {
		this.context = context;
		Log.d(TAG,"GPSManager()");
		Intent queryIntent = new Intent(Constants.ACTION_GPS_SERVICE);

		PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> services = packageManager.queryIntentServices(queryIntent, PackageManager.GET_META_DATA);
		if(services!=null) {
			for (ResolveInfo resolveInfo : services) {
				Log.d(TAG, "service=" + resolveInfo);
				ServiceInfo serviceInfo = resolveInfo.serviceInfo;
				if(serviceInfo!=null) {
					ComponentName cn = new ComponentName(serviceInfo.packageName, serviceInfo.name);
					intent = new Intent(Constants.ACTION_GPS_SERVICE);
					intent.setComponent(cn);
					this.context.bindService(intent, iCManager, Context.BIND_AUTO_CREATE);
					return;
				}
			}
		}
		throw new IllegalArgumentException("No gps service found in the manifest.");
	}

	public void destroy() {
		synchronized (callbacksLock) {
			callbacks.clear();
		}
		waitingQueue.clear();
		try {
			context.unbindService(iCManager);
			igps=null;
		}
		catch(IllegalArgumentException e) {
			Log.e(TAG,"",e);
		}
	}

	public void startLocationListener()
	{
		if(igps!=null)
		{
			if(DEBUG)
				Log.d(TAG,"launchLocationListener");
			try {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				int gps_accuracy = Integer.parseInt(prefs.getString(context.getString(R.string.key_gps_accuracy),context.getString(R.string.default_gps_sync)));
				igps.startLocationListener(gps_accuracy);
			} catch (RemoteException e) {
				Log.e(TAG,"",e);
			}
		}
		else
			waitingQueue.add(QueueModel.QUEUE_ACTION.LAUNCH_LOCATION_LISTENER);
	}

	public void stopLocationListener()
	{
		if(igps!=null)
		{
			if(DEBUG)
				Log.d(TAG,"stopLocationListener");
			try {
				igps.stopLocationListener();
			} catch (RemoteException e) {
				Log.e(TAG,"",e);
			}
		}
		else
			waitingQueue.add(QueueModel.QUEUE_ACTION.STOP_LOCATION_LISTENER);
	}

	public void getLastKnownLocation() {
		Log.d(TAG,"getLastKnowLocation");
		if(igps!=null)
		{
			try {
				igps.getLastKnownLocation();
			} catch (RemoteException e) {
				Log.e(TAG,"",e);
			}
		}
		else
			waitingQueue.add(QueueModel.QUEUE_ACTION.LAST_KNOW_LOCATION);
	}

	public void addCallback(GPSCallback callback) {
		synchronized (callbacksLock) {
			callbacks.add(callback);
		}
		Log.d(TAG,"addCallback: New size="+callbacks.size());
	}

	public void deleteCallback(GPSCallback callback) {
		synchronized (callbacksLock) {
			if(callbacks!=null) {
				int size = callbacks.size();
				for (int i = size - 1; i>=0;i--) {
					callbacks.remove(callback);
				}
				Log.d(TAG,"deleteCallback: New size="+callbacks.size());
			}
		}
	}

	public boolean isBound() {
		return igps!=null;
	}

	public void unbindService() {
		if(iCManager!=null) {
			try {
				context.unbindService(iCManager);
				igps=null;
				synchronized (callbacksLock) {
					for (GPSCallback callback : callbacks) {
						callback.onDisconnected();
					}
				}
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "", e);
			}
		}
	}

	public void bindService() {
		if(intent!=null) {
			boolean bound = this.context.bindService(intent, iCManager, Context.BIND_AUTO_CREATE);
			Log.d(TAG, "bindService: bound=" + bound);
		}
	}
}