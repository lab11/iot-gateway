package edu.umich.eecs.lab11.gateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class StartOnBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences cur_settings = PreferenceManager.getDefaultSharedPreferences(context);
        if(cur_settings.getBoolean("master_agreement",false)) {
            Intent serviceIntent = new Intent(context,GatewayService.class);
            context.startService(serviceIntent);
        }
    }
}
