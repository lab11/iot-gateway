package edu.umich.eecs.lab11.gateway;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.util.ArrayList;

public class Gateway extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ArrayList<String> validPrograms = new ArrayList<String>();
    private ArrayList<Boolean> programCredentials = new ArrayList<Boolean>();

    private Peripheral cur_peripheral = new Peripheral();

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

    private SharedPreferences cur_settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new GatewayFragment()).commit();
        cur_settings = PreferenceManager.getDefaultSharedPreferences(this);

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
        if (!cur_settings.getBoolean("master_agreement",false)) {
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


        Integer tmp_switch = cur_settings.getInt("reliability_rate",0);

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

                                entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
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


