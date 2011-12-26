package com.android.scheffsblend.orientation;

public interface OrientationListener {
	
	public void onOrientationChanged(float roll, float pitch, float yaw, float gravity[]);
	
}
