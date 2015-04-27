package edu.umich.eecs.lab11.gateway;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;


public class UiList extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().add(android.R.id.content, new UiListFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ui_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            this.startActivity(new Intent(this, Gateway.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class UiListFragment extends ListFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private SharedPreferences preferences;
        private UiListAdapter uiListAdapter;
        private ArrayList<String> devices;
        private static UiListFragment instance = null;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if(preferences.getBoolean("master_agreement",false) && !GatewayService.isInstanceCreated()) {
                Intent gatewayIntent = new Intent(getActivity(),GatewayService.class);
                getActivity().startService(gatewayIntent);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            preferences.registerOnSharedPreferenceChangeListener(this);
            devices = new ArrayList<String>(preferences.getStringSet("ui_devices",new HashSet<String>()));
            uiListAdapter = new UiListAdapter();
            setListAdapter(uiListAdapter);
            instance = this;
        }

        @Override
        public void onPause() {
            super.onPause();
            preferences.unregisterOnSharedPreferenceChangeListener(this);
            devices.clear();
            instance = null;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            System.out.println("CHANGE : " + s);
            if (s.equals("ui_devices")) {
                devices = new ArrayList<String>(preferences.getStringSet(s,new HashSet<String>()));
                uiListAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            super.onListItemClick(l, v, position, id);
            String [] strings = ((String) uiListAdapter.getItem(position)).split(" @ ");
            String address = strings[0].split(" ")[0];
            String url = strings[1];
            String name = "";
            try {name = strings[0].split(" ")[1].replaceAll("[()]","");} catch (Exception e) {}
            popupWebView(address, name, url);
        }

        public static UiListFragment getInstance() {
            return instance;
        }

        public void popupWebView(String deviceAddress,String deviceName,String ui) {
            Intent intent = new Intent(getActivity(), WebViewActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("deviceAddress", deviceAddress);
            intent.putExtra("deviceName", deviceName);
            intent.putExtra("ui", ui);
            getActivity().startActivity(intent);
        }

        // Adapter for holding devices found through scanning.
        public class UiListAdapter extends BaseAdapter {

            private LayoutInflater inflater;

            public UiListAdapter() {
                super();
                inflater = getActivity().getLayoutInflater();
            }

            @Override
            public int getCount() {
                return devices.size();
            }

            @Override
            public Object getItem(int i) {
                return devices.get(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                // General ListView optimization code.
                if (view == null) view = inflater.inflate(android.R.layout.simple_list_item_2, null);
                String [] strings = ((String) getItem(i)).split(" @ ");
                try {
                    ((TextView) view.findViewById(android.R.id.text1)).setTextColor(Color.parseColor("#8BC34A"));
                    ((TextView) view.findViewById(android.R.id.text1)).setText(strings[0]);
                    ((TextView) view.findViewById(android.R.id.text2)).setText(strings[1]);
                } catch (Exception e) {}
                return view;
            }
        }

    }
}
