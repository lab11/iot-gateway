package edu.umich.eecs.eecs589;

/**
 * Created by nklugman on 11/23/14.
 */
public class Settings {
    public boolean gps_fine_agreement;
    public boolean gps_mid_agreement;
    public boolean gps_far_agreement;
    public boolean time_agreement;
    public boolean accel_agreement;
    public boolean user_input_agreement;
    public boolean user_camera_agreement;
    public boolean ambient_agreement;
    public boolean mon_agreement;
    public boolean non_mon_agreement;
    public boolean master_agreement;
    public Integer dataRate;
    public Integer reliabilityRate;
    public String programText;

    public Settings() {
        gps_fine_agreement = false;
        gps_mid_agreement = false;
        gps_far_agreement = false;
        time_agreement = false;
        accel_agreement = false;
        user_input_agreement = false;
        user_camera_agreement = false;
        ambient_agreement = false;

        mon_agreement = false;
        non_mon_agreement = false;

        master_agreement = false;

        dataRate = -1;
        reliabilityRate = -1;

        programText = "";
    }

    public void setDataRate(Integer setting){
        dataRate = setting;
    }
    public void setReliability(Integer setting){
        reliabilityRate = setting;
    }

    public void setProgramText(String setting) { programText = setting; }

    public void setGPS_fine(boolean setting){
        gps_fine_agreement = setting;
    }

    public void setGPS_mid(boolean setting){
        gps_mid_agreement = setting;
    }

    public void setGPS_far(boolean setting){
        gps_far_agreement = setting;
    }

    public void setTime(boolean setting){
        time_agreement = setting;
    }

    public void setAccel(boolean setting){
        accel_agreement = setting;
    }

    public void setUser_input(boolean setting) {
        user_input_agreement = setting;
    }

    public void setUser_camera(boolean setting) {
        user_camera_agreement = setting;
    }

    public void setAmbient(boolean setting){
        ambient_agreement = setting;
    }

    public void setMon(boolean setting){
        mon_agreement = setting;
    }

    public void setNonMon(boolean setting){
        non_mon_agreement = setting;
    }

    public void setMaster(boolean setting){
        master_agreement = setting;
    }

    public String boolToString(boolean state) {
        if (state) {
            return "1";
        } else return "0";
    }

    public String toString() {
        String final_str = "";
        final_str += boolToString(gps_fine_agreement);
        final_str += boolToString(gps_mid_agreement);
        final_str += boolToString(gps_far_agreement);
        final_str += boolToString(time_agreement);
        final_str += boolToString(accel_agreement);
        final_str += boolToString(user_input_agreement);
        final_str += boolToString(user_camera_agreement);
        final_str += boolToString(ambient_agreement);
        final_str += boolToString(mon_agreement);
        final_str += boolToString(non_mon_agreement);
        final_str += boolToString(master_agreement);
        final_str += dataRate;
        final_str += reliabilityRate;
        final_str += programText;
        return final_str;
    }

    public String prettyString() {
        String final_str = "";
        final_str += "\nGPS_FINE: " + boolToString(gps_fine_agreement) + "\n";
        final_str += "GPS_MID: " + boolToString(gps_mid_agreement) + "\n";
        final_str += "GPS_FAR: " + boolToString(gps_far_agreement) + "\n";
        final_str += "TIME: " + boolToString(time_agreement) + "\n";
        final_str += "ACCEL: " + boolToString(accel_agreement) + "\n";
        final_str += "USER TEXT: " + boolToString(user_input_agreement) + "\n";
        final_str += "USER CAMERA: " + boolToString(user_camera_agreement) + "\n";
        final_str += "AMBIENT: " + boolToString(ambient_agreement) + "\n";
        final_str += "MON: " + boolToString(mon_agreement) + "\n";
        final_str += "NON MON: " + boolToString(non_mon_agreement) + "\n";
        final_str += "MASTER: " + boolToString(master_agreement) + "\n";
        final_str += "DATA RATE: " + dataRate + "\n";
        final_str += "RELIABILITY: " + reliabilityRate + "\n";
        final_str += "PROGRAM TEXT: " + programText.toString() + "\n";
        return final_str;
    }

}
