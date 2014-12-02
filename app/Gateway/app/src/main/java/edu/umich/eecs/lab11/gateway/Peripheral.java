package edu.umich.eecs.lab11.gateway;

/**
 * Created by nklugman on 11/19/14.
 */
public class Peripheral {

    public String[] PEEK_FLAGS;
    public String[] TRANSPARENT_FLAGS;

    public enum TRANSPARENT_ENUM {
        ip_address, transparent_flag, rate, program_type,
        data_blob
    }

    public enum LEVEL_ENUM {
        REQ_ALL, REQ_ALL_NO_RATE,
        REQ_ALL_NO_SENSORS, REQ_ALL_NO_SENSORS_NOT_INCL_GPS,
        REQ_NONE_BUT_CONNECTION, REQ_NONE_BUT_SERVICE, REQ_NONE
    }

    public enum PEEK_ENUM {
        ip_address, transparent_flag, rate,
        level, gps, temp, humidity, time, accel, text, pic, ambiant,
        program_need, program_type,
        data_blob
    }

    public enum SENSOR_ENUM {
        gps, temp, humidity, time, accel, text, pic, ambiant
    }

    public void empty() {

        PEEK_FLAGS = new String[50];
        TRANSPARENT_FLAGS = new String[50];
    }

    public Peripheral() {
        //UUID_FLAGS = new int[7];
        //SENSOR_FLAGS = new int[8];
        //ADV_FLAGS = new int[2];
        PEEK_FLAGS = new String[50];
        TRANSPARENT_FLAGS = new String[50];
    }

}
