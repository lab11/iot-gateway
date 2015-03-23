package edu.umich.eecs.lab11.gateway;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by nklugman on 11/19/14.
 */
public class Peripheral {

    public String[] FLAGS;
    public ArrayList<String> DATA_TO_PEEK = new ArrayList<String>();
    public boolean TRANSPARENT;

    public enum QOS_ENUM {
        REQ_ALL, REQ_ALL_NO_RATE,
        REQ_ALL_NO_SENSORS, REQ_ALL_NO_SENSORS_NOT_INCL_GPS,
        REQ_NONE_BUT_CONNECTION, REQ_NONE_BUT_SERVICE, REQ_NONE
    }

    public enum ENUM {
        url, transparent, rate, sensors, qos,
        time, gps, accel, ambient, text, pic, ui, ip,
        program_need, program_type,
        data_blob, dev_address, dev_name
    }

    public enum SENSOR_ENUM {
        time, gps, accel, ambient, text, pic, ui, ip
    }

    public void empty() {
        FLAGS = new String[50];
        DATA_TO_PEEK.clear();
    }

    public String hexToBinary(final String hexStr) {
        StringBuilder binStr = new StringBuilder();
        String[] conversionTable = {"0000", "0001", "0010", "0011", "0100", "0101", "0110", "0111", "1000", "1001", "1010", "1011", "1100", "1101", "1110", "1111"};
        for (int i = 0; i < hexStr.length(); i++) binStr.append(conversionTable[Character.digit(hexStr.charAt(i), 16)]);
        return binStr.toString();
    }

    public Peripheral() {
        //UUID_FLAGS = new int[7];
        //SENSOR_FLAGS = new int[8];
        //ADV_FLAGS = new int[2];
        FLAGS = new String[50];
//        TRANSPARENT = false;
    }

    public Peripheral(String devName, String devAddress, int rssi, String a, String fullURL) {
        FLAGS = new String[50];

        String final_binary_str = hexToBinary(a.substring(28));
        Log.w("PARSE_FINAL", final_binary_str);
//        String RATE = final_binary_str.substring(1, 4);
        String PROGRAM_TYPE = final_binary_str.substring(0,4);
        String RELIABILITY = final_binary_str.substring(4, 8);
        String SENSORS = final_binary_str.substring(8, 16);
        String DATA = a.substring(32);

        TRANSPARENT = SENSORS.equals("00000000");
        FLAGS[Peripheral.ENUM.url.ordinal()] = fullURL;
//        FLAGS[Peripheral.ENUM.rate.ordinal()] = RATE;
        FLAGS[Peripheral.ENUM.qos.ordinal()] = RELIABILITY;
        FLAGS[Peripheral.ENUM.program_type.ordinal()] = PROGRAM_TYPE;
        FLAGS[Peripheral.ENUM.data_blob.ordinal()] = DATA;
        FLAGS[Peripheral.ENUM.dev_address.ordinal()] = devAddress;
        FLAGS[Peripheral.ENUM.dev_name.ordinal()] = devName;
        if (!TRANSPARENT) {
            FLAGS[Peripheral.ENUM.sensors.ordinal()] = SENSORS;
            FLAGS[Peripheral.ENUM.time.ordinal()]    = String.valueOf(SENSORS.charAt(0));
            FLAGS[Peripheral.ENUM.gps.ordinal()]     = String.valueOf(SENSORS.charAt(1));
            FLAGS[Peripheral.ENUM.accel.ordinal()]   = String.valueOf(SENSORS.charAt(2)); // Can change to peripheral.SENSOR_ENUM.x.ordinal()
            FLAGS[Peripheral.ENUM.ambient.ordinal()] = String.valueOf(SENSORS.charAt(3));
            FLAGS[Peripheral.ENUM.text.ordinal()]    = String.valueOf(SENSORS.charAt(4));
            FLAGS[Peripheral.ENUM.pic.ordinal()]     = String.valueOf(SENSORS.charAt(5));
            FLAGS[Peripheral.ENUM.ui.ordinal()]      = String.valueOf(SENSORS.charAt(6));
            FLAGS[Peripheral.ENUM.ip.ordinal()]      = String.valueOf(SENSORS.charAt(7));
        }

    }
    
}
