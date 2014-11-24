package edu.umich.eecs.eecs589;

import java.sql.Array;

/**
 * Created by nklugman on 11/19/14.
 */
public class Peripheral {

    public String[] PEEK_FLAGS;
    public String[] TRANSPARENT_FLAGS;

    public enum TRANSPARENT_ENUM {
        ip_address, transparent_flag, rate,
        data_blob
    }

    public enum PEEK_ENUM {
        ip_address, transparent_flag, rate,
        level, sensors, program_need, program_type,
        data_blob
    }

    public Peripheral() {
        //UUID_FLAGS = new int[7];
        //SENSOR_FLAGS = new int[8];
        //ADV_FLAGS = new int[2];
        PEEK_FLAGS = new String[20];
        TRANSPARENT_FLAGS = new String[20];
    }

}
