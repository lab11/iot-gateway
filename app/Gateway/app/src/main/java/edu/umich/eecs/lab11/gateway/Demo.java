package edu.umich.eecs.lab11.gateway;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

public class Demo extends PreferenceActivity {

    //public char[] finalStr;
    public String final_str;
    public String tag = "tag";

    private SharedPreferences cur_settings;

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
    public void onHeaderClick(Header header, int position) {
        super.onHeaderClick(header, position);
        if (header.id == R.id.gen_btn) {
            doGenBTN();
        }
        if (header.id == R.id.send_btn) {
            doSendBTN();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cur_settings = PreferenceManager.getDefaultSharedPreferences(this);

    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    @Override
    public void onBuildHeaders(List <Header> target) {
        loadHeadersFromResource(R.xml.activity_demo, target);

        for(Header header: target) {
            if (header.title.equals("GENERATE")) {
                String summary = "Final String: " + final_str;
                header.summary = summary;
            }
        }

    }

    public String boolToStr(Boolean btn_state) {
        String res;
        if (btn_state) {
            return res = "1";
        } else return res = "0";
    }

    public void doSendBTN() {
        Log.w(tag, "doSendBTN");
        if (!final_str.equals("")) {
            setResult(Activity.RESULT_OK,
                    new Intent().putExtra("FINAL_STR", final_str));
            finish();
        } else {
            finish();
        }
    }

    public String boolToStr(boolean state) {
        if (state) return "1";
        return "0";
    }

    public void doGenBTN() {
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

        /*
        Log.w("DEMO_VAL_IP", IPTEXT);
        Log.w("DEMO_VAL_TRANSPARENT", transparentBTN);
        Log.w("DEMO_VAL_RATE", rateTEXT);
        Log.w("DEMO_VAL_LEVEL", levelTEXT);
        Log.w("DEMO_VAL_GPS", gpsBTN);
        Log.w("DEMO_VAL_TEMP", tempBTN);
        Log.w("DEMO_VAL_HUMID", humidityBTN);
        Log.w("DEMO_VAL_TIME", timeBTN);
        Log.w("DEMO_VAL_ACCL", accelBTN);
        Log.w("DEMO_VAL_TEXT", textBTN);
        Log.w("DEMO_VAL_PIC", userPicBTN);
        Log.w("DEMO_VAL_AMBIENT", ambientBTN);
        Log.w("DEMO_VAL_PROGRAM", programTEXT);
        Log.w("DEMO_VAL_DATA", dataTEXT);
        */


        rateTEXT = Integer.toString(Integer.valueOf(rateTEXT.replaceAll("\\s+","")), 2);
        if (rateTEXT.length() != 3) {
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


        invalidateHeaders();



        //this.finalTEXT.setText(final_str);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_demo) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



}
