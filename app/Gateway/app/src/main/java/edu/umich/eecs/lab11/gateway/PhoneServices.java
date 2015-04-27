package edu.umich.eecs.lab11.gateway;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.widget.EditText;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by thomas on 3/12/15.
 */
public class PhoneServices {
    private Context context;
    private SparseArray<String> sensors;
    private LocationManager locationManager;
    private PackageManager packageManager;
    private SensorManager sensorManager;
    private SharedPreferences preferences;
    
    private final static String INTENT_TRUE = "TRUE";
    private final static String INTENT_FALSE = "FALSE";

    private final static String INTENT_SENSOR_ACCEL = "ACCEL";
    private final static String INTENT_SENSOR_TEMP = "TEMP";
    private final static String INTENT_SENSOR_GPS = "GPS";
    private final static String INTENT_SENSOR_AMBIENT = "AMBIENT";
    private final static String INTENT_SENSOR_HUMIDITY = "HUMIDITY";

    private final static String INTENT_EXTRA_SENSOR_VAL = "SENSOR_VAL";

    private String popup_text_string = "";
    private String popup_pic_string = "";

    private NotificationManager notificationManager;
    private Notification.Builder notificationBuilder;
    private int notificationCount = 0;
    
    public PhoneServices(Context c) {
        context = c;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        packageManager = context.getPackageManager();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putStringSet("ui_devices",new HashSet<String>()).commit();
        System.out.println("AVAILABLE SENSORS : " + sensorManager.getSensorList(Sensor.TYPE_ALL).toString());
        sensors = new SparseArray<String>();
        sensors.put(Sensor.TYPE_MAGNETIC_FIELD, "");
        sensors.put(Sensor.TYPE_PRESSURE, "");
        sensors.put(Sensor.TYPE_ACCELEROMETER, "");
        sensors.put(Sensor.TYPE_LIGHT, "");
    }

