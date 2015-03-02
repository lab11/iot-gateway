package edu.umich.eecs.lab11.gateway;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Gateway extends PreferenceActivity {

    private ArrayList<String> programValid = new ArrayList<String>();
    private ArrayList<Boolean> programCredentials = new ArrayList<Boolean>();
    private ArrayList<Integer> programMaxPay = new ArrayList<Integer>();
    private ArrayList<String> programURL = new ArrayList<String>();
    private ArrayList<Integer> programSizesTotal = new ArrayList<Integer>();

    private Map<String,BluetoothDevice> deviceMap = new HashMap<String, BluetoothDevice>();

    private Map<String,String> urlMap = new HashMap<String, String>();

    private PackageManager packageManager;

    private Peripheral cur_peripheral;

    private SensorManager mSensorManager;
    private HashMap<Integer,String> mSensors;

    private final static String INTENT_TRUE = "TRUE";
    private final static String INTENT_FALSE = "FALSE";

    private final static String INTENT_SENSOR_ACCEL = "ACCEL";
    private final static String INTENT_SENSOR_TEMP = "TEMP";
    private final static String INTENT_SENSOR_GPS = "GPS";
    private final static String INTENT_SENSOR_AMBIENT = "AMBIENT";
    private final static String INTENT_SENSOR_HUMIDITY = "HUMIDITY";

    private final static String INTENT_EXTRA_SENSOR_VAL = "SENSOR_VAL";

    private LocationManager locationManager;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private boolean mScanning;
    private Handler mScanHandler;
    private boolean paused;
    private static AsyncHttpClient client = new AsyncHttpClient();

    private HandlerThread mThread = new HandlerThread("mThread");
    private Handler mHandler;

    private final String tag = "tag";
    private final String top = "top";

    private long peek_time;

    private String program_name_to_send;
    private String program_pay_to_send;
    private String program_url_to_send;
    private Integer program_cur_packet_size;

    private String final_str;
    private String final_binary_str;

    private String popup_text_string = "";
    private String popup_pic_string = "";

    private boolean waitingForOffload;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private SharedPreferences cur_settings;

    private String adv_id;
    private String gateway_first_contact_time;
    private String gateway_transmit_time;
    private String gate_size_transmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new GatewayFragment()).commit();
        cur_settings = PreferenceManager.getDefaultSharedPreferences(this);

        cur_peripheral = new Peripheral();

        packageManager = getApplicationContext().getPackageManager();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        System.out.println("MAGIC : " + mSensorManager.getSensorList(Sensor.TYPE_ALL).toString());
        mSensors = new HashMap<Integer, String>();
        mSensors.put(Sensor.TYPE_AMBIENT_TEMPERATURE,"");
        mSensors.put(Sensor.TYPE_RELATIVE_HUMIDITY,"");
        mSensors.put(Sensor.TYPE_ACCELEROMETER,"");
        mSensors.put(Sensor.TYPE_LIGHT,"");

        programValid.add("PayMe");
        programValid.add("YOUPAY");
        programCredentials.add(true);
        programCredentials.add(true);
        programMaxPay.add(8);
        programMaxPay.add(15);
        programURL.add("www.payme.com/api");
        programURL.add("www.youpay.com/iot");
        programSizesTotal.add(0);
        programSizesTotal.add(0);

        program_name_to_send = "";
        program_pay_to_send = "";
        program_url_to_send = "";

        getSystemService(Context.LOCATION_SERVICE);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mScanHandler = new Handler();
        mThread.start();
        mHandler = new Handler(mThread.getLooper(), mHandlerCallback);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
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
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL,5000000);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY), SensorManager.SENSOR_DELAY_NORMAL,5000000);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL,5000000);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL,5000000);

        // Initializes list view adapter.
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScanning = false;
        paused = true;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mSensorManager.unregisterListener(mSensorListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap bm = (Bitmap) extras.get("data");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            popup_pic_string = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        }
        super.onActivityResult(requestCode, resultCode, data);
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


    public String hexToBinary(final String hexStr) {
        StringBuffer binStr = new StringBuffer();
        String[] conversionTable = {"0000", "0001", "0010", "0011", "0100", "0101", "0110", "0111", "1000", "1001", "1010", "1011", "1100", "1101", "1110", "1111"};

        for (int i = 0; i < hexStr.length(); i++) {
            binStr.append(conversionTable[Character.digit(hexStr.charAt(i), 16)]);
        }
        return binStr.toString();
    }



    public void parse(String devName, String devAddress, int rssi, String a) {
        Log.w(top, "parse()");

        if (!cur_settings.getBoolean("master_agreement", false)) {
            Log.w("POINT", "GATEWAY NOT ENABLED!");
            return; //BREAK OUT
        }
        cur_peripheral.empty(); //NEW PACKET

        String t = a.substring(2,4) + a.substring(0,2) + a.substring(4,32) + "    PARSE_:" + a.substring(32);
        Log.w("PARSING A REAL THING!", t);


        String IP = a.substring(2, 4) + a.substring(0,2) + a.substring(4,32);
        if (urlMap.get(IP) == null) {
            unshortUrl(IP); // resolve url for peripheral's next advertisement
            return;
        }

        final_binary_str = hexToBinary(a.substring(32));
        Log.w("PARSE_FINAL", final_binary_str);
        String TRANSPARENT = final_binary_str.substring(0, 1);
        String RATE = final_binary_str.substring(1, 4);
        String QOS = final_binary_str.substring(4, 8);
        String SENSORS = final_binary_str.substring(8, 16);
        String PROGRAM_TYPE = final_binary_str.substring(16, 20);
        String DATA = final_binary_str.substring(20);

        if (TRANSPARENT.equals("1")) { //DONE WITH TRANSPARENT BIT
            Log.w("POINT", "TRANSPARENT FORWARD");
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.ip_address.ordinal()] = IP;
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.rate.ordinal()] = RATE;
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.program_type.ordinal()] = PROGRAM_TYPE;
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.data_blob.ordinal()] = DATA;
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.dev_address.ordinal()] = devAddress;
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.dev_name.ordinal()] = devName;
            cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.qos.ordinal()] = QOS;
            cur_peripheral.TRANSPARENT = true;

        } else {
            Log.w("POINT", "PEEK FORWARD");
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.ip_address.ordinal()] = IP;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.rate.ordinal()] = RATE;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.qos.ordinal()] = QOS;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.accel.ordinal()] = String.valueOf(SENSORS.charAt(4)); //Jesus is this hacky... Hardcoded to match sensor order... Can change to peripheral.SENSOR_ENUM.x.ordinal()
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.temp.ordinal()] = String.valueOf(SENSORS.charAt(1));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.time.ordinal()] = String.valueOf(SENSORS.charAt(3));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.gps.ordinal()] = String.valueOf(SENSORS.charAt(0));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.humidity.ordinal()] = String.valueOf(SENSORS.charAt(2));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.pic.ordinal()] = String.valueOf(SENSORS.charAt(6));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.text.ordinal()] = String.valueOf(SENSORS.charAt(5));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.ambient.ordinal()] = String.valueOf(SENSORS.charAt(7));
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.program_type.ordinal()] = PROGRAM_TYPE;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.data_blob.ordinal()] = DATA;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.dev_address.ordinal()] = devAddress;
            cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.dev_name.ordinal()] = devName;
            cur_peripheral.TRANSPARENT = false;
        }

        //          debug cloud
        try {
            JSONObject gatdParams = new JSONObject();
            gatdParams.put("TYPE", "debug");
            gatdParams.put("DEVICE_ID", devAddress);
            gatdParams.put("NAME", devName);
            gatdParams.put("RSSI", rssi);
            gatdParams.put("DESTINATION", IP);
            gatdParams.put("TRANSPARENT",TRANSPARENT);
            gatdParams.put("RATE", RATE);
            gatdParams.put("QOS", QOS);
            if (TRANSPARENT.equals("0")) {
                gatdParams.put("SENSORS", SENSORS);
            }
            gatdParams.put("PROGRAM", PROGRAM_TYPE);
            gatdParams.put("DATA", DATA);
            StringEntity entity = new StringEntity(gatdParams.toString());
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            client.post(getBaseContext(),"http://gatd.eecs.umich.edu:8081/SgYPCHTR5a", entity, "application/json", new AsyncHttpResponseHandler() {
                @Override public void onSuccess(int statusCode, org.apache.http.Header[] headers, byte[] responseBody) { }
                @Override public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, Throwable error) { }
            });
        } catch (Exception e) {}

        run_forward(TRANSPARENT);

    }

    public boolean run_forward(String TRANSPARENT) {

        //DO SENSORS
        Integer peripheral_program_level;
        if (TRANSPARENT.equals("0")) { //DONE WITH TRANSPARENT BIT
            if (do_sensors()) { //all sensors were fine
                Log.w("POINT", "SENSORS DONE!");
                peripheral_program_level = Integer.parseInt(cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.program_type.ordinal()], 2);
            } else {
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
//        Log.w(top, "switch_grant()");

        Integer user_pay_floor = cur_settings.getInt("min_pay_rate", 0);

        Log.w("SWITCH_GRANT_prog_level", peripheral_program_level.toString());
        Log.w("SWITCH_GRANT_pay_floor", user_pay_floor.toString());


        if (peripheral_program_level == 0) { // no program supported by peripheral
            if (user_pay_floor == 0) { // user does not want monetary program
                schedule();
            } else {
                Log.w("POINT", "Program NOT SUPPORTED"); // peripheral can't pay
                return false;
            }
        } else if (peripheral_program_level == 15) { // every program supported by peripheral
            if (user_pay_floor != 0) { // user wants monetary program
                if (!do_monetary_program(peripheral_program_level))
                    return false; // peripheral can pay... max that sucker out
                schedule();
            } else {
                schedule(); //user doesn't care... send it altruistically
            }
        } else {  // some programs supported by peripheral
            if (peripheral_program_level >= user_pay_floor) { // the peripheral can support up to a certain amount of pay... user accepts
                if (!do_monetary_program(peripheral_program_level))
                    return false; //pay as much as the peripheral wants
                schedule();
            } else {
                Log.w("POINT", "Peripheral CAN'T PAY"); // peripheral can't pay
                return false;
            }
        }
        return true;
    }

    private boolean do_monetary_program(Integer pay_level) {
//        Log.w(top, "do_monetary_program(Integer pay_level)");
        String program_name = cur_settings.getString("program_text", "PayMe").trim();
        Log.w("program_debug", program_name);
        Integer program_index = programValid.lastIndexOf(program_name);
        //Log.w("program_debug", program_index.toString());
        Log.w("program_debug_pay_level", String.valueOf(pay_level));
        Log.w("program_debug_max_pay", String.valueOf(programMaxPay.get(program_index)));

        if (program_index != -1) {
            if (programCredentials.get(program_index)) {
                if (pay_level <= programMaxPay.get(program_index)) {
                    program_name_to_send = program_name;
                    program_pay_to_send = pay_level.toString();
                    program_url_to_send = programURL.get(program_index);
                } else {
                    Log.w("POINT", "Program CAN'T PAY PAY LEVEL"); // peripheral can't pay
                    return false;
                }
            } else {
                Log.w("POINT", "Program NOT CREDENTIALED"); // peripheral can't pay
                return false;
            }
        } else {
            Log.w("POINT", "Program NAME NOT FOUND"); // peripheral can't pay
            return false;
        }
        Log.w("POINT", "Program OK");
        return true;
    }

    private void schedule() {
//        Log.w(top, "schedule()");
//        Log.w("POINT", "scheduling is not implemented");
        post();
    }

    private void post() {

        // data cloud
        try {
            final JSONObject gatdDataParams = new JSONObject();
            gatdDataParams.put("TYPE", "data");
            String IP;
            if (cur_peripheral.TRANSPARENT) {
                gatdDataParams.put("DEVICE_ID", cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.dev_address.ordinal()]);
                gatdDataParams.put("NAME", cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.dev_name.ordinal()]);
                gatdDataParams.put("DATA", cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.data_blob.ordinal()]);
                gatdDataParams.put("QOS", cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.qos.ordinal()]);
                IP = cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.ip_address.ordinal()];
            } else {
                for (int i = 0; i < cur_peripheral.DATA_TO_PEEK.size(); i++) {
                    String[] key_val = cur_peripheral.DATA_TO_PEEK.get(i).split(" ");
                    gatdDataParams.put(key_val[0], key_val[1]);
                }
                gatdDataParams.put("DEVICE_ID", cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.dev_address.ordinal()]);
                gatdDataParams.put("NAME", cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.dev_name.ordinal()]);
                gatdDataParams.put("DATA", cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.data_blob.ordinal()]);
                gatdDataParams.put("QOS", cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.qos.ordinal()]);
                IP = cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.ip_address.ordinal()];
            }
            final StringEntity entity = new StringEntity(gatdDataParams.toString());
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            String url = urlMap.get(IP);
            org.apache.http.Header[] headers = {
                new BasicHeader("DEVICE_ID",gatdDataParams.getString("DEVICE_ID")),
                new BasicHeader("QOS",gatdDataParams.getString("QOS"))
            };
            Log.i("POSTING_DATA", gatdDataParams.toString());
            System.out.println("SENDING TO : " + url);
            client.post(getBaseContext(),url,headers,entity,"application/json", asyncResponder);


            if (!program_name_to_send.equals("")) { //there is a program
                Log.i("PROGRAM_NAME", program_name_to_send);
                Integer program_index = programValid.lastIndexOf(program_name_to_send);
                program_cur_packet_size = entity.toString().getBytes().length; // TODO i'm not sure this is correct... the tostring changes the size... approx for now
                Integer cur_total_size = programSizesTotal.get(program_index);
                cur_total_size += program_cur_packet_size;
                programSizesTotal.set(program_index, cur_total_size);
                send_program();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private AsyncHttpResponseHandler asyncResponder = new TextHttpResponseHandler() {
        @Override
        public void onFailure(int statusCode, org.apache.http.Header[] headers, String responseString, Throwable throwable) { System.out.println("FAILED REQUEST : " + responseString); }

        @Override
        public void onSuccess(int statusCode, org.apache.http.Header[] headers, final String responseString) {
            BluetoothDevice device = deviceMap.get(getRequestHeaders()[0].getValue());
            Integer qos = Integer.parseInt(getRequestHeaders()[1].getValue(), 2);
            System.out.println("RESPONSE : " + responseString);
            if (responseString.length()<2) return;
            try {
                final JSONObject responseJSON = new JSONObject(responseString);
                if (qos >= 7 && responseJSON.has("services") && responseJSON.has("device_id") && device.getAddress().equals(responseJSON.getString("device_id")))
                    if (mHandler.sendMessage(Message.obtain(mHandler,0,0,0,responseJSON)) &&
                        mHandler.sendMessage(Message.obtain(mHandler,0,1,0,getRequestHeaders())) &&
                        mHandler.sendMessage(Message.obtain(mHandler,0,2,0,getRequestURI())))
                            mHandler.sendMessage(Message.obtain(mHandler,0,3,0,device));
            } catch(Exception e){ mBluetoothGatt.disconnect(); }
        }
    };

    Handler.Callback mHandlerCallback = new Handler.Callback() {
        JSONArray services;
        JSONArray charArray = new JSONArray();
        ArrayList<BluetoothGattCharacteristic> characteristicsList;
        ArrayList<String> actionsList;
        ArrayList<BluetoothGattCharacteristic> notifyList;
        BluetoothDevice device;
        org.apache.http.Header[] headers;
        URI uri;
        Boolean servicesDiscovered;
        Long notifyDeadline;


        @Override
        public boolean handleMessage(Message message) {
            try {
                if (message.what==0) {
                    if (message.arg1 == 0) services = ((JSONObject) message.obj).getJSONArray("services");
                    else if (message.arg1 == 1) headers = (org.apache.http.Header[]) message.obj;
                    else if (message.arg1 == 2) uri = (URI) message.obj;
                    else if (message.arg1 == 3) {
                        device = (BluetoothDevice) message.obj;
                        if (mBluetoothGatt!=null && mBluetoothManager.getConnectionState(device,BluetoothProfile.GATT)==BluetoothProfile.STATE_CONNECTED)
                            mHandler.sendMessage(Message.obtain(mHandler, 1, mBluetoothGatt));
                        else {
                            servicesDiscovered = false;
                            notifyList = new ArrayList<BluetoothGattCharacteristic>();
                            mBluetoothGatt = device.connectGatt(getBaseContext(), false, gattCallback);
                        }
                    }
                } else if (message.what==1) {
                    if (servicesDiscovered)  mHandler.sendMessage(Message.obtain(mHandler, 2, message.obj));
                    else ((BluetoothGatt) message.obj).discoverServices();
                } else if (message.what==2) {
                    BluetoothGatt gatt = (BluetoothGatt) message.obj;
                    servicesDiscovered = true;
                    for (int i = 0; i < services.length(); i++) {
                        String service = services.getJSONObject(i).getString("uuid");
                        BluetoothGattService bgs = gatt.getService(UUID.fromString(service));
                        if (bgs != null) {
                            JSONArray characteristics = services.getJSONObject(i).getJSONArray("characteristics");
                            characteristicsList = new ArrayList<BluetoothGattCharacteristic>();
                            actionsList = new ArrayList<String>();
                            for (int j = 0; j < characteristics.length(); j++) {
                                try {
                                    JSONObject charObject = characteristics.getJSONObject(j);
                                    BluetoothGattCharacteristic characteristic = bgs.getCharacteristic(UUID.fromString(charObject.getString("uuid")));
                                    String action = charObject.getString("action");
                                    if (action.equals("write"))
                                        if (charObject.has("format")) {
                                            Integer format = GattSpecs.formatLookup(charObject.getString("format"));
                                            if (format!=null) {
                                                if (format>0) characteristic.setValue(charObject.getInt("value"),format, charObject.has("offset")?charObject.getInt("offset"):0);
                                                else if (format==-1) characteristic.setValue(charObject.getString("value"));
                                                else if (format==-2) characteristic.setValue((byte []) charObject.get("value"));
                                            }
                                        } else characteristic.setValue(charObject.getString("value"));
                                    characteristicsList.add(characteristic);
                                    actionsList.add(action);
                                } catch (Exception e) { e.printStackTrace(); }
                            }
                        }
                    }
                    characteristicAction(gatt, 0);
                } else if (message.what==3) {
                    if (message.arg1 < 2) {
                        BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) message.obj;
                        final JSONObject chara = new JSONObject();
                        try {
                            chara.put("service", characteristic.getService().getUuid().toString());
                            chara.put("characteristic", characteristic.getUuid().toString());
                            chara.put("value", getHexString(characteristic.getValue()));
                        } catch (Exception e) {e.printStackTrace();}
                        charArray.put(chara);
                        characteristicAction(mBluetoothGatt,characteristicsList.indexOf(characteristic)+1);
                    } else if (message.arg1 == 2) {
                        notifyList.add((BluetoothGattCharacteristic) message.obj);
                        Calendar.getInstance().getTimeInMillis();
                    }
                } else if (message.what==4) {
                    if (notifyList.contains(message.obj)) ;
                }
                return true;
            } catch (Exception e) {e.printStackTrace(); return false;}
        }

        private void characteristicAction(BluetoothGatt gatt, Integer index) {
            if (index < characteristicsList.size()) {
                Boolean actionSuccess = false;
                BluetoothGattCharacteristic characteristic = characteristicsList.get(index);
                String action = actionsList.get(index);
                if (action.equals("read")) {
                    actionSuccess = gatt.readCharacteristic(characteristic);
                } else if (action.equals("write")) {
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    actionSuccess = gatt.writeCharacteristic(characteristicsList.get(index));
                } else if (action.equals("notify") || action.equals("indicate")) {
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(GattSpecs.CCC_DESCRIPTOR);
                    gatt.setCharacteristicNotification(characteristic, true);
                    descriptor.setValue(action.equals("notify") ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    actionSuccess = gatt.writeDescriptor(descriptor);
                }
                if (!actionSuccess) characteristicAction(gatt, index + 1);
            } else {
                try {
                    final JSONObject reResponse = new JSONObject("{\"DEVICE_ID\":\"" + device.getAddress() + "\"}");
                    reResponse.put("ATTRIBUTES", charArray);
                    System.out.println("RSEND : " + reResponse);
                    final StringEntity entity = new StringEntity(reResponse.toString());
                    entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                    client.post(getBaseContext(), uri.toString(), headers, entity, "application/json", asyncResponder);
                } catch (Exception e) { e. printStackTrace();}
                gatt.disconnect();
            }
        }
    };

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(tag, "Connected to GATT server.");
                mHandler.sendMessage(Message.obtain(mHandler,1,gatt));
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(tag, "Disconnected from GATT server.");
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(tag, "Service Discovery Successful: ");
                mHandler.sendMessage(Message.obtain(mHandler,2,gatt));
            } else {
                Log.w(tag, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            mHandler.sendMessage(Message.obtain(mHandler,3,0,0,characteristic));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            mHandler.sendMessage(Message.obtain(mHandler,3,1,0,characteristic));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            mHandler.sendMessage(Message.obtain(mHandler,4,characteristic));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (descriptor.getUuid().equals(GattSpecs.CCC_DESCRIPTOR)) mHandler.sendMessage(Message.obtain(mHandler,3,2,0,descriptor.getCharacteristic()));
        }
    };

    public boolean hasGPSDevice(Context context) {
        final LocationManager mgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mgr == null) return false;
        final List<String> providers = mgr.getAllProviders();
        if (providers == null) return false;
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

        for (int i = Peripheral.PEEK_ENUM.gps.ordinal(); i <= Peripheral.PEEK_ENUM.ambient.ordinal(); i++) {
//            Log.w("sensor_debug", "TEST");
            if (cur_peripheral.PEEK_FLAGS[i].equals("1")) {
                if (i == Peripheral.PEEK_ENUM.accel.ordinal()) {
                    if (cur_settings.getBoolean("accel_agreement", true)) {
                        Log.w("sensor_debug", "adding accel to intent");
                        String key_val = "ACCELEROMETER ";
                        String sensor = mSensors.get(Sensor.TYPE_ACCELEROMETER);
                        key_val += sensor;
                        if (sensor.length()>0) cur_peripheral.DATA_TO_PEEK.add(key_val);
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW ACCEL");
                        sensor_access += "accel";
                    }
                } else if (i == Peripheral.PEEK_ENUM.time.ordinal()) {
                    if (cur_settings.getBoolean("time_agreement", true)) {
                        Log.w("sensor_debug", "adding time to intent");
                        String key_val = "GWTIME ";
                        key_val += System.currentTimeMillis();
                        cur_peripheral.DATA_TO_PEEK.add(key_val);
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW TIME");
                        sensor_access += "time";
                    }
                } else if (i == Peripheral.PEEK_ENUM.temp.ordinal()) {
                    if (cur_settings.getBoolean("temp_agreement", true)) {
                        Log.w("sensor_debug", "adding temp to intent");
                        String key_val = "TEMPERATURE ";
                        String sensor = mSensors.get(Sensor.TYPE_AMBIENT_TEMPERATURE);
                        key_val += sensor;
                        if (sensor.length()>0) cur_peripheral.DATA_TO_PEEK.add(key_val);
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW temp");
                        sensor_access += "temp";
                    }
                } else if (i == Peripheral.PEEK_ENUM.gps.ordinal()) {
                    if (cur_settings.getBoolean("gps_agreement", true)) {
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
                } else if (i == Peripheral.PEEK_ENUM.humidity.ordinal()) {
                    if (cur_settings.getBoolean("humidity_agreement", true)) {
                        Log.w("sensor_debug", "adding humidity to intent");
                        String key_val = "HUMIDITY ";
                        String sensor = mSensors.get(Sensor.TYPE_RELATIVE_HUMIDITY);
                        key_val += sensor;
                        if (sensor.length()>0) cur_peripheral.DATA_TO_PEEK.add(key_val);
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW humidity");
                        sensor_access += "humidity";
                    }
                } else if (i == Peripheral.PEEK_ENUM.pic.ordinal()) {
                    if (cur_settings.getBoolean("user_camera_agreement", true)) {
                        Log.w("sensor_debug", "doing picture sensor");
                        do_popup_pic();
                        String key_val = "IMAGE ";
                        key_val += popup_pic_string;
                        Log.w("sensor_debug", popup_pic_string);
                        if (popup_pic_string.length()>0) {
                            cur_peripheral.DATA_TO_PEEK.add(key_val);
                            Log.w("sensor_debug", popup_pic_string);
                        }
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW pic");
                        sensor_access += "pic";
                    }
                } else if (i == Peripheral.PEEK_ENUM.text.ordinal()) {
                    if (cur_settings.getBoolean("user_input_agreement", true)) {
                        Log.w("sensor_debug", "doing text sensor");
                        do_popup_text();
                        String key_val = "TEXT ";
                        key_val += popup_text_string;
                        if (popup_text_string.length()>0) {
                            cur_peripheral.DATA_TO_PEEK.add(key_val);
                            Log.w("sensor_debug", popup_text_string);
                        }
                    } else {
                        Log.w("USER_AGREEMENT", "DOESNT ALLOW input");
                        sensor_access += "input";
                    }
                } else if (i == Peripheral.PEEK_ENUM.ambient.ordinal()) {
                    if (cur_settings.getBoolean("ambient_agreement", true)) {
                        Log.w("sensor_debug", "adding ambient to intent");
                        String key_val = "LIGHT ";
                        String sensor = mSensors.get(Sensor.TYPE_LIGHT);
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
        Integer periph_qos = Integer.parseInt(cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.qos.ordinal()], 2);
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
            startService(intent);
            return true;
        } else if (is_able) {
            return true;
        }
        return false;
    }

    public void do_popup_pic() {
        Log.w(top, "do_popup_pic");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    public void do_popup_text() {
        Log.w(top, "do_popup_text");

        mScanning = false;
        paused = true;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Peripheral Asked for Text");
        alert.setMessage("Enter Text:");
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                popup_text_string = value;
                mScanning = true;
                paused = false;
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                popup_text_string = "";
                mScanning = true;
                paused = false;
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
        });
        alert.show();
    }

    private SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public final void onAccuracyChanged(Sensor sensor, int accuracy) { }

        @Override
        public final void onSensorChanged(SensorEvent event) {
            Integer type = event.sensor.getType();
            switch (type) {
                case Sensor.TYPE_AMBIENT_TEMPERATURE: mSensors.put(type,event.values[0]+"C"); break;
                case Sensor.TYPE_RELATIVE_HUMIDITY: mSensors.put(type,event.values[0]+"%"); break;
                case Sensor.TYPE_ACCELEROMETER: mSensors.put(type,"["+event.values[0]+","+event.values[1]+","+event.values[2]+"]m/s^2"); break;
                case Sensor.TYPE_LIGHT: mSensors.put(type,event.values[0]+"lx"); break;
            }
        }
    };

    private BroadcastReceiver mServiceMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(tag, intent.getStringExtra(INTENT_EXTRA_SENSOR_VAL));
        }
    };

    public void check_ack() {
        Log.w(top, "check_ack()");

        int tmp_switch = 0;
        switch (tmp_switch) {
            case 1:
                check_ack_able();
                schedule_ack();
                break;
            default:
                break;
        }
    }

    public void check_ack_able() {
        Log.w(top, "check_ack_able()");
    }

    public void schedule_ack() {
        Log.w(top, "schedule_ack()");
    }

    public void send_ack() {
        Log.w(top, "send_ack()");
    }

    private void scanLeDevice(final boolean enable) {
        if (enable & !paused & !mScanning) {
            // Stops scanning after a pre-defined scan period.
            mScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(false);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            try {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (!paused)
                mScanHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanLeDevice(true);
                    }
                }, 1000);
            mScanning = false;
            try {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void send_program() {
        Log.w(top, "send_program()");
        Log.i("SENDING_PROGRAM", "send_program()");
        JSONObject gatdProgramParams = new JSONObject();
        Integer program_index = programValid.lastIndexOf(program_name_to_send);
        try {
            gatdProgramParams.put("TYPE", "incentive");
            gatdProgramParams.put("PROGRAM_IP", program_url_to_send);
            gatdProgramParams.put("PROGRAM_NAME", program_name_to_send);
            gatdProgramParams.put("PROGRAM_PAY", program_pay_to_send);
            gatdProgramParams.put("TOTAL_SIZE", programSizesTotal.get(program_index));
            gatdProgramParams.put("THIS_SIZE", program_cur_packet_size);
            if (cur_peripheral.TRANSPARENT) {
                gatdProgramParams.put("DEVICE_ID", cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.dev_address.ordinal()]);
                gatdProgramParams.put("NAME", cur_peripheral.TRANSPARENT_FLAGS[Peripheral.TRANSPARENT_ENUM.dev_name.ordinal()]);
            } else {
                gatdProgramParams.put("DEVICE_ID", cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.dev_address.ordinal()]);
                gatdProgramParams.put("NAME", cur_peripheral.PEEK_FLAGS[Peripheral.PEEK_ENUM.dev_name.ordinal()]);
            }
            StringEntity entity = new StringEntity(gatdProgramParams.toString());
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            client.post(getBaseContext(),"http://gatd.eecs.umich.edu:8081/SgYPCHTR5a", entity, "application/json", new AsyncHttpResponseHandler() {
                @Override public void onSuccess(int statusCode, org.apache.http.Header[] headers, byte[] responseBody) { }
                @Override public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, Throwable error) { }
            });
        } catch (Exception e) {}
        Log.i("POSTING_PROGRAM", gatdProgramParams.toString());
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
                                Log.d("DEBUG", device.getName() + " : " + getHexString(scanRecord));
                                parseStuff(device, rssi, scanRecord);
                            } catch (Exception e) { }
                        }
                    });
                }
            };

    public static String getHexString(byte[] buf) {
        if (buf != null) {
            StringBuffer sb = new StringBuffer();
            for (byte b : buf) sb.append(String.format("%02X", b));
            return sb.toString();
        } else return "";
    }

    private void unshortUrl(String shortUrlHex) {
        // shortUrlHex is hexstring -> convert to ascii first, then unshorten
        StringBuilder shortUrl = new StringBuilder();
        for (int i = 0; i < shortUrlHex.length(); i+=2) {
            String str = shortUrlHex.substring(i, i + 2);
            if (!str.equals("00")) shortUrl.append((char) Integer.parseInt(str, 16));
        }
        new unshortTask().execute(shortUrlHex,shortUrl.toString());
    }

    private class unshortTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... shortUrls) {
            try {
                URL url = new URL("http://" + shortUrls[1]);
                HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
                ucon.setInstanceFollowRedirects(false);
                String location = ucon.getHeaderField("Location");
                System.out.println("RESOLVED LOCATION : " + location);
                if (location!=null) urlMap.put(shortUrls[0],location);
                return true;
            } catch (Exception e) { e.printStackTrace(); return false; }
        }
    }

    public void parseStuff(BluetoothDevice device, int rssi, byte[] scanRecord) {
        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index++];
//Done once we run out of records
            if (length == 0) break;

            int type = scanRecord[index];
//Done if our record isn't a valid type
            if (type == 0) break;

            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);
            if (type==22) {
                Log.w("MATCH", "Type matched, submitting for parsing: " + device.getName());
                deviceMap.put(device.getAddress(), device);
                parse(device.getName(), device.getAddress(), rssi, getHexString(data));
            }
//Advance
            index += length;
        }
    }

}