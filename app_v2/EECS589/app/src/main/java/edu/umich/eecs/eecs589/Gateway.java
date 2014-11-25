package edu.umich.eecs.eecs589;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.hardware.SensorEventListener;
import android.location.LocationManager;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;
import com.loopj.android.http.*;

import android.util.Log;

import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import android.hardware.SensorManager;

public class Gateway extends Activity {

    public edu.umich.eecs.eecs589.Settings cur_settings;

    private Switch GW_masterBTN;
    private Switch GW_GPS_FINEBTN;
    private Switch GW_GPS_MIDBTN;
    private Switch GW_GPS_FARBTN;
    private Switch GW_timeBTN;
    private Switch GW_accelBTN;
    private Switch GW_user_textBTN;
    private Switch GW_user_pictureBTN;
    private Switch GW_ambiantBTN;
    private Switch GW_monBTN;
    private Switch GW_nonMonBTN;
    private SeekBar GW_dataBAR;
    private SeekBar GW_reliabilityBAR;
    private EditText GW_programTEXT;

    private ArrayList<String> validPrograms = new ArrayList<String>();
    private ArrayList<Boolean> programCredentials = new ArrayList<Boolean>();

    public Peripheral cur_peripheral = new Peripheral();

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private boolean paused;
    LocationManager locationManager;
    private static AsyncHttpClient client = new AsyncHttpClient();

    private final String tag = "tag";
    private final String top = "top";

    private String final_str;
    private String final_binary_str;