    public void start() {
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL, 5000000);
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL, 5000000);
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL, 5000000);
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL, 5000000);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, (new Intent()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setClass(context, UiList.class), PendingIntent.FLAG_UPDATE_CURRENT);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = (new Notification.Builder(context)).setContentTitle("Interactive Devices Nearby").setContentText("BLE Peripherals closeby have UIs available.").setContentIntent(resultPendingIntent).setFullScreenIntent(resultPendingIntent,true).setPriority(Notification.PRIORITY_HIGH).setSmallIcon(R.drawable.ic_launcher).setVibrate(new long[]{100, 250, 100, 250});
    }

    public void stop() {
        sensorManager.unregisterListener(sensorListener);
        preferences.edit().putStringSet("ui_devices",new HashSet<String>()).commit();
    }

    private SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public final void onAccuracyChanged(Sensor sensor, int accuracy) { }

        @Override
        public final void onSensorChanged(SensorEvent event) {
            Integer type = event.sensor.getType();
            switch (type) {
                case Sensor.TYPE_MAGNETIC_FIELD: sensors.put(type,"["+event.values[0]+","+event.values[1]+","+event.values[2]+"]uT"); break;
                case Sensor.TYPE_PRESSURE: sensors.put(type,event.values[0]+"hPa"); break;
                case Sensor.TYPE_ACCELEROMETER: sensors.put(type,"["+event.values[0]+","+event.values[1]+","+event.values[2]+"]m/s^2"); break;
                case Sensor.TYPE_LIGHT: sensors.put(type,event.values[0]+"lx"); break;
            }
        }
    };


    public boolean getData(Peripheral cur_peripheral) {
        Log.w("top", "phoneServices.getData()");
        Intent intent = new Intent(context, GatewayService.class);

        intent.putExtra(INTENT_SENSOR_ACCEL, INTENT_FALSE);
        intent.putExtra(INTENT_SENSOR_AMBIENT, INTENT_FALSE);
        intent.putExtra(INTENT_SENSOR_GPS, INTENT_FALSE);
        intent.putExtra(INTENT_SENSOR_HUMIDITY, INTENT_FALSE);
        intent.putExtra(INTENT_SENSOR_TEMP, INTENT_FALSE);

        boolean intent_needed = false;

        String failable_hw = "";
        String sensor_access = "";
        boolean hasGPS = true;
        boolean allowsGPS = true;


        // the main sensor loop... does a couple things
        // 1. checks to see if user grants access... if it does
        // 2. checks to see if hw exists... if it does
        // 3. adds req for sensor to the intent
        // 4. if hw doesn't exist, adds to failable_hw to be skipped or not with level
        // 5. if user doesn't grant, adds to sensor_access to be skipped or not with level

        for (int i = Peripheral.ENUM.time.ordinal(); i <= Peripheral.ENUM.ip.ordinal(); i++) {
//            Log.w("sensor_debug", "TEST");
            if (cur_peripheral.FLAGS[i].equals("1")) {
                if (i == Peripheral.ENUM.accel.ordinal()) {
                    if (preferences.getBoolean("accel_agreement", true)) {
                        Log.w("sensor_debug", "adding accel to intent");
                        String key_val = "ACCELEROMETER ";
                        String sensor = sensors.get(Sensor.TYPE_ACCELEROMETER);
                        key_val += sensor;
                        if (sensor.length()>0) cur_peripheral.DATA_TO_PEEK.add(key_val);
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW ACCEL");
                        sensor_access += "accel";
                    }
                } else if (i == Peripheral.ENUM.time.ordinal()) {
                    if (preferences.getBoolean("time_agreement", true)) {
                        Log.w("sensor_debug", "adding time to intent");
                        String key_val = "GWTIME ";
                        key_val += System.currentTimeMillis();
                        cur_peripheral.DATA_TO_PEEK.add(key_val);
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW TIME");
                        sensor_access += "time";
                    }
//                } else if (i == Peripheral.ENUM.temp.ordinal()) {
//                    if (preferences.getBoolean("temp_agreement", true)) {
//                        Log.w("sensor_debug", "adding temp to intent");
//                        String key_val = "TEMPERATURE ";
//                        String sensor = sensors.get(Sensor.TYPE_AMBIENT_TEMPERATURE);
//                        key_val += sensor;
//                        if (sensor.length()>0) cur_peripheral.DATA_TO_PEEK.add(key_val);
//                    } else {
//                        Log.w("USER_AGREEMENT", "DOESNT ALLOW temp");
//                        sensor_access += "temp";
//                    }
                } else if (i == Peripheral.ENUM.gps.ordinal()) {
                    if (preferences.getBoolean("gps_agreement", true)) {
                        Log.w("sensor_debug", "adding gps to intent");
                        allowsGPS = true; //for a specific level
                        hasGPS = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
                        if (hasGPS) {
                            Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            String key_val = "LOCATION ";
                            key_val += loc.getLatitude()+","+loc.getLongitude()+","+loc.getAltitude();
                            cur_peripheral.DATA_TO_PEEK.add(key_val);
                        } else {
                            Log.w("USER_AGREEMENT", "DOESNT SUPPORT gps");
                        }
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW gps");
                    }
//                } else if (i == Peripheral.ENUM.humidity.ordinal()) {
//                    if (preferences.getBoolean("humidity_agreement", true)) {
//                        Log.w("sensor_debug", "adding humidity to intent");
//                        String key_val = "HUMIDITY ";
//                        String sensor = sensors.get(Sensor.TYPE_RELATIVE_HUMIDITY);
//                        key_val += sensor;
//                        if (sensor.length()>0) cur_peripheral.DATA_TO_PEEK.add(key_val);
//                    } else {
//                        Log.w("USER_AGREEMENT", "DOESNT ALLOW humidity");
//                        sensor_access += "humidity";
//                    }
                } else if (i == Peripheral.ENUM.pic.ordinal()) { // TODO: replace all camera/pic references with magnetometer
                    if (preferences.getBoolean("user_camera_agreement", true)) {
                        Log.w("sensor_debug", "adding magnetic field to intent");
                        String key_val = "MAGNETIC ";
                        String sensor = sensors.get(Sensor.TYPE_MAGNETIC_FIELD);
                        key_val += sensor;
                        if (sensor.length()>0) cur_peripheral.DATA_TO_PEEK.add(key_val);
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW magnetic field");
                        sensor_access += "pic";
                    }
                } else if (i == Peripheral.ENUM.text.ordinal()) {
                    if (preferences.getBoolean("user_input_agreement", true)) {
                        Log.w("sensor_debug", "adding barometer to intent");
                        String key_val = "BAROMETER ";
                        String sensor = sensors.get(Sensor.TYPE_PRESSURE);
                        key_val += sensor;
                        if (sensor.length()>0) cur_peripheral.DATA_TO_PEEK.add(key_val);
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW input");
                        sensor_access += "input";
                    }
                } else if (i == Peripheral.ENUM.ui.ordinal()) {
                    if (preferences.getBoolean("ui_agreement", true)) {
                        Log.w("sensor_debug", "doing ui");
//                        if (preferences.getString(cur_peripheral.FLAGS[Peripheral.ENUM.dev_address.ordinal()],"null").equals("null")) {
//                            BetterEditText ui_pref = new BetterEditText(context);
//                            ui_pref.setTitle(cur_peripheral.FLAGS[Peripheral.ENUM.dev_name.ordinal()]);
//                        }
                        Set<String> ui_devices = preferences.getStringSet("ui_devices", new HashSet<String>());
                        int size = ui_devices.size();
                        String dev_name = (cur_peripheral.FLAGS[Peripheral.ENUM.url.ordinal()]!=null) ? " (" + cur_peripheral.FLAGS[Peripheral.ENUM.dev_name.ordinal()] + ")" : "";
                        ui_devices.add(cur_peripheral.FLAGS[Peripheral.ENUM.dev_address.ordinal()] + dev_name + " @ " + cur_peripheral.FLAGS[Peripheral.ENUM.url.ordinal()]);
                        if (size != ui_devices.size()) {
                            preferences.edit().putStringSet("ui_devices", ui_devices).commit();//(cur_peripheral.FLAGS[Peripheral.ENUM.dev_address.ordinal()], cur_peripheral.FLAGS[Peripheral.ENUM.url.ordinal()]).apply();
                            System.out.println("GUI : " + preferences.getStringSet("ui_devices", new HashSet<String>()));
                            Notification notification = notificationBuilder.setNumber(ui_devices.size()).build();
//                            notification.flags = Notification.FLAG_
                            notificationManager.notify(1, notification);
                            UiList.UiListFragment fragment = UiList.UiListFragment.getInstance();
                            if (fragment!=null) fragment.onSharedPreferenceChanged(preferences,"ui_devices");
                        }
//                        preferences.edit().
//                        popupWebView(cur_peripheral.FLAGS[Peripheral.ENUM.data_blob.ordinal()], cur_peripheral.FLAGS[Peripheral.ENUM.dev_address.ordinal()], cur_peripheral.FLAGS[Peripheral.ENUM.url.ordinal()]);
//                        String key_val = "IMAGE ";
//                        key_val += popup_pic_string;
//                        Log.w("sensor_debug", popup_pic_string);
//                        if (popup_pic_string.length()>0) {
//                            cur_peripheral.DATA_TO_PEEK.add(key_val);
//                            Log.w("sensor_debug", popup_pic_string);
//                        }
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW pic");
                        sensor_access += "pic";
                    }
                } else if (i == Peripheral.ENUM.ambient.ordinal()) {
                    if (preferences.getBoolean("ambient_agreement", true)) {
                        Log.w("sensor_debug", "adding ambient to intent");
                        String key_val = "LIGHT ";
                        String sensor = sensors.get(Sensor.TYPE_LIGHT);
                        key_val += sensor;
                        if (sensor.length()>0) cur_peripheral.DATA_TO_PEEK.add(key_val);
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW ambient");
                        sensor_access += "ambient";
                    }
                }
            }
        }

        //Check to see if the failure is ok based on peripheral_qos
        Integer periph_qos = Integer.parseInt(cur_peripheral.FLAGS[Peripheral.ENUM.qos.ordinal()], 2);
        boolean is_able = true;
        if (failable_hw.length() != 0 || sensor_access.length() != 0) { //so hw doesn't support some sensor or user doesn't allow some sensor
            if (periph_qos == Peripheral.QOS_ENUM.REQ_NONE.ordinal() ||
                    periph_qos == Peripheral.QOS_ENUM.REQ_ALL_NO_SENSORS.ordinal() ||
                    periph_qos == Peripheral.QOS_ENUM.REQ_ALL_NO_SENSORS_NOT_INCL_GPS.ordinal() ||
                    periph_qos == Peripheral.QOS_ENUM.REQ_NONE_BUT_CONNECTION.ordinal() ||
                    periph_qos == Peripheral.QOS_ENUM.REQ_NONE_BUT_SERVICE.ordinal()) {
            } else {
                Log.w("IS_ABLE_FALSE", "PERIPHERAL QOS NOT ALIGNED");
                is_able = false;
            }
        }
        if (!hasGPS || !allowsGPS) { //for the req_all_no_sensors_not_incl_gps qos
            if (periph_qos == Peripheral.QOS_ENUM.REQ_NONE.ordinal() ||
                    periph_qos == Peripheral.QOS_ENUM.REQ_ALL_NO_SENSORS.ordinal() ||
                    periph_qos == Peripheral.QOS_ENUM.REQ_NONE_BUT_CONNECTION.ordinal() ||
                    periph_qos == Peripheral.QOS_ENUM.REQ_NONE_BUT_SERVICE.ordinal()) {
            } else {
                Log.w("IS_ABLE_FALSE", "PERIPHERAL QOS NOT ALIGNED GPS");
                is_able = false;
            }
        }

        if (periph_qos == Peripheral.QOS_ENUM.REQ_NONE_BUT_SERVICE.ordinal()) {
            if (!isOnline()) {
                Log.w("IS_ABLE_FALSE", "PERIPHERAL QOS NOT ALIGNED SERVICE");
                is_able = false;
            }
        }


        Log.w("IS_ABLE_SENSORS", String.valueOf(is_able));

        if (is_able && intent_needed) {
//            context.startService(intent);
            return true;
        } else if (is_able) {
            return true;
        }
        return false;
    }


    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void popupPic() {
        Log.w("top", "popupPic");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) context.startActivity(takePictureIntent);
    }

    public void popupText() {
        Log.w("top", "popupText");

//        mScanning = false;
//        paused = true;
//        mBluetoothAdapter.stopLeScan(mLeScanCallback);

        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle("Peripheral Asked for Text");
        alert.setMessage("Enter Text:");
        final EditText input = new EditText(context);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                popup_text_string = input.getText().toString();
//                mScanning = true;
//                paused = false;
//                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                popup_text_string = "";
//                mScanning = true;
//                paused = false;
//                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
        });
        alert.show();
    }

}
