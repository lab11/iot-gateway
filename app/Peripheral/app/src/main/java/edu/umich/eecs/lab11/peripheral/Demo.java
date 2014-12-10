package edu.umich.eecs.lab11.peripheral;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class Demo extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //public char[] finalStr;
    public String final_str;
    public String tag = "tag";

    private SharedPreferences cur_settings;
    private BluetoothAdapter bleAdapter;
    private BluetoothLeAdvertiser bleAdvertiser;
    private AdvertiseSettings.Builder settingsBuilder;
    private AdvertiseData.Builder dataBuilder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cur_settings = PreferenceManager.getDefaultSharedPreferences(this);
        cur_settings.registerOnSharedPreferenceChangeListener(this);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new DemoFragment()).commit();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bleAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

    }

    protected void onResume() {
        super.onResume();
        cur_settings = PreferenceManager.getDefaultSharedPreferences(this);
        cur_settings.registerOnSharedPreferenceChangeListener(this);

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bleAdapter.isEnabled()) {
            if (!bleAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }

        bleAdvertiser = bleAdapter.getBluetoothLeAdvertiser();
        advertise();
    }

    protected void onPause() {
        super.onPause();
        cur_settings.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void advertise() {
        getFragmentManager().beginTransaction().replace(android.R.id.content, new DemoFragment()).commit();
        String ad_value = cur_settings.getString("advertisement_value","0000");
        settingsBuilder = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true).setTimeout(0);
        dataBuilder = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true)
                .addServiceData(shortUUID(ad_value.substring(0,4)),toByteArray(ad_value.substring(4)));
        if (cur_settings.getBoolean("advertise_switch",false))
            bleAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), advertiseCallback);
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            toastNotify("Started Advertising");
        }
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            toastNotify("Failed to Advertise");
            cur_settings.edit().putBoolean("advertise_switch",false).commit();
        }

    };


    /**
     * Settings Fragment: Pulls information from preference xml & automatically updates on change
     */
    public static class DemoFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.demo_preferences);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        doGen();
        if(key.equals("advertise_switch")) {
            if (cur_settings.getBoolean("advertise_switch",false)) {
                advertise();
            } else {
                bleAdvertiser.stopAdvertising(advertiseCallback);
                toastNotify("Stopped Advertising");
            }
        } else cur_settings.edit().putBoolean("advertise_switch",false).commit();
    }

//    @Override
//    public void onHeaderClick(Header header, int position) {
//        super.onHeaderClick(header, position);
//        if (header.id == R.id.gen_btn) {
////            doGenBTN();
//        }
//        if (header.id == R.id.send_btn) {
////            doSendBTN();
//        }
//    }
//
//    @Override
//    protected boolean isValidFragment(String fragmentName) {
//        return true;
//    }
//
//    @Override
//    public void onBuildHeaders(List <Header> target) {
//        loadHeadersFromResource(R.xml.activity_demo, target);
//
//        for(Header header: target) {
//            if (header.title.equals("GENERATE")) {
//                String summary = "Final String: " + final_str;
//                header.summary = summary;
//            }
//        }
//
//    }

