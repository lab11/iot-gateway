package edu.umich.eecs.lab11.gateway;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GatewayService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private ArrayList<String>  programURL         = new ArrayList<String> (Arrays.asList("http://payme.com/api","http://youpay.com/iot"));
    private ArrayList<String>  programValid       = new ArrayList<String> (Arrays.asList("PayMe","YOUPAY"));
    private ArrayList<Integer> programMaxPay      = new ArrayList<Integer>(Arrays.asList(8,15));
    private ArrayList<Integer> programSizesTotal  = new ArrayList<Integer>(Arrays.asList(0,0));
    private ArrayList<Boolean> programCredentials = new ArrayList<Boolean>(Arrays.asList(true,true));

    private Map<String,String>          urlMap    = new HashMap<String, String>();
    private Map<String,BluetoothDevice> deviceMap = new HashMap<String, BluetoothDevice>();

    private PhoneServices phoneServices;

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

    private String program_name_to_send = "";
    private String program_pay_to_send = "";
    private String program_url_to_send = "";
    private Integer program_cur_packet_size;

    private Integer radioSilenceCount = 0;

    // Pauses scanning after SCAN_PERIOD milliseconds, restarts 1 second later
    private final Integer SCAN_PERIOD = 10000;
    private SharedPreferences cur_settings;

    private ArrayList<BluetoothDevice> deviceConnected = new ArrayList<BluetoothDevice>();
    private static Intent thisIntent;

    private  static GatewayService instance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        cur_settings = PreferenceManager.getDefaultSharedPreferences(this);
        phoneServices = new PhoneServices(getApplicationContext());

        mScanHandler = new Handler();
        mThread.start();
        mHandler = new Handler(mThread.getLooper(), mHandlerCallback);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            stopSelf();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            try {
                mBluetoothAdapter.enable();
            } catch (Exception e) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getBaseContext().startActivity(enableBtIntent);
            }
        }
        paused = false;
        thisIntent = intent;
        phoneServices.start();

        Toast.makeText(this, "Gateway Service Started", Toast.LENGTH_SHORT).show();
        cur_settings.registerOnSharedPreferenceChangeListener(this);

        // Initializes list view adapter.
        scanLeDevice(true);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mScanning = false;
        paused = true;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        cur_settings.unregisterOnSharedPreferenceChangeListener(this);
        phoneServices.stop();
        instance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isInstanceCreated() { return instance != null; }

    public static Intent getIntent() { return thisIntent; }

    public void parse(String devName, String devAddress, int rssi, String a) {
        Log.w(top, "parse()");

        String IP = a.substring(0,28);
        String fullURL = urlMap.get(IP);
        if (fullURL == null) {
            unshortUrl(IP); // resolve url for peripheral's next advertisement
            return;
        }

        String t = fullURL + "    PARSE_:" + a.substring(28);
        Log.w("API MATCH!", t);

        Peripheral cur_peripheral = new Peripheral(devName,devAddress,rssi,a,fullURL);

        //          debug cloud
        try {
            JSONObject gatdParams = new JSONObject();
            gatdParams.put("TYPE", "debug");
            gatdParams.put("DEVICE_ID", cur_peripheral.FLAGS[Peripheral.ENUM.dev_address.ordinal()]);
            gatdParams.put("NAME", cur_peripheral.FLAGS[Peripheral.ENUM.dev_name.ordinal()]);
            gatdParams.put("RSSI", rssi);
            gatdParams.put("DESTINATION", fullURL);
//            gatdParams.put("TRANSPARENT",cur_peripheral.TRANSPARENT);
//            gatdParams.put("RATE", cur_peripheral.FLAGS[Peripheral.ENUM.rate.ordinal()]);
            gatdParams.put("QOS", cur_peripheral.FLAGS[Peripheral.ENUM.qos.ordinal()]);
            if (!cur_peripheral.TRANSPARENT) gatdParams.put("SENSORS", cur_peripheral.FLAGS[Peripheral.ENUM.sensors.ordinal()]);
            gatdParams.put("PROGRAM", cur_peripheral.FLAGS[Peripheral.ENUM.program_type.ordinal()]);
            gatdParams.put("DATA", cur_peripheral.FLAGS[Peripheral.ENUM.data_blob.ordinal()]);
            StringEntity entity = new StringEntity(gatdParams.toString());
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            client.post(getBaseContext(),"http://gatd.eecs.umich.edu:8081/SgYPCHTR5a", entity, "application/json", new AsyncHttpResponseHandler() {
                @Override public void onSuccess(int statusCode, org.apache.http.Header[] headers, byte[] responseBody) { }
                @Override public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, Throwable error) { }
            });
        } catch (Exception e) { e.printStackTrace(); }

        run_forward(cur_peripheral);

    }

    public boolean run_forward(Peripheral cur_peripheral) {

        //DO SENSORS
        Integer peripheral_program_level;
        if (!cur_peripheral.TRANSPARENT && !phoneServices.getData(cur_peripheral)) { //DONE WITH TRANSPARENT BIT
            Log.w("POINT", "SENSORS NOT ABLE!");
            return false;
        } else {
            Log.w("POINT", "SENSORS DONE!");
            peripheral_program_level = Integer.parseInt(cur_peripheral.FLAGS[Peripheral.ENUM.program_type.ordinal()], 2);
        }
        switch_grant(cur_peripheral,peripheral_program_level);
        return true;
    }

    public boolean switch_grant(Peripheral cur_peripheral, Integer peripheral_program_level) {
//        Log.w(top, "switch_grant()");

        Integer user_pay_floor = cur_settings.getInt("min_pay_rate", 0);

        Log.w("SWITCH_GRANT_prog_level", peripheral_program_level.toString());
        Log.w("SWITCH_GRANT_pay_floor", user_pay_floor.toString());


        if (peripheral_program_level == 0) { // no program supported by peripheral
            if (user_pay_floor == 0) { // user does not want monetary program
                schedule(cur_peripheral);
            } else {
                Log.w("POINT", "Program NOT SUPPORTED"); // peripheral can't pay
                return false;
            }
        } else if (peripheral_program_level == 15) { // every program supported by peripheral
            if (user_pay_floor != 0) { // user wants monetary program
                if (!do_monetary_program(peripheral_program_level))
                    return false; // peripheral can pay... max that sucker out
                schedule(cur_peripheral);
            } else {
                schedule(cur_peripheral); //user doesn't care... send it altruistically
            }
        } else {  // some programs supported by peripheral
            if (peripheral_program_level >= user_pay_floor) { // the peripheral can support up to a certain amount of pay... user accepts
                if (!do_monetary_program(peripheral_program_level))
                    return false; //pay as much as the peripheral wants
                schedule(cur_peripheral);
            } else {
                Log.w("POINT", "Peripheral CAN'T PAY"); // peripheral can't pay
                return false;
            }
        }
        return true;
    }

    private boolean do_monetary_program(Integer pay_level) {
        String program_name = cur_settings.getString("program_text", "PayMe").trim();
        Log.w("program_debug", program_name);
        Integer program_index = programValid.lastIndexOf(program_name);
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

    private void schedule(Peripheral cur_peripheral) {
        // implement scheduling
        post(cur_peripheral);
    }

    private void post(Peripheral cur_peripheral) {
        // data cloud
        try {
            final JSONObject gatdDataParams = new JSONObject();
            gatdDataParams.put("TYPE", "data");
            if (!cur_peripheral.TRANSPARENT) {
                for (int i = 0; i < cur_peripheral.DATA_TO_PEEK.size(); i++) {
                    String[] key_val = cur_peripheral.DATA_TO_PEEK.get(i).split(" ");
                    gatdDataParams.put(key_val[0], key_val[1]);
                }
            }
            gatdDataParams.put("DEVICE_ID", cur_peripheral.FLAGS[Peripheral.ENUM.dev_address.ordinal()]);
            gatdDataParams.put("NAME", cur_peripheral.FLAGS[Peripheral.ENUM.dev_name.ordinal()]);
            gatdDataParams.put("DATA", cur_peripheral.FLAGS[Peripheral.ENUM.data_blob.ordinal()]);
            gatdDataParams.put("QOS", cur_peripheral.FLAGS[Peripheral.ENUM.qos.ordinal()]);
            String url = cur_peripheral.FLAGS[Peripheral.ENUM.url.ordinal()];
            final StringEntity entity = new StringEntity(gatdDataParams.toString());
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            org.apache.http.Header[] headers = {
                    new BasicHeader("DEVICE_ID",gatdDataParams.getString("DEVICE_ID")),
                    new BasicHeader("QOS",gatdDataParams.getString("QOS"))
            };
            if (cur_peripheral.TRANSPARENT || cur_peripheral.FLAGS[Peripheral.ENUM.ui.ordinal()].equals("0")) {
                Log.i("POSTING_DATA", gatdDataParams.toString());
                System.out.println("SENDING TO : " + url);
                client.post(getBaseContext(), url, headers, entity, "application/json", asyncResponder);
            }

            if (!program_name_to_send.equals("")) { //there is a program
                Log.i("PROGRAM_NAME", program_name_to_send);
                Integer program_index = programValid.lastIndexOf(program_name_to_send);
                program_cur_packet_size = entity.toString().getBytes().length; // TODO i'm not sure this is correct... the tostring changes the size... approx for now
                Integer cur_total_size = programSizesTotal.get(program_index);
                cur_total_size += program_cur_packet_size;
                programSizesTotal.set(program_index, cur_total_size);
                send_program(cur_peripheral);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private AsyncHttpResponseHandler asyncResponder = new TextHttpResponseHandler() {
        @Override
        public void onFailure(int statusCode, org.apache.http.Header[] headers, String responseString, Throwable throwable) { System.out.println("FAILED REQUEST : " + responseString); }

        @Override
        public void onSuccess(int statusCode, org.apache.http.Header[] headers, final String responseString) {
            BluetoothDevice device = deviceMap.get(getRequestHeaders()[0].getValue()); System.out.println(getRequestHeaders()[0].getValue());
            Integer qos = Integer.parseInt(getRequestHeaders()[1].getValue(), 2);
            System.out.println("RESPONSE : " + responseString);
            try {
                final JSONObject responseJSON = new JSONObject(responseString);
                if (!( qos >= 7 && responseJSON.has("services") && responseJSON.has("device_id") && device.getAddress().equals(responseJSON.getString("device_id")) &&
                        mHandler.sendMessage(Message.obtain(mHandler, 0, 0, 0, responseJSON)) && mHandler.sendMessage(Message.obtain(mHandler,0,1,0,getRequestHeaders())) &&
                        mHandler.sendMessage(Message.obtain(mHandler,0,2,0,getRequestURI())) && mHandler.sendMessage(Message.obtain(mHandler, 0, 3, 0, device)) ))
                    throw new Exception();
            } catch (Exception e) {
                if(device!=null &&  mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT)==BluetoothProfile.STATE_CONNECTED) {
                    mBluetoothGatt.disconnect();
                }
            }
        }
    };

    Handler.Callback mHandlerCallback = new Handler.Callback() {
        JSONArray services;
        JSONArray charArray = new JSONArray();
        ArrayList<BluetoothGattCharacteristic> characteristicsList;
        ArrayList<String> actionsList;
        Map<BluetoothGattCharacteristic,JSONArray> notifyList;
        BluetoothDevice device;
        org.apache.http.Header[] headers;
        URI uri;
        Boolean ccUri = false;
        Boolean servicesDiscovered;
        Boolean notifying;
        Boolean scheduled;
        //** MAP FOR OPO CLOUDCOMM - FROM NOTIFYING CHARACTERISTIC TO DEFRAG MANAGING CHARACTERISTIC **/
        Map<BluetoothGattCharacteristic,BluetoothGattCharacteristic> ccManagerMap;
        String dataBuilder;
        Integer count;
        //** END: MAP FOR OPO CLOUDCOMM - FROM NOTIFYING CHARACTERISTIC TO DEFRAG MANAGING CHARACTERISTIC **/

        @Override
        public boolean handleMessage(Message message) {
            try {
                if (message.what==0) { // Action from HTTP response. Connect device if not already.
                    if (message.arg1 == 0) services = ((JSONObject) message.obj).getJSONArray("services");
                    else if (message.arg1 == 1) headers = (org.apache.http.Header[]) message.obj;
                    else if (message.arg1 == 2) uri = (URI) message.obj;
                    else if (message.arg1 == 3) {
                        device = (BluetoothDevice) message.obj;
                        scheduled = false;
                        ccManagerMap = new HashMap<BluetoothGattCharacteristic, BluetoothGattCharacteristic>();
                        count=0;
                        if (mBluetoothGatt!=null && (mBluetoothManager.getConnectionState(device,BluetoothProfile.GATT)==BluetoothProfile.STATE_CONNECTED))
                            mHandler.sendMessage(Message.obtain(mHandler, 1, mBluetoothGatt));
                        else {
                            notifying = false;
                            ccUri = false;
                            dataBuilder = "";
                            notifyList = new HashMap<BluetoothGattCharacteristic, JSONArray>();
                            servicesDiscovered = false;
                            mScanning = false; paused = true;
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            mBluetoothGatt = device.connectGatt(getBaseContext(), false, gattCallback);
                        }
                    }
                } else if (message.what==1) { // Discover services if undiscovered.
                    if (servicesDiscovered)  mHandler.sendMessage(Message.obtain(mHandler, 2, message.obj));
                    else ((BluetoothGatt) message.obj).discoverServices();
                } else if (message.what==2) { // Get requested characteristics. Prepare for & initiate action.
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
                                    /** SET UP FOR OPO CLOUDCOMM SERVICE REQUESTS **/
                                    if ((action.equals("indicate")||action.equals("notify"))&&charObject.has("ccmanager_uuid")) {
                                        BluetoothGattCharacteristic ccmanager = bgs.getCharacteristic(UUID.fromString(charObject.getString("ccmanager_uuid")));
                                        ccmanager.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0); //TODO: change write value from 1 to 0
                                        ccManagerMap.put(characteristic, ccmanager);
                                    }
                                    /** END: SET UP FOR OPO CLOUDCOMM SERVICE REQUESTS **/

                                } catch (Exception e) { e.printStackTrace(); }
                            }
                        }
                    }
                    characteristicAction(gatt);
                } else if (message.what==3) { // On read, write, or notify, store in JSON object to send back to server
                    BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) message.obj;
                    if (!characteristicsList.isEmpty() && characteristicsList.get(0).equals(characteristic)) {
                        if (message.arg2 == BluetoothGatt.GATT_SUCCESS) {
                            if (message.arg1 < 2) {
                                final JSONObject chara = new JSONObject();
                                try {
                                    chara.put("service", characteristic.getService().getUuid().toString());
                                    chara.put("characteristic", characteristic.getUuid().toString());
                                    chara.put("value", getHexString(characteristic.getValue()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                charArray.put(chara);
                            } else if (message.arg1 == 2) {
                                notifyList.put(characteristic, new JSONArray());
                                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                                mBluetoothGatt.writeCharacteristic(characteristic);
//                                notifyDeadline = Calendar.getInstance().getTimeInMillis() + 5000;
                            }
                        }
                        actionsList.remove(characteristicsList.indexOf(characteristic));
                        characteristicsList.remove(characteristic);
                        /** INITIAL DEFRAG MANAGER WRITE FOR OPO CLOUDCOMM **/
                        if (ccManagerMap.containsKey(characteristic)) {
                            BluetoothGattCharacteristic ccmanager = ccManagerMap.get(characteristic);
                            ccmanager.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            mBluetoothGatt.writeCharacteristic(ccmanager);
                        } else
                        /** END: INITIAL DEFRAG MANAGER WRITE FOR OPO CLOUDCOMM **/
                            characteristicAction(mBluetoothGatt);
                    } else if (message.arg2==BluetoothGatt.GATT_SUCCESS && notifyList.containsKey(characteristic)) {
                        /** DEFRAG MANAGER WRITE ACK FOR OPO CLOUDCOMM **/
                        if (ccManagerMap.containsKey(characteristic)) {
                            byte[] cData = characteristic.getValue();
                            int seq_num = cData[0];
                            if(seq_num < 0) seq_num += 256;
                            System.out.println(getHexString(cData));
                            dataBuilder += getHexString(cData).substring(2);
                            if (seq_num == 255) {
                                if (!ccUri) try{ uri = URI.create(hexToAscii(dataBuilder)); ccUri=true; } catch (Exception e) {e.printStackTrace();}
                                else notifyList.get(characteristic).put(dataBuilder);
                                dataBuilder="";
                            }
                            BluetoothGattCharacteristic ccmanager = ccManagerMap.get(characteristic);
                            ccmanager.setValue(seq_num, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            ccmanager.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            mBluetoothGatt.writeCharacteristic(ccmanager);
                            count++;
                        } else {
                            /** END: DEFRAG MANAGER WRITE ACK FOR OPO CLOUDCOMM **/
                            notifyList.get(characteristic).put(getHexString(characteristic.getValue()));
                        }
                    }
                    /** CONTINUE NORMAL STUFF AFTER INITIAL DEFRAG MANAGER WRITE FOR OPO CLOUDCOMM **/
                    else if (ccManagerMap.containsValue(characteristic)) characteristicAction(mBluetoothGatt);
                    /** END: CONTINUE NORMAL STUFF AFTER INITIAL DEFRAG MANAGER WRITE FOR OPO CLOUDCOMM **/
                } else if (message.what==4) {
                    if (notifyList.containsKey(message.obj)) mBluetoothGatt.readCharacteristic((BluetoothGattCharacteristic) message.obj);
                } else if (message.what==5) {
                    BluetoothGatt gatt = (BluetoothGatt) message.obj;
                    if (deviceConnected.size()>2) deviceConnected.get(0).connectGatt(getBaseContext(),false,gattCallback);
                    else { paused=false; scanLeDevice(true); }
                    gatt.close();
                    if (notifying) send();
                }
                return true;
            } catch (Exception e) {e.printStackTrace(); return false;}
        }

        private void characteristicAction(BluetoothGatt gatt) {
            if (characteristicsList.size()>0) {
                Boolean actionSuccess = false;
                BluetoothGattCharacteristic characteristic = characteristicsList.get(0);
                String action = actionsList.get(0);
                if (action.equals("read")) {
                    actionSuccess = gatt.readCharacteristic(characteristic);
                } else if (action.equals("write")) {
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    actionSuccess = gatt.writeCharacteristic(characteristic);
                } else if (action.equals("notify") || action.equals("indicate")) {
                    try {
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(GattSpecs.CCC_DESCRIPTOR);
                        gatt.setCharacteristicNotification(characteristic, true);
                        descriptor.setValue(action.equals("notify") ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        actionSuccess = gatt.writeDescriptor(descriptor);
                        notifying = true;
                    } catch (Exception e) {actionSuccess=false;}
                }
                if (!actionSuccess) {
                    characteristicsList.remove(0);
                    actionsList.remove(0);
                    characteristicAction(gatt);
                }
            } else if (!scheduled) {
                final Handler handler = new Handler();
                if(!notifying) send();
                scheduled = true;
            }
        }

        private void send() {
            try {
                final JSONObject reResponse = new JSONObject("{\"DEVICE_ID\":\"" + device.getAddress() + "\"}");
                for (BluetoothGattCharacteristic characteristic : notifyList.keySet()) {
                    final JSONObject chara = new JSONObject();
                    try {
                        chara.put("service", characteristic.getService().getUuid().toString());
                        chara.put("characteristic", characteristic.getUuid().toString());
                        chara.put("value", notifyList.get(characteristic));
                    } catch (Exception e) { e.printStackTrace(); }
                    charArray.put(chara);
                }
                reResponse.put("ATTRIBUTES", charArray);
                System.out.println("RSEND : " + reResponse);
                System.out.println("TO : " + uri.toString());
                final StringEntity entity = new StringEntity(reResponse.toString());
                entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                client.post(getBaseContext(), uri.toString(), headers, entity, "application/json", asyncResponder);
                charArray = new JSONArray();
            } catch (Exception e) { e. printStackTrace();}
        }
    };

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(tag, "Connected to GATT server.");
                mHandler.sendMessage(Message.obtain(mHandler, 1, gatt));
                if (deviceConnected.size()>2) deviceConnected.remove(0);
                if (!deviceConnected.contains(gatt.getDevice())) deviceConnected.add(gatt.getDevice());
                System.out.println("DEVICES : " + deviceConnected.toString());
            } else {//if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(tag, "Disconnected from GATT server.");
                mHandler.sendMessage(Message.obtain(mHandler, 5, gatt));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(tag, "Service Discovery Successful: ");
                mHandler.sendMessage(Message.obtain(mHandler,2,gatt));
            } else Log.w(tag, "Service Discovery Failure: " + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.w(tag, "Read From Characteristic: " + characteristic.getUuid());
            mHandler.sendMessage(Message.obtain(mHandler,3,0,status,characteristic));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.w(tag, "Write To Characteristic: " + characteristic.getUuid());
            mHandler.sendMessage(Message.obtain(mHandler,3,1,status,characteristic));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.w(tag, "Characteristic Change: " + characteristic.getUuid());
            mHandler.sendMessage(Message.obtain(mHandler,4,characteristic));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.w(tag, "Set Up Notifications: " + descriptor.getCharacteristic().getUuid());
            if (descriptor.getUuid().equals(GattSpecs.CCC_DESCRIPTOR)) mHandler.sendMessage(Message.obtain(mHandler,3,2,status,descriptor.getCharacteristic()));
        }
    };

    public boolean hasGPSDevice(Context context) {
        final LocationManager mgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mgr == null) return false;
        final List<String> providers = mgr.getAllProviders();
        return providers != null && providers.contains(LocationManager.GPS_PROVIDER);
    }


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

    private void send_program(Peripheral cur_peripheral) {
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
            gatdProgramParams.put("DEVICE_ID", cur_peripheral.FLAGS[Peripheral.ENUM.dev_address.ordinal()]);
            gatdProgramParams.put("NAME", cur_peripheral.FLAGS[Peripheral.ENUM.dev_name.ordinal()]);
            StringEntity entity = new StringEntity(gatdProgramParams.toString());
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            client.post(getBaseContext(),"http://gatd.eecs.umich.edu:8081/SgYPCHTR5a", entity, "application/json", new AsyncHttpResponseHandler() {
                @Override public void onSuccess(int statusCode, org.apache.http.Header[] headers, byte[] responseBody) { }
                @Override public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, Throwable error) { }
            });
        } catch (Exception e) { }
        Log.i("POSTING_PROGRAM", gatdProgramParams.toString());
    }

    private void scanLeDevice(final boolean enable) {
        if (enable & !paused & !mScanning) {
            mScanHandler.postDelayed(new Runnable() { @Override public void run() { scanLeDevice(false); } }, SCAN_PERIOD);
            try { mScanning = mBluetoothAdapter.startLeScan(mLeScanCallback); } catch (Exception e) { e.printStackTrace(); }
        } else {
            if (!paused) {
                if (radioSilenceCount++ > 6) {
                    mScanHandler.postDelayed(new Runnable() {
                        @Override public void run() { startService(thisIntent); scanLeDevice(true);  }
                    }, SCAN_PERIOD);
                    mBluetoothAdapter.disable();
                    stopSelf();
                    System.out.println("PERIPHERALS ARE NOT BEING SCANNED. RESTARTING BLUETOOTH.");

                } else
                mScanHandler.postDelayed(new Runnable() { @Override public void run() { scanLeDevice(true); } }, 1000);

            }

            try {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
            } catch (Exception e) { e.printStackTrace(); }

        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            try {
                radioSilenceCount=0;
                Log.d("DEBUG", device.getName() + " : " + getHexString(scanRecord));
                /** TESTING FOR OPO TODO: REMOVE **/
                if (device.getName()!=null && device.getName().equals("Cloudcomm")) {
                    parse(device.getName(), device.getAddress(), rssi, "676F6F2E676C2F6A524D78453000770000");
                    deviceMap.put(device.getAddress(), device);
                }
                /** END: TESTING FOR OPO **/
                /** TESTING FOR ROBOSMART TODO: REMOVE **/
                else if (device.getName()!=null && device.getName().contains("SHL ")) parse(device.getName(),device.getAddress(),rssi,"676F6F2E676C2F35475147396B007702");
                /** END: TESTING FOR ROBOSMART **/
            apiCheck(device, rssi, scanRecord);
            } catch (Exception e) { e.printStackTrace(); }
        }
    };

    public static String getHexString(byte[] buf) {
        if (buf != null) {
            StringBuilder sb = new StringBuilder();
            for (byte b : buf) sb.append(String.format("%02X", b));
            return sb.toString();
        } else return "";
    }

    public static String hexToAscii(String hex) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hex.length(); i+=2) {
            String str = hex.substring(i, i+2);
            output.append((char)Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    private String unshortUrl(String shortUrlHex) {
        // shortUrlHex is hexstring -> convert to ascii first, then unshorten
        StringBuilder shortUrl = new StringBuilder();
        for (int i = 0; i < shortUrlHex.length(); i+=2) {
            String str = shortUrlHex.substring(i, i + 2);
            if (!str.equals("00")) shortUrl.append((char) Integer.parseInt(str, 16));
        }
        new unshortTask().execute(shortUrlHex,shortUrl.toString());
        return shortUrl.toString();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals("pause")) {
            if (cur_settings.getBoolean("pause", false)) {
                mScanning = false;
                paused = true;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                paused = false;
                scanLeDevice(true);
            }

        }
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

    public void apiCheck(BluetoothDevice device, int rssi, byte[] scanRecord) {
        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index++];
            if (length == 0) break; //Done once we run out of records
            int type = scanRecord[index];
            if (type == 0) break; //Done if our record isn't a valid type
            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);
            if ((type==0x13 || type==0x16) && data[0]>=' ' && data.length>2) {
                Log.w("MATCH", "Type matched, submitting for parsing: " + device.getName());
                deviceMap.put(device.getAddress(), device);
                String d = getHexString(data);
                parse(device.getName(), device.getAddress(), rssi, (type==0x13) ? d : (d.substring(2, 4) + d.substring(0,2) + d.substring(4)) );
            }
            index += length; //Advance
        }
    }

}