    private boolean waitingForOffload;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int DEMO_STR = 0;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private void initSettings() {
        doAmbiantBTN_config();
        doNonMon_config();
        doMon_config();
        doMasterBTN_config();
        doGPSFineBTN_config();
        doGPSMidBTN_config();
        doGPSFarBTN_config();
        doTimeBTN_config();
        doAccelBTN_config();
        doUser_TextBTN_config();
        doUser_PhotoBTN_config();
        cur_settings.setDataRate(GW_dataBAR.getProgress());
        cur_settings.setReliability(GW_reliabilityBAR.getProgress());
        cur_settings.setProgramText(GW_programTEXT.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        validPrograms.add("IPAY");
        programCredentials.add(true);
        validPrograms.add("YOUPAY");
        programCredentials.add(false);

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
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cur_settings = new edu.umich.eecs.eecs589.Settings();
        GW_masterBTN = (Switch) findViewById(R.id.GW_masterBTN);
        GW_GPS_FINEBTN = (Switch) findViewById(R.id.GW_GPS_FINEBTN);
        GW_GPS_MIDBTN = (Switch) findViewById(R.id.GW_GPS_MIDBTN);
        GW_GPS_FARBTN = (Switch) findViewById(R.id.GW_GPS_FARBTN);
        GW_timeBTN = (Switch) findViewById(R.id.GW_timeBTN);
        GW_accelBTN = (Switch) findViewById(R.id.GW_accelBTN);
        GW_user_textBTN = (Switch) findViewById(R.id.GW_User_InputBTN);
        GW_user_pictureBTN = (Switch) findViewById(R.id.GW_User_PhotoBTN);
        GW_ambiantBTN  = (Switch) findViewById(R.id.GW_ambientBTN);
        GW_monBTN  = (Switch) findViewById(R.id.GW_MONBTN);
        GW_nonMonBTN  = (Switch) findViewById(R.id.GW_NONMONBTN);
        GW_programTEXT = (EditText) findViewById(R.id.GW_programTEXT);
        GW_dataBAR = (SeekBar) findViewById(R.id.GW_dataSLIDER);
        GW_reliabilityBAR = (SeekBar) findViewById(R.id.GW_reliabilitySLIDER);

        GW_masterBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doMasterBTN_config();
            }
        });
        GW_GPS_FINEBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doGPSFineBTN_config();
            }
        });
        GW_GPS_MIDBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doGPSMidBTN_config();
            }
        });
        GW_GPS_FARBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doGPSFarBTN_config();
            }
        });
        GW_timeBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doTimeBTN_config();
            }
        });
        GW_accelBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doAccelBTN_config();
            }
        });
        GW_user_textBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doUser_TextBTN_config();
            }
        });
        GW_user_pictureBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doUser_PhotoBTN_config();
            }
        });
        GW_ambiantBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doAmbiantBTN_config();
            }
        });
        GW_nonMonBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doNonMon_config();
            }
        });
        GW_monBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doMon_config();
            }
        });
        GW_programTEXT.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                Log.w(top, GW_programTEXT.getText().toString());
                cur_settings.setProgramText(GW_programTEXT.getText().toString());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        GW_dataBAR.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.w(top, String.valueOf(seekBar.getProgress()));
                cur_settings.setDataRate(seekBar.getProgress());
            }
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        GW_reliabilityBAR.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.w(top, String.valueOf(seekBar.getProgress()));
                cur_settings.setReliability(seekBar.getProgress());
            }
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        initSettings();
    }

    public ArrayList<Integer> getGPS() {
        ArrayList<Integer> cur_loc = new ArrayList<Integer>();

        return cur_loc;
    }

    public long getTime() {
        return System.currentTimeMillis();
    }

    public boolean findValidProgram(String program) {
        int find = validPrograms.lastIndexOf(program);
        return checkProgramCredentials(find);
    }

    public boolean checkProgramCredentials(Integer index) {
        return programCredentials.get(index);
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
        if (!cur_settings.master_agreement) {
            Log.w("BREAK", "GATEWAY NOT ENABLED!");
            return; //BREAK OUT
        }

        final_binary_str = hexToBinary(final_str);
        Log.w("FINAL_STR", final_str);

        String IP = final_binary_str.substring(0,64);
        IP = String.format("%21X", Long.parseLong(IP,2));
        Log.w("IP", IP);

        String TRANSPARENT = final_binary_str.substring(64,65);
        Log.w("TRANSPARENT", TRANSPARENT);

        String RATE = final_binary_str.substring(65,68);
        Log.w("RATE", RATE);

        String LEVEL = final_binary_str.substring(68,71);
        Log.w("LEVEL", LEVEL);

        String SENSORS = final_binary_str.substring(71,79);
        Log.w("SENSORS", SENSORS);

        String PROGRAM_NEED = final_binary_str.substring(79,80);
        Log.w("PROGRAM_NEED", PROGRAM_NEED);

        String PROGRAM_TYPE = final_binary_str.substring(80,84);
        //PROGRAM_TYPE = String.format("%21X", Long.parseLong(PROGRAM_TYPE,2));
        Log.w("PROGRAM_TYPE", PROGRAM_TYPE);

        String DATA = final_binary_str.substring(84);
        //DATA = String.format("%21X", Long.parseLong(DATA,2));
        Log.w("DATA", DATA);

        if (TRANSPARENT == "1") { //DONE WITH TRANSPARENT BIT
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.ip_address.ordinal()] = IP;
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.rate.ordinal()] = RATE;
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.data_blob.ordinal()] = DATA;
            if (isSensorAble()){
                do_sensors();
            } else {
                Log.w("BREAK", "SENSORS NOT ABLE!");
                return;
            }
            switch_grant();
        } else {
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.ip_address.ordinal()] = IP;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.rate.ordinal()] = RATE;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.level.ordinal()] = LEVEL;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.sensors.ordinal()] = SENSORS;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.program_need.ordinal()] = PROGRAM_NEED;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.program_type.ordinal()] = PROGRAM_TYPE;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.data_blob.ordinal()] = DATA;
            switch_grant();
        }
    }

    public void do_sensors() {

    }

    public boolean isSensorAble() {

        return false;
    }

    public void switch_grant() {

    }

    public void start() {

        Log.w(top, "start()");
        //Log.w(top, cur_settings.prettyString());


        Integer tmp_switch = cur_settings.reliabilityRate;

        Log.w(top, tmp_switch.toString());

        switch(tmp_switch) {
            case 0:
                Log.w(top, "SWITCH1: 0");
                break;
            case 1:
                Log.w(top, "SWITCH1: 1");
                check_program();
                check_if_grant_able();
                break;
            case 2:
                Log.w(top, "SWITCH1: 2");
                check_if_grant_able();
                break;
            default: break;
        }
        //switch_gateway_mode();
    }

    public void check_if_grant_able() {
        Log.w(top, "check_if_grant_able()");

    }

    public void check_program() {
        Log.w(top, "check_program()");

    }

    public void switch_gateway_mode() {
        Log.w(top, "switch_gateway_mode()");

        int tmp_switch = 0;
        switch (tmp_switch) {
            case 1:
                break;
            case 2:
                break;
            default: break;
        }
        schedule();
        forward_data();
    }

    public void schedule() {
        Log.w(top, "schedule()");
    }

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
        getMenuInflater().inflate(R.menu.home, menu);
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

    public void doAmbiantBTN_config(){
        Log.w(top, "doAmbiantBTN_config()");
        boolean cur_state = GW_ambiantBTN.isChecked();
        cur_settings.setAmbient(cur_state);
    }
    public void doNonMon_config(){
        Log.w(top, "doNonMon_config()");
        boolean cur_state = GW_nonMonBTN.isChecked();
        cur_settings.setNonMon(cur_state);
    }
    public void doMon_config(){
        Log.w(top, "doMon_config()");
        boolean cur_state = GW_monBTN.isChecked();
        cur_settings.setMon(cur_state);
    }
    public void doMasterBTN_config(){
        Log.w(top, "doMasterBTN_config()");
        boolean cur_state = GW_masterBTN.isChecked();

        cur_settings.setMaster(cur_state);
    }
    public void doGPSFineBTN_config(){
        Log.w(top, "doGPSFineBTN_config()");
        boolean cur_state = GW_GPS_FINEBTN.isChecked();
        cur_settings.setGPS_fine(cur_state);
    }
    public void doGPSMidBTN_config(){
        Log.w(top, "doGPSMidBTN_config()");
        boolean cur_state = GW_GPS_MIDBTN.isChecked();
        cur_settings.setGPS_mid(cur_state);
    }
    public void doGPSFarBTN_config(){
        Log.w(top, "doGPSFarBTN_config()");
        boolean cur_state = GW_GPS_FARBTN.isChecked();
        cur_settings.setGPS_far(cur_state);
    }
    public void doTimeBTN_config(){
        Log.w(top, "doTimeBTN_config()");
        boolean cur_state = GW_timeBTN.isChecked();
        cur_settings.setTime(cur_state);
    }
    public void doAccelBTN_config(){
        Log.w(top, "doAccelBTN_config()");
        boolean cur_state = GW_accelBTN.isChecked();
        cur_settings.setAccel(cur_state);
    }
    public void doUser_TextBTN_config(){
        Log.w(top, "doUser_TextBTN_config()");
        boolean cur_state = GW_user_textBTN.isChecked();
        cur_settings.setUser_input(cur_state);
    }
    public void doUser_PhotoBTN_config(){
        Log.w(top, "doUser_PhotoBTN_config()");
        boolean cur_state = GW_user_pictureBTN.isChecked();
        cur_settings.setUser_camera(cur_state);
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

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSONObject jsonParams = new JSONObject();
                            try {
                                jsonParams.put("advertisement",getHexString(scanRecord));
                                jsonParams.put("device", (device.getName()!=null) ? device.getName() : "Unnamed" );
                                jsonParams.put("address", device.getAddress());
                                jsonParams.put("rssi", rssi);
                                System.out.println(getHexString(scanRecord));
                                StringEntity entity = new StringEntity(jsonParams.toString());

                                /*
                                if (UUID_parse(device.t)) {
                                    ADV_parse(device);
                                    run();
                                }
                                */


                                entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                                client.post(getBaseContext(),"http://inductor.eecs.umich.edu:8081/SgYPCHTR5a", entity, "application/json", new AsyncHttpResponseHandler() {
                                    @Override
                                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                                    }
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                                    }
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


/*
    public boolean UUID_parse(String msg) {
        Log.w(top, "UUID_parse()");
        cur_peripheral.UUID_FLAGS[Peripheral.UUID_Fields.transparent_flag.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.UUID_FLAGS[Peripheral.UUID_Fields.rate.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.UUID_FLAGS[Peripheral.UUID_Fields.level.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.UUID_FLAGS[Peripheral.UUID_Fields.sensors.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.UUID_FLAGS[Peripheral.UUID_Fields.program_need.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.UUID_FLAGS[Peripheral.UUID_Fields.program_type.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.UUID_FLAGS[Peripheral.UUID_Fields.UUID_REST.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        return false;
    }

    public void ADV_parse(String msg) {
        Log.w(top, "ADV_parse()");
        cur_peripheral.ADV_FLAGS[Peripheral.ADV_Fields.ip_address.ordinal()] =
                Integer.getInteger(msg.substring(0,127));
        cur_peripheral.ADV_FLAGS[Peripheral.ADV_Fields.data_blob.ordinal()] =
                Integer.getInteger(msg.substring(128,0));
    }

    public void SENSOR_parse(String msg) {
        Log.w(top, "SENSOR_parse()");
        cur_peripheral.SENSOR_FLAGS[Peripheral.Sensor_Fields.GPS_FINE.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.SENSOR_FLAGS[Peripheral.Sensor_Fields.GPS_MID.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.SENSOR_FLAGS[Peripheral.Sensor_Fields.GPS_FAR.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.SENSOR_FLAGS[Peripheral.Sensor_Fields.time.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.SENSOR_FLAGS[Peripheral.Sensor_Fields.accel.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.SENSOR_FLAGS[Peripheral.Sensor_Fields.user_text.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.SENSOR_FLAGS[Peripheral.Sensor_Fields.user_picture.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
        cur_peripheral.SENSOR_FLAGS[Peripheral.Sensor_Fields.ambiant_light.ordinal()] =
                Integer.getInteger(msg.substring(0,0));
    }
 */

