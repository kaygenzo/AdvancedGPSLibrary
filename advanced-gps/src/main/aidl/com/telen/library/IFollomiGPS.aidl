package com.telen.library;

import com.telen.library.IFollomiGPSListener;

interface IFollomiGPS
{
	void registerListener(in IFollomiGPSListener listener);
	void unregisterListener();
	void startLocationListener(int powerPlan);
	void stopLocationListener();
	void getLastKnownLocation();
}