//    public void doSendBTN() {
//        Log.w(tag, "doSendBTN");
//        if (!final_str.equals("")) {
//            setResult(Activity.RESULT_OK,
//                    new Intent().putExtra("FINAL_STR", final_str));
//            finish();
//        } else {
//            finish();
//        }
//    }

    public String boolToStr(boolean state) {
        if (state) return "1";
        return "0";
    }

    public void doGen() {
        Log.w(tag, "doGenBTN");

        String IPTEXT = cur_settings.getString("ip_text", "20014701F1013202");
        String transparentBTN = boolToStr(cur_settings.getBoolean("peripheral_transparent_req", true));
        String rateTEXT = String.valueOf(cur_settings.getInt("data_rate", -1));
        String levelTEXT = String.valueOf(cur_settings.getInt("level_rate", -1));
        String gpsBTN = boolToStr(cur_settings.getBoolean("peripheral_gps_req", true));
        String tempBTN = boolToStr(cur_settings.getBoolean("peripheral_temp_req", true));
        String humidityBTN = boolToStr(cur_settings.getBoolean("peripheral_humidity_req", true));
        String timeBTN = boolToStr(cur_settings.getBoolean("peripheral_time_req", true));
        String accelBTN = boolToStr(cur_settings.getBoolean("peripheral_accel_req", true));
        String textBTN = boolToStr(cur_settings.getBoolean("peripheral_text_req", true));
        String userPicBTN = boolToStr(cur_settings.getBoolean("peripheral_pic_req", true));
        String ambientBTN = boolToStr(cur_settings.getBoolean("peripheral_ambient_req", true));
        String programTEXT = String.valueOf(cur_settings.getInt("incentive_rate", -1));
        String dataTEXT = cur_settings.getString("data_text", "FF");

        rateTEXT = Integer.toString(Integer.valueOf(rateTEXT.replaceAll("\\s+","")), 2);
        while (rateTEXT.length() != 3) {
            rateTEXT = "0" + rateTEXT;
        }
        String first_nib = transparentBTN + rateTEXT;
        Log.w("DEMO_VAL_FIRST NIB BITS", first_nib);
        first_nib = String.format("%21X", Long.parseLong(first_nib,2)).replaceAll("\\s+","");
        Log.w("DEMO_VAL_FIRST NIB", first_nib);

        levelTEXT = Integer.toString(Integer.valueOf(levelTEXT.replaceAll("\\s+","")), 2);

        String second_nib = levelTEXT;
        Log.w("DEMO_VAL_SECOND NIB BITS", second_nib);
        second_nib = String.format("%21X", Long.parseLong(second_nib,2)).replaceAll("\\s+","");
        Log.w("DEMO_VAL_SECOND NIB", second_nib);


        String sensor_str = gpsBTN + tempBTN + humidityBTN;
        sensor_str += timeBTN + accelBTN + textBTN + userPicBTN;
        sensor_str += ambientBTN;
        Log.w("DEMO_VAL_THIRD_FOURTH NIB BITS", sensor_str);
        String third_fourth_nib = String.format("%21X", Long.parseLong(sensor_str,2)).replaceAll("\\s+","");
        if (third_fourth_nib.length() == 1) {
            third_fourth_nib = "0" + third_fourth_nib;
        }
        Log.w("DEMO_VAL_THIRD_FOURTH NIB", third_fourth_nib);

        programTEXT = Integer.toString(Integer.valueOf(programTEXT.replaceAll("\\s+","")), 2);
        if (programTEXT.length() != 4) {
            programTEXT = "0" + programTEXT;
        }
        String fifth_nib = programTEXT;
        fifth_nib = String.format("%21X", Long.parseLong(fifth_nib,2)).replaceAll("\\s+","");
        Log.w("DEMO_VAL_FIFTH NIB", fifth_nib);

        final_str = IPTEXT + first_nib + second_nib;
        final_str += third_fourth_nib + fifth_nib + dataTEXT;
        Log.w(tag, final_str);
        cur_settings.edit().putString("advertisement_value",final_str).commit();
        cur_settings.getString("advertisement_value",final_str);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("http://bit.ly/11MSxZW")));
            return true;
        } else if (id == R.id.action_reset) {
            cur_settings.edit().clear().commit();
            getFragmentManager().beginTransaction().replace(android.R.id.content, new DemoFragment()).commit();
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * HELPER FUNCTIONS
     */

    public byte[] toByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2 + len % 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + ((i+1)>=len ? 0 : Character.digit(s.charAt(i+1), 16)));
        }
        return data;
    }

    public ParcelUuid shortUUID(String s) {
        return ParcelUuid.fromString("0000" + s + "-0000-1000-8000-00805F9B34FB");
    }

    public void toastNotify(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }


}
