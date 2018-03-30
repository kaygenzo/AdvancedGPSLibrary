package com.telen.library;
interface IFollomiGPSListener
{
	void onLocationChanged(in Location location);
	void onLastKnownLocation(in Location location);
	void onLastKnownLocationFailed(in String exception);
	void onNeedLocationPermissions(int code);
	void onLocationTurnedOff(int code, in PendingIntent intent);
	void onLocationSuccessfullyStarted();
	void onLocationSuccessfullyStopped();
}
