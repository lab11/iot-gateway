package edu.umich.eecs.lab11.gateway;

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
        ip_address, transparent_flag, rate,
        qos, gps, temp, humidity, time, accel, text, pic, ambient,
        program_need, program_type,
        data_blob, dev_address, dev_name
    }

    public enum SENSOR_ENUM {
        gps, temp, humidity, time, accel, text, pic, ambient
    }

    public void empty() {
        FLAGS = new String[50];
        DATA_TO_PEEK.clear();
    }

    public Peripheral() {
        //UUID_FLAGS = new int[7];
        //SENSOR_FLAGS = new int[8];
        //ADV_FLAGS = new int[2];
        FLAGS = new String[50];
        TRANSPARENT = false;
    }

}
