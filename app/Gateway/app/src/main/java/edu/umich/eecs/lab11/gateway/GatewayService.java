package edu.umich.eecs.lab11.gateway;

import android.app.IntentService;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;


/**
 * Created by nklugman on 11/26/14.
 */
public class GatewayService extends IntentService implements SensorEventListener {

    private SensorManager mSensorManager;
    private LocationManager mLocationManager;

    private final static long LOCATION_WAIT_TIME = 300000l;

    private  final static String INTENT_TRUE = "TRUE";

    private final static String INTENT_SENSOR_ACCEL = "ACCEL";
    private final static String INTENT_SENSOR_TEMP = "TEMP";
    private final static String INTENT_SENSOR_GPS = "GPS";
    private final static String INTENT_SENSOR_AMBIANT = "AMBIANT";
    private final static String INTENT_SENSOR_HUMIDITY = "HUMIDITY";


    private Sensor mTemp;
    private Sensor mHumidity;
    private Sensor mAmbiant;

    public GatewayService() {
        super("IN SENSOR INTENT");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Normally we would do some work here, like download a file.
        // For our sample, we just sleep for 5 seconds.
        Log.w("INTENT", "IN SENSOR INTENT");



    }

    @Override
    public void onCreate() {
        /*
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            mTemp = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        } else {
            Log.w("VAL_SENSOR_ERROR", "no temp!");
        }
        mHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        mAmbiant = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        */

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        /*
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL); //FIRST TIME TO CHECK THE FUCKER
        Log.w("VAL_SENSOR_LIST", deviceSensors.toString());

        if (intent != null && intent.getExtras() != null) {
            try {
                if (intent.getExtras().getString(INTENT_SENSOR_ACCEL).equals(INTENT_TRUE)) {
                    Log.w("sensor_debug", "starting accel intent");
                    do_accel();
                }
                if (intent.getExtras().getString(INTENT_SENSOR_AMBIANT).equals(INTENT_TRUE)) {
                    Log.w("sensor_debug", "starting ambient intent");
                    do_ambiant();
                }
                if (intent.getExtras().getString(INTENT_SENSOR_GPS).equals(INTENT_TRUE)) {
                    Log.w("sensor_debug", "starting gps intent");
                    do_gps();
                }
                if (intent.getExtras().getString(INTENT_SENSOR_HUMIDITY).equals(INTENT_TRUE)) {
                    Log.w("sensor_debug", "starting humidity intent");
                    do_humidity();
                }
                if (intent.getExtras().getString(INTENT_SENSOR_TEMP).equals(INTENT_TRUE)) {
                    Log.w("sensor_debug", "starting temp intent");
                    do_temp();
                }
            }
            catch (IOError b) {
                Log.w("ERRRRRRR", "IOERR");
            }
        }
        */
        return super.onStartCommand(intent,flags,startId);
    }


    public void do_temp() {
        Log.w("top", "doing temp service!");
            mSensorManager.registerListener(this, mTemp, SensorManager.SENSOR_DELAY_NORMAL);



    }

    public void do_humidity() {
        Log.w("top", "doing humidity service!");
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null){
            mSensorManager.registerListener(this, mHumidity, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {
            Log.w("VAL_SENSOR_ERROR", "no temp!");
        }
    }

    public void do_gps() {
        Log.w("top", "doing gps!");
    }

    public void do_time() {
        Log.w("tag", "doing time!");
    }

    public void do_ambiant() {
        Log.w("top", "doing ambiant service!");
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            mSensorManager.registerListener(this, mAmbiant, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {
            Log.w("VAL_SENSOR_ERROR", "no light sensor!");
        }
    }

    public void do_accel() {
        Log.w("top", "doing accel service!");
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        Log.w("VAL_SENSOR_CHANGED", event.sensor.getName());



    }

    /*
    private Location getLocationByProvider(String provider) {
        Location location = null;
        try {
            if (mLocationManager.isProviderEnabled(provider)) {
                location = mLocationManager.getLastKnownLocation(provider);
            }
        } catch (IllegalArgumentException e) { }
        return location;
    }


    // Call to generate listeners that request the phones location.
    private void updateLocation () {

        for (String s : mLocationManager.getAllProviders()) {
            mLocationManager.requestLocationUpdates(s, LOCATION_WAIT_TIME, 0.0f, new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {
                    // Once we get a new location cancel our location updating
                    mLocationManager.removeUpdates(this);
                }

                @Override
                public void onProviderDisabled(String provider) { }

                @Override
                public void onProviderEnabled(String provider) { }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) { }
            });
        }
    }
    */

    /*
    @Override
    public void onCreate() {

        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(INTENT_ACCEL);
        ifilter.addAction(INTENT_GPS_FINE);
        ifilter.addAction(INTENT_TEMP);
        this.registerReceiver(mSensorTypeReceiver, ifilter);



        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }

    private void broadcastIntent (Intent lIntent) {
        //lIntent.setPackage("edu.umich.eecs.Gateway");
        //sendBroadcast(lIntent);
    }

    protected void onResume() {
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        mSensorManager.unregisterListener(this);
    }



    private BroadcastReceiver mSensorTypeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_TEMP)) {
                do_temp();
            }
        }
    };

    public void do_temp() {
        Log.w("top", "do_temp");
    }


    @Override
    public IBinder onBind(Intent arg0) {
        // Service does not allow binding
        return null;
    }



    // GPS
    private Location getLocationByProvider(String provider) {
        Location location = null;
        try {
            if (mLocationManager.isProviderEnabled(provider)) {
                location = mLocationManager.getLastKnownLocation(provider);
            }
        } catch (IllegalArgumentException e) { }
        return location;
    }

    private void updateLocation () {

        for (String s : mLocationManager.getAllProviders()) {
            mLocationManager.requestLocationUpdates(s, LOCATION_WAIT_TIME, 0.0f, new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {
                    // Once we get a new location cancel our location updating
                    mLocationManager.removeUpdates(this);
                }

                @Override
                public void onProviderDisabled(String provider) { }

                @Override
                public void onProviderEnabled(String provider) { }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) { }
            });
        }
    }
    */

}