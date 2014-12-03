package edu.umich.eecs.lab11.gateway;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;



public class Gateway extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ArrayList<String> programValid = new ArrayList<String>();
    private ArrayList<Boolean> programCredentials = new ArrayList<Boolean>();
    private ArrayList<Integer> programMaxPay = new ArrayList<Integer>();
    private ArrayList<String> programURL = new ArrayList<String>();
    private ArrayList<Integer> programSizesTotal = new ArrayList<Integer>();


    private Peripheral cur_peripheral;

    private JSONObject jsonParams;
    private JSONObject programJSONParams;

    private final boolean DEMO = false; //turns on or off the toast reports on failures

    private SensorManager mSensorManager;

    private  final static String INTENT_TRUE = "TRUE";
    private  final static String INTENT_FALSE = "FALSE";

    private final static String INTENT_SENSOR_ACCEL = "ACCEL";
    private final static String INTENT_SENSOR_TEMP = "TEMP";
    private final static String INTENT_SENSOR_GPS = "GPS";
    private final static String INTENT_SENSOR_AMBIANT = "AMBIANT";
    private final static String INTENT_SENSOR_HUMIDITY = "HUMIDITY";

    private final static String INTENT_EXTRA_SENSOR_VAL = "SENSOR_VAL";

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private boolean paused;
    private static AsyncHttpClient client = new AsyncHttpClient();

    private final String tag = "tag";
    private final String top = "top";

    private long peek_time;

    private String program_name_to_send;
    private String program_pay_to_send;
    private String program_url_to_send;
    private Integer program_cur_packet_size;

    private String final_str;
    private String final_binary_str;

    private boolean waitingForOffload;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int DEMO_STR = 0;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private SharedPreferences cur_settings;

    // META data to pass along
    //private String gateway_id = Secure.getString(getApplicationContext().getContentResolver(),
    //        Secure.ANDROID_ID); //TODO Known issues with this method... move to an DB call that we keep
    private String gateway_first_contact_time;
    private String gateway_transmit_time;
    private String gate_size_transmit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new GatewayFragment()).commit();
        cur_settings = PreferenceManager.getDefaultSharedPreferences(this);

        cur_peripheral = new Peripheral();

        jsonParams = new JSONObject();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        programValid.add("PayMe");
        programValid.add("YOUPAY");
        programCredentials.add(true);
        programCredentials.add(true);
        programMaxPay.add(8);
        programMaxPay.add(15);
        programURL.add("www.google.com");
        programURL.add("www.facebook.com");

        program_name_to_send = "";
        program_pay_to_send = "";
        program_url_to_send = "";

        getSystemService(Context.LOCATION_SERVICE);

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    /**
     * Settings Fragment: Pulls information from preference xml & automatically updates on change
     */
    public static class GatewayFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }


    public String hexToBinary(final String hexStr) {
        StringBuffer binStr = new StringBuffer();
        String[] conversionTable = { "0000", "0001", "0010", "0011", "0100", "0101", "0110", "0111", "1000", "1001", "1010", "1011", "1100", "1101", "1110", "1111" };

        for (int i = 0; i < hexStr.length(); i++) {
            binStr.append(conversionTable[Character.digit(hexStr.charAt(i), 16)]);
        }
        return binStr.toString();
    }

    public void parse() {
        Log.w(top, "parse()");


        cur_peripheral.empty(); //NEW PACKET

        if (!cur_settings.getBoolean("master_agreement",false)) {
            if (DEMO) {
                Toast.makeText(this, "GATEWAY NOT ENABLED! STOPPING", Toast.LENGTH_SHORT).show();
            }

            Log.w("POINT", "GATEWAY NOT ENABLED!");
            return; //BREAK OUT
        }

        // Do the actual parsing
        // TODO: change these to actual offsets... hardcode hack
        final_binary_str = hexToBinary(final_str);
        String IP = final_binary_str.substring(0,64);
        IP = String.format("%21X", Long.parseLong(IP,2));
        String TRANSPARENT = final_binary_str.substring(64,65);
        String RATE = final_binary_str.substring(65,68);
        String LEVEL = final_binary_str.substring(68,72);
        String SENSORS = final_binary_str.substring(72,80);
        String PROGRAM_TYPE = final_binary_str.substring(80,84);
        String DATA = final_binary_str.substring(84);

        /*
        Log.w("PARSE_FINAL_STR", final_str);
        Log.w("PARSE_IP", IP);
        Log.w("PARSE_TRANSPARENT", TRANSPARENT);
        Log.w("PARSE_RATE", RATE);
        Log.w("PARSE_LEVEL", LEVEL);
        Log.w("PARSE_SENSORS", SENSORS);
        Log.w("PARSE_PROGRAM_TYPE", PROGRAM_TYPE);
        Log.w("PARSE_DATA", DATA);
        */

        if (TRANSPARENT.equals("1")) { //DONE WITH TRANSPARENT BIT
            Log.w("POINT", "TRANSPARENT FORWARD");
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.ip_address.ordinal()] = IP;
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.rate.ordinal()] = RATE;
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.program_type.ordinal()] = PROGRAM_TYPE;
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.data_blob.ordinal()] = DATA;
        } else {
            Log.w("POINT", "PEEK FORWARD");
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.ip_address.ordinal()] = IP;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.rate.ordinal()] = RATE;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.level.ordinal()] = LEVEL;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.accel.ordinal()] = String.valueOf(SENSORS.charAt(4)); //Jesus is this hacky... Hardcoded to match sensor order... Can change to peripheral.SENSOR_ENUM.x.ordinal()
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.temp.ordinal()] = String.valueOf(SENSORS.charAt(1));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.time.ordinal()] = String.valueOf(SENSORS.charAt(3));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.gps.ordinal()] = String.valueOf(SENSORS.charAt(0));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.humidity.ordinal()] = String.valueOf(SENSORS.charAt(2));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.pic.ordinal()] = String.valueOf(SENSORS.charAt(6));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.text.ordinal()] = String.valueOf(SENSORS.charAt(5));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.ambiant.ordinal()] = String.valueOf(SENSORS.charAt(7));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.program_type.ordinal()] = PROGRAM_TYPE;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.data_blob.ordinal()] = DATA;
        }
        run_forward(TRANSPARENT);
    }


    public boolean run_forward(String TRANSPARENT) {

        //DO SENSORS
        Integer peripheral_program_level;
        if (TRANSPARENT.equals("0")) { //DONE WITH TRANSPARENT BIT
            if (do_sensors()){ //all sensors were fine
                Log.w("POINT", "SENSORS DONE!");
                peripheral_program_level = Integer.parseInt(cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.program_type.ordinal()], 2);
            } else {
                if (DEMO) {
                    Toast.makeText(this, "SENSORS NOT ABLE! Stopping", Toast.LENGTH_SHORT).show();
                }
                Log.w("POINT", "SENSORS NOT ABLE!");
                return false;
            }
        } else {
            peripheral_program_level = Integer.parseInt(cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.program_type.ordinal()], 2);
        }
        switch_grant(peripheral_program_level);
        return true;
    }

    public boolean switch_grant(Integer peripheral_program_level) {
        Log.w(top, "switch_grant()");

        Integer user_program_level = cur_settings.getInt("level_rate", 15);
        Integer user_pay_floor = cur_settings.getInt("min_pay_rate",0);

        Log.w("SWITCH_GRANT_periph_level", peripheral_program_level.toString());
        Log.w("SWITCH_GRANT_user_level", user_program_level.toString());
        Log.w("SWITCH_GRANT_user_pay_level", user_pay_floor.toString());


        if (peripheral_program_level == 0) { // no program supported by peripheral
            if (user_pay_floor == 0) { // user does not want monitary program
                schedule();
            } else {
                if (DEMO) {
                    Toast.makeText(this, "PROGRAM NOT SUPPORTED! Stopping", Toast.LENGTH_SHORT).show();
                }
                Log.w("POINT", "Program NOT SUPPORTED"); // peripheral can't pay
                return false;
            }
        }
        else if (peripheral_program_level == 15) { // every program supported by peripheral
            if (user_pay_floor != 0) { // user wants monitary program
                if (!do_monitary_program(peripheral_program_level)) return false; // peripheral can pay... max that sucker out
                schedule();
            } else {
                schedule(); //user doesn't care... send it altruistically
            }
        }
        else {  // some programs supported by peripheral
            if (peripheral_program_level >= user_pay_floor) { // the peripheral can support up to a certain amount of pay... user accepts
                if (!do_monitary_program(peripheral_program_level)) return false; //pay as much as the peripheral wants
                schedule();
            } else {
                if (DEMO) {
                    Toast.makeText(this, "PERIPHERAL CAN'T PAY! Stopping", Toast.LENGTH_SHORT).show();
                }
                Log.w("POINT", "Peripheral CAN'T PAY"); // peripheral can't pay
                return false;
            }
        }
        return true;
    }

    private boolean do_monitary_program(Integer pay_level) {
        Log.w(top, "do_monitary_program(Integer pay_level)");
        String program_name = cur_settings.getString("program_text", "PayMe").trim();
        Log.w("program_debug", program_name);
        Integer program_index = programValid.lastIndexOf(program_name);
        Log.w("program_debug", program_index.toString());
        if (program_index != -1) {
            if (programCredentials.get(program_index)) {
                if (pay_level <= programMaxPay.get(program_index)) {
                    program_name_to_send = program_name;
                    program_pay_to_send = pay_level.toString();
                    program_url_to_send = programURL.get(program_index);
                } else {
                    if (DEMO) {
                        Toast.makeText(this, "PROGRAM CAN'T PAY PAY LEVEL! Stopping", Toast.LENGTH_SHORT).show();
                    }
                    Log.w("POINT", "Program CAN'T PAY PAY LEVEL"); // peripheral can't pay
                    return false;
                }
            } else {
                if (DEMO) {
                    Toast.makeText(this, "PROGRAM NOT CREDENTIALED! Stopping", Toast.LENGTH_SHORT).show();
                }
                Log.w("POINT", "Program NOT CREDENTIALED"); // peripheral can't pay
                return false;
            }
        } else {
            if (DEMO) {
                Toast.makeText(this, "PROGRAM NAME NOT FOUND! Stopping", Toast.LENGTH_SHORT).show();
            }
            Log.w("POINT", "Program NAME NOT FOUND"); // peripheral can't pay
            return false;
        }
        return true;
    }

    private void schedule() {
        Log.w(top, "schedule()");
    }

    String mCurrentPhotoPath;
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        try {
            jsonParams.put("s_image", image);
        } catch (JSONException e) {
            Log.w("JSONEception", "IMAGE");
        }
        return image;
    }

    public void take_picture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, 1);
            }
        }
    }


    public boolean hasGPSDevice(Context context)
    {
        final LocationManager mgr = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        if ( mgr == null ) return false;
        final List<String> providers = mgr.getAllProviders();
        if ( providers == null ) return false;
        return providers.contains(LocationManager.GPS_PROVIDER);
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public boolean do_sensors() {
        Log.w(top, "do_sensors()");
        Intent intent = new Intent(this, GatewayService.class);

        intent.putExtra(INTENT_SENSOR_ACCEL, INTENT_FALSE);
        intent.putExtra(INTENT_SENSOR_AMBIANT, INTENT_FALSE);
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
        for (int i = Peripheral.PEEK_ENUM.gps.ordinal(); i <= Peripheral.PEEK_ENUM.ambiant.ordinal(); i++) {
            //Log.w("sensor_debug", cur_peripheral.PEEK_FLAGS[i]);
            if (cur_peripheral.PEEK_FLAGS[i].equals("1")) {
                if (i == Peripheral.PEEK_ENUM.accel.ordinal()) {
                    if (cur_settings.getBoolean("accel_agreement", true)) {
                        Log.w("USER_AGREEMENT", "DOES ALLOW ACCEL");
                        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                            Log.w("sensor_debug", "adding accel to intent");
                            intent.putExtra(INTENT_SENSOR_ACCEL, INTENT_TRUE);
                            intent_needed = true;
                        } else {
                            Log.w("USER_AGREEMENT", "DOESNT SUPPORT accel");
                            failable_hw += " accel ";
                        }
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW ACCEL");
                        sensor_access += "accel";
                    }
                } else if (i == Peripheral.PEEK_ENUM.time.ordinal()) {
                    if (cur_settings.getBoolean("time_agreement", true)) {
                        Log.w("sensor_debug", "adding time to intent");
                        peek_time = System.currentTimeMillis();
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW TIME");
                        sensor_access += "time";
                    }
                } else if (i == Peripheral.PEEK_ENUM.temp.ordinal()) {
                    if (cur_settings.getBoolean("temp_agreement", true)) {
                        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
                            Log.w("sensor_debug", "adding temp to intent");
                            intent.putExtra(INTENT_SENSOR_TEMP, INTENT_TRUE);
                            intent_needed = true;
                        } else {
                            Log.w("USER_AGREEMENT", "DOESNT SUPPORT temp");
                            failable_hw += " temp ";
                        }
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW temp");
                        sensor_access += "temp";
                    }
                } else if (i == Peripheral.PEEK_ENUM.gps.ordinal()) {
                    if (cur_settings.getBoolean("gps_agreement", true)) {
                        Log.w("sensor_debug", "adding gps to intent");
                        allowsGPS = true; //for a specific level
                        PackageManager packageManager = getApplicationContext().getPackageManager();
                        hasGPS = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
                        if (hasGPS) {
                            intent.putExtra(INTENT_SENSOR_GPS, INTENT_TRUE);
                            intent_needed = true;
                        } else {
                            Log.w("USER_AGREEMENT", "DOESNT SUPPORT gps");
                        }
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW gps");
                    }
                } else if (i == Peripheral.PEEK_ENUM.humidity.ordinal()) {
                    if (cur_settings.getBoolean("humidity_agreement", true)) {
                        if (mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null) {
                            Log.w("sensor_debug", "adding humidity to intent");
                            intent.putExtra(INTENT_SENSOR_HUMIDITY, INTENT_TRUE);
                            intent_needed = true;
                        } else {
                            Log.w("USER_AGREEMENT", "DOESNT SUPPORT humidity");
                            failable_hw += "humidity";
                        }
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW humidity");
                        sensor_access += "humidity";
                    }
                } else if (i == Peripheral.PEEK_ENUM.pic.ordinal()) {
                    if (cur_settings.getBoolean("user_camera_agreement", true)) {
                        Log.w("sensor_debug", "doing picture sensor");
                        do_popup_pic();
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW pic");
                        sensor_access += "pic";
                    }
                } else if (i == Peripheral.PEEK_ENUM.text.ordinal()) {
                    if (cur_settings.getBoolean("user_input_agreement", true)) {
                        Log.w("sensor_debug", "doing text sensor");
                        do_popup_text();
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW input");
                        sensor_access += "input";
                    }
                } else if (i == Peripheral.PEEK_ENUM.ambiant.ordinal()) {
                    if (cur_settings.getBoolean("ambient_agreement", true)) {
                        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
                            Log.w("sensor_debug", "adding ambiant to intent");
                            intent.putExtra(INTENT_SENSOR_AMBIANT, INTENT_TRUE);
                            intent_needed = true;
                        } else {
                            Log.w("USER_AGREEMENT", "DOESNT SUPPORT ambiant");
                            failable_hw += "ambiant";
                        }
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW ambiant");
                        sensor_access += "ambiant";
                    }
                }
            }
        }

        //Check to see if the failure is ok based on peripheral_level
        Integer periph_level = Integer.parseInt(cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.level.ordinal()],2);
        boolean is_able = true;
        if (failable_hw.length() != 0 || sensor_access.length() != 0) { //so hw doesn't support some sensor or user doesn't allow some sensor
            if (periph_level == Peripheral.LEVEL_ENUM.REQ_NONE.ordinal() ||
                    periph_level == Peripheral.LEVEL_ENUM.REQ_ALL_NO_SENSORS.ordinal() ||
                    periph_level == Peripheral.LEVEL_ENUM.REQ_ALL_NO_SENSORS_NOT_INCL_GPS.ordinal() ||
                    periph_level == Peripheral.LEVEL_ENUM.REQ_NONE_BUT_CONNECTION.ordinal() ||
                    periph_level == Peripheral.LEVEL_ENUM.REQ_NONE_BUT_SERVICE.ordinal()) {
            } else {
                Log.w("IS_ABLE_FALSE", "PERIPHERAL LEVEL NOT ALIGNED");
                is_able = false;
            }
        }
        if (!hasGPS || !allowsGPS) { //for the req_all_no_sensors_not_incl_gps level
            if (periph_level == Peripheral.LEVEL_ENUM.REQ_NONE.ordinal() ||
                    periph_level == Peripheral.LEVEL_ENUM.REQ_ALL_NO_SENSORS.ordinal() ||
                    periph_level == Peripheral.LEVEL_ENUM.REQ_NONE_BUT_CONNECTION.ordinal() ||
                    periph_level == Peripheral.LEVEL_ENUM.REQ_NONE_BUT_SERVICE.ordinal() ) {
            }
            else {
                Log.w("IS_ABLE_FALSE", "PERIPHERAL LEVEL NOT ALIGNED GPS");
                is_able = false;
            }
        }

        if (periph_level == Peripheral.LEVEL_ENUM.REQ_NONE_BUT_SERVICE.ordinal()) {
            if (!isOnline()) {
                Log.w("IS_ABLE_FALSE", "PERIPHERAL LEVEL NOT ALIGNED SERVICE");
                is_able = false;
            }
        }


        Log.w("IS_ABLE_SENSORS", String.valueOf(is_able));

        if (is_able && intent_needed) {
            startService(intent);
            return true;
        } else if (is_able) {
            return true;
        }
        return false;
    }

    public void do_popup_pic() {
        Log.w(top, "do_popup_pic");
        take_picture();
    }

    public void do_popup_text() {
        Log.w(top, "do_popup_text");
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Peripheral Asked for Text");
        alert.setMessage("Enter Text:");
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                try {
                    jsonParams.put("s_text", value);
                } catch (JSONException d) {
                    Log.w("JSONException", "text");
                }
                Log.w("sensor_debug", value);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        alert.show();
    }

    private BroadcastReceiver mServiceMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(tag, intent.getStringExtra(INTENT_EXTRA_SENSOR_VAL));
        }
    };

    public void forward_data() {
        Log.w(top, "forward_data()");
        check_ack();
    }

    public void check_ack() {
        Log.w(top, "check_ack()");

        int tmp_switch = 0;
        switch (tmp_switch) {
            case 1:
                check_ack_able();
                schedule_ack();
                break;
            default: break;
        }
    }

    public void check_ack_able() {
        Log.w(top, "check_ack_able()");
    }

    public void schedule_ack() {
        Log.w(top, "schedule_ack()");
    }

    public void on_ack_callback() {
        Log.w(top, "on_ack_callback()");
        send_ack();
    }

    public void send_ack() {
        Log.w(top, "send_ack()");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gateway, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_demo) {
            Intent intent = new Intent(this, Demo.class);
            intent.putExtra("FINAL_STR", "");
            startActivityForResult(intent, 0);
            //startActivityForResult(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        paused = false;

        // Initializes list view adapter.
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        if (requestCode == DEMO_STR && resultCode == Activity.RESULT_OK) {
            final_str  = data.getStringExtra("FINAL_STR");
            Log.w(top, "HIT DEMO REQUEST");

            this.parse(); //FROM DEMO
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScanning = false;
        paused = true;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable & !paused & !mScanning) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(false);
                }
            }, SCAN_PERIOD/2);

            mScanning = true;
            try {mBluetoothAdapter.startLeScan(mLeScanCallback);} catch(Exception e) {e.printStackTrace();}
        } else {
            if (!paused)
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanLeDevice(true);
                    }
                }, SCAN_PERIOD/2);
            mScanning = false;
            try {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } catch (Exception e) {e.printStackTrace();}
        }
    }

    private void send_program(){
        try {
            Integer program_index = programValid.lastIndexOf(program_name_to_send);
            programJSONParams.put("program_name", program_name_to_send);
            programJSONParams.put("size", program_cur_packet_size);
            programJSONParams.put("total_size", programSizesTotal.get(program_index));

            StringEntity entity = new StringEntity(programJSONParams.toString());
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

            client.post(getBaseContext(), programURL.get(program_index), entity, "application/json", new AsyncHttpResponseHandler() {
                @Override public void onSuccess(int statusCode, org.apache.http.Header[] headers, byte[] responseBody) { }
                @Override public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, Throwable error) { }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                jsonParams.put("advertisement",getHexString(scanRecord));
                                jsonParams.put("device", (device.getName()!=null) ? device.getName() : "Unnamed" );
                                jsonParams.put("address", device.getAddress());
                                jsonParams.put("rssi", rssi);
                                System.out.println(getHexString(scanRecord));
                                StringEntity entity = new StringEntity(jsonParams.toString());

                                entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));



                                if (!program_name_to_send.equals("")) { //there is a program
                                    Integer program_index = programValid.lastIndexOf(program_name_to_send);
                                    program_cur_packet_size = entity.toString().getBytes().length; // TODO i'm not sure this is correct... the tostring changes the size... approx for now
                                    Integer cur_total_size = programSizesTotal.get(program_index);
                                    cur_total_size += program_cur_packet_size;
                                    programSizesTotal.set(program_index, cur_total_size);
                                    send_program();
                                }

                                client.post(getBaseContext(),"http://inductor.eecs.umich.edu:8081/SgYPCHTR5a", entity, "application/json", new AsyncHttpResponseHandler() {
                                    @Override public void onSuccess(int statusCode, org.apache.http.Header[] headers, byte[] responseBody) { }
                                    @Override public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, Throwable error) { }
                                });


                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            };


    public static String getHexString (byte[] buf)
    {
        StringBuffer sb = new StringBuffer();
        for (byte b:buf) sb.append(String.format("%X", b));
        return sb.toString();
    }
}
