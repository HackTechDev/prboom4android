package com.android.scheffsblend.orientation;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class OrientationManager {
	
	private static final int ORIENTATION_RAW = 0;
	private static final int ORIENTATION_AVERAGED = 1;
	private static final int ORIENTATION_FILTERED = 2;
	
    private static float yawAverage[] = null;
    private static float pitchAverage[] = null;
    private static float rollAverage[] = null;
    private static int avgCount = 0;
    private static int numToAverage = 0;
    private static float lastYaw = 0.0f;
    private static float lastPitch = 0.0f;
    private static float lastRoll = 0.0f;
    private static float yawOffset = 0.0f;
    private static float pitchOffset = 0.0f;
    private static float rollOffset = 0.0f;

    private static float[] inR = new float[16];
    private static float[] outR= new float[16];
    private static float[] I = new float[16];
    private static float[] gravity = new float[3];
    private static float[] geomag = new float[3];
    private static float[] orientVals = new float[3];

	private static SensorManager sensorManager;
	private static OrientationListener listener;
	
	/** indicates whether or not Accelerometer sensor is supported */
	private static Boolean supported;
	/** indicates whether or not the Accelerometer Sensor is running */
	private static boolean running = false;
	
	/** the alpha value used for low-pass filtering */
	private static float filterAlpha = 0.2f;
	
	/** identifies the type of readings to return to the listener */
	private static int readingsMode = ORIENTATION_RAW;
	
	/**
	 * Returns true if the manager is listening to orientation changes
	 */
	public static boolean isListening() {
		return running;
	}
	
	/**
	 * Unregisters listeners
	 */
	public static void stopListening() {
		running = false;
		try {
			if (sensorManager != null && sensorEventListener != null) {
				sensorManager.unregisterListener(sensorEventListener);
			}
		} catch (Exception e) {}
	}
	
	/**
	 * Returns true if at least one Accelerometer sensor is available
	 */
	public static boolean isSupported(Context ctx) {
		if (supported == null) {
			if (ctx != null) {
				sensorManager = (SensorManager) ctx.getSystemService(ctx.SENSOR_SERVICE);
				List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
				supported = new Boolean(sensors.size() > 0);
			} else {
				supported = Boolean.FALSE;
			}
		}
		
		return supported;
	}
	
	/**
	 * Registers a listener and start listening
	 * @param ctx
	 * 			context of activity to register listener with
	 * @param orientationListener
	 * 			callback for orientation events
	 */
	public static void startListening(Context ctx, OrientationListener orientationListener) {
		sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		running = (sensorManager.registerListener(sensorEventListener, 
						sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
						SensorManager.SENSOR_DELAY_GAME) |
				sensorManager.registerListener(sensorEventListener, 
						sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 
						SensorManager.SENSOR_DELAY_GAME));
		
		readingsMode = ORIENTATION_RAW;
		listener = orientationListener;
	}
	
	/**
	 * Registers a listener and start listening
	 * @param ctx
	 * 			context of activity to register listener with
	 * @param orientationListener
	 * 			callback for orientation events
	 * @param avgCount
	 * 			number of readings to average before calling listener
	 */
	public static void startListening(Context ctx, OrientationListener orientationListener, int avgCount) {
		sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		running = (sensorManager.registerListener(sensorEventListener, 
						sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
						SensorManager.SENSOR_DELAY_FASTEST) |
				sensorManager.registerListener(sensorEventListener, 
						sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 
						SensorManager.SENSOR_DELAY_FASTEST));
		numToAverage = avgCount;
		if( numToAverage > 1 ) {
			yawAverage = new float[numToAverage];
			pitchAverage = new float[numToAverage];
			rollAverage = new float[numToAverage];
			readingsMode = ORIENTATION_AVERAGED;
		} else {
			readingsMode = ORIENTATION_RAW;
		}

		listener = orientationListener;
	}
	
	/**
	 * Registers a listener and start listening
	 * @param ctx
	 * 			context of activity to register listener with
	 * @param orientationListener
	 * 			callback for orientation events
	 * @param alpha
	 * 			number of readings to average before calling listener
	 */
	public static void startListening(Context ctx, OrientationListener orientationListener, float alpha) {
		sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		running = (sensorManager.registerListener(sensorEventListener, 
						sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
						SensorManager.SENSOR_DELAY_FASTEST) |
				sensorManager.registerListener(sensorEventListener, 
						sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 
						SensorManager.SENSOR_DELAY_FASTEST));

		if(alpha > 0.0f && alpha < 1.0f)
			filterAlpha = alpha;
		else
			filterAlpha = 0.2f;
		
		readingsMode = ORIENTATION_FILTERED;
		listener = orientationListener;
	}
	
    private static SensorEventListener sensorEventListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void onSensorChanged(SensorEvent event) {
			// If the sensor data is unreliable return
			if (event.accuracy != SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
				return;

			// Gets the value of the sensor that has been changed
			switch (event.sensor.getType()){  
				case Sensor.TYPE_ACCELEROMETER:
					gravity = event.values.clone();
					break;
				case Sensor.TYPE_MAGNETIC_FIELD:
					geomag = event.values.clone();
					break;
			}

			// If gravity and geomag have values then find rotation matrix
			if (gravity != null && geomag != null){
				boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
			    if (success){
			    	// Re-map coordinates so y-axis comes out of camera
	                SensorManager.remapCoordinateSystem(inR, 
	                		SensorManager.AXIS_X, SensorManager.AXIS_Z, outR); 
	                SensorManager.getOrientation(outR, orientVals); 
	                
	                float yaw = orientVals[0] + (float)Math.PI - yawOffset;
	                float pitch = orientVals[1] - pitchOffset;
	                float roll = orientVals[2] - rollOffset;

	                switch(readingsMode) {
	                	case ORIENTATION_RAW:
	                		listener.onOrientationChanged(roll, pitch, yaw, gravity);
	                		break;
	                	case ORIENTATION_AVERAGED:
		                	yawAverage[avgCount] = yaw;
		                	pitchAverage[avgCount] = pitch;
		                	rollAverage[avgCount] = roll;
		                	avgCount++;
				     
		                	if(avgCount >= numToAverage) {
		                		float pitchAvg = 0;
		                		float yawAvg = 0;
		                		float rollAvg = 0;
		                		for(int i = 0; i < numToAverage; i++) {
		                			yawAvg += yawAverage[i];
		                			pitchAvg += pitchAverage[i];
		                			rollAvg += rollAverage[i];
		                		}
		                		yawAvg /= numToAverage;
		                		pitchAvg /= numToAverage;
		                		rollAvg /= numToAverage;
		                		avgCount = 0;
		                		listener.onOrientationChanged(rollAvg, pitchAvg, yawAvg, gravity);
		                	}
	                			break;
	                	case ORIENTATION_FILTERED:
	                		float yawDiff = Math.abs(lastYaw-yaw);
	                		if(yawDiff < (float)Math.PI/6)
	                			lastYaw = lastYaw + (filterAlpha * (yaw - lastYaw));
	                		else if(yawDiff < (float)Math.PI)
	                			lastYaw = lastYaw + ((filterAlpha*4) * (yaw - lastYaw));
	                		else
	                			lastYaw = yaw;

	                		float pitchDiff = Math.abs(lastPitch-pitch);
	                		if(pitchDiff < (float)Math.PI/6)
	                			lastPitch = lastPitch+ (filterAlpha * (pitch - lastPitch));
	                		else if(pitchDiff < (float)Math.PI)
	                			lastPitch = lastPitch+ ((filterAlpha*4) * (pitch - lastPitch));
	                		else
	                			lastPitch = pitch;
	                		
	                		lastRoll = lastRoll + (filterAlpha * (roll - lastRoll));
	                		
	                		listener.onOrientationChanged(lastRoll, lastPitch, lastYaw, gravity);
	                		break;
	                	
	                }
			    }
			}   
		}
    };
    
    public static void setOffsets(float yaw, float pitch, float roll) {
    	yawOffset = yaw;
    	pitchOffset = pitch;
    	rollOffset = roll;
    }
}
