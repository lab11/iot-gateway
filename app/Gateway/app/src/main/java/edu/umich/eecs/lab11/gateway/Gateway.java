package edu.umich.eecs.lab11.gateway;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.location.LocationManager;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Toast;
import com.loopj.android.http.*;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;






public class Gateway extends Activity {

    private enum AccessLevels {
        ALL, NO_GPS, FINE_GPS,
        MID_GPS, NO_TIME, NO_DATA,
        ONLY_PROGRAM, NO_WIFI, NO_PROGRAM, MID_PROGRAM
    }

    private ArrayList<String> validPrograms = new ArrayList<String>();
    private ArrayList<Boolean> programCredentials = new ArrayList<Boolean>();


    private Settings cur_settings = new Settings();

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private boolean paused;
    LocationManager locationManager;
    private static AsyncHttpClient client = new AsyncHttpClient();

    private boolean waitingForOffload;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        validPrograms.add("IPAY");
        programCredentials.add(true);
        validPrograms.add("YOUPAY");
        programCredentials.add(false);

        getSystemService(Context.LOCATION_SERVICE);

        setContentView(R.layout.activity_gateway);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new GatewayFragment())
                    .commit();
        }
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


    public void buildFinalPacket() {
        //if no offloading and have required network
            //send packet
        //if don't have required networking and offloading is done
            // raise alarm
            // if dtn enabled
                //queue packet
            // else
                //drop packet
        //if have required networking and offloading is done
            // send packet
    }

    public void doFlags(String msg) {



    }

    public void doConfig() {
        // READ ALL SLIDERS AND SET CONFIGS

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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class GatewayFragment extends Fragment {

        public GatewayFragment() {
        }

        private WebView myWebView;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_gateway, container, false);
            myWebView = (WebView) v.findViewById(R.id.webView);
            WebSettings webSettings = myWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            myWebView.loadUrl("file:///android_asset/index.html");
            return v;
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
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.print(device);
                            JSONObject jsonParams = new JSONObject();
                            try {
                                jsonParams.put("device", (device.getName()!=null) ? device.getName() : "Unnamed" );
                                jsonParams.put("address", device.getAddress());
                                jsonParams.put("rssi", rssi);
                                StringEntity entity = new StringEntity(jsonParams.toString());
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

}
