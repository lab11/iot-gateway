package edu.umich.eecs.lab11.gateway;

import java.sql.Array;

/**
 * Created by nklugman on 11/19/14.
 */
public class Peripheral {

        public Array UUID_FLAGS;
        public Array SENSOR_FLAGS;
        public Array ADV_FLAGS;

        public enum Sensor_Fields {
            GPS_FINE, GPS_MID, GPS_FAR,
            time, accel, user_text,
            user_picture, ambiant_light
        }

        public enum UUID_Fields {
            transparent_flag, rate,
            level, sensors, program_need,
            program_type, UUID_REST
        }

        public enum ADV_Fields {
            ip_address, data_blob
        }

        public Peripheral() {}

}
