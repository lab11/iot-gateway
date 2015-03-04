package edu.umich.eecs.lab11.gateway;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class Gateway extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

//    private static final int REQUEST_ENABLE_BT = 1;
//    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private SharedPreferences cur_settings;
    private Intent gatewayIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new GatewayFragment()).commit();
        cur_settings = PreferenceManager.getDefaultSharedPreferences(this);
        if(cur_settings.getBoolean("master_agreement",false)) {
            gatewayIntent = new Intent(this,GatewayService.class);
            startService(gatewayIntent);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        cur_settings.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cur_settings.unregisterOnSharedPreferenceChangeListener(this);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        // User chose not to enable Bluetooth.
//        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
//            finish();
//            return;
//        }
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//            Bundle extras = data.getExtras();
//            Bitmap bm = (Bitmap) extras.get("data");
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//            popup_pic_string = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if(s.equals("master_agreement"))
            if (cur_settings.getBoolean("master_agreement", false)) {
                gatewayIntent = new Intent(this,GatewayService.class);
                startService(gatewayIntent);
            } else stopService(gatewayIntent);
    }

    /**
     * Settings Fragment: Pulls information from preference xml & automatically updates on change
     */
    public static class GatewayFragment extends PreferenceFragment {
        private SensorManager mSensorManager;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) == null) getPreferenceManager().findPreference("temp_agreement").setEnabled(false);
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) == null) getPreferenceManager().findPreference("humidity_agreement").setEnabled(false);
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) getPreferenceManager().findPreference("accel_agreement").setEnabled(false);
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) getPreferenceManager().findPreference("ambient_agreement").setEnabled(false);
        }
    }

}