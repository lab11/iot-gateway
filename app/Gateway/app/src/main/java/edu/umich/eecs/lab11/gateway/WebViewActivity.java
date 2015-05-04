package edu.umich.eecs.lab11.gateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.JavascriptInterface;

import org.apache.cordova.CordovaActivity;

import java.util.Calendar;

public class WebViewActivity extends CordovaActivity {

    private String data;
    private String deviceAddress;
    private String deviceName;
    private String ui;
    private SharedPreferences cur_settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.init();
        super.appView.addJavascriptInterface(new JavaScriptInterface(super.getActivity()), "gateway");
        cur_settings = PreferenceManager.getDefaultSharedPreferences(this);
        data = getIntent().getStringExtra("data");
        deviceAddress = getIntent().getStringExtra("deviceAddress");
        deviceName = getIntent().getStringExtra("deviceName");
        ui = getIntent().getStringExtra("ui");
//        if (ui.length()==0) updateData(deviceName, data);
//        else webView.loadData(ui, "text/html", "UTF-8");
        loadUrl(ui);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cur_settings.edit().putBoolean("pause",false).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cur_settings.edit().putBoolean("pause",true).apply();
    }

    public class JavaScriptInterface {
        Context mContext;

        JavaScriptInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public String getDeviceId(){
            return deviceAddress;
        }

        @JavascriptInterface
        public String getDeviceName() { return deviceName; }
    }

    public void updateData(String deviceName, String data) {
        Calendar c = Calendar.getInstance();
        String customHtml = "<html><body style='font-family:sans-serif-thin; font-size:20pt;'>DEVICE:<br />"+deviceName+"<br /><br />DATA:<br />"+data+"<br /><br />RECEIVED:<br />"+ c.getTime().toString() +"</body></html>";
//        webView.loadData(customHtml, "text/html", "UTF-8");
    }

}
