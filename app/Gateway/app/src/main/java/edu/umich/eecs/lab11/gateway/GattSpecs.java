package edu.umich.eecs.lab11.gateway;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Thomas Zachariah on 1/30/15.
 */

public class GattSpecs {

    // ADOPTED GATT SERVICES AS OF BLE 4.2 (1/30/15)

    private static HashMap<UUID, String> services = new HashMap<UUID, String>();
    static {
        services.put(shortUUID("1811"), "Alert Notification Service");
        services.put(shortUUID("180F"), "Battery Service");
        services.put(shortUUID("1810"), "Blood Pressure");
        services.put(shortUUID("181B"), "Body Composition");
        services.put(shortUUID("181E"), "Bond Management");
        services.put(shortUUID("181F"), "Continuous Glucose Monitoring");
        services.put(shortUUID("1805"), "Current Time Service");
        services.put(shortUUID("1818"), "Cycling Power");
        services.put(shortUUID("1816"), "Cycling Speed and Cadence");
        services.put(shortUUID("180A"), "Device Information");
        services.put(shortUUID("181A"), "Environmental Sensing");
        services.put(shortUUID("1800"), "Generic Access");
        services.put(shortUUID("1801"), "Generic Attribute");
        services.put(shortUUID("1808"), "Glucose");
        services.put(shortUUID("1809"), "Health Thermometer");
        services.put(shortUUID("180D"), "Heart Rate");
        services.put(shortUUID("1812"), "Human Interface Device");
        services.put(shortUUID("1802"), "Immediate Alert");
        services.put(shortUUID("1820"), "Internet Protocol Support");
        services.put(shortUUID("1803"), "Link Loss");
        services.put(shortUUID("1819"), "Location and Navigation");
        services.put(shortUUID("1807"), "Next DST Change Service");
        services.put(shortUUID("180E"), "Phone Alert Status Service");
        services.put(shortUUID("1806"), "Reference Time Update Service");
        services.put(shortUUID("1814"), "Running Speed and Cadence");
        services.put(shortUUID("1813"), "Scan Parameters");
        services.put(shortUUID("1804"), "Tx Power");
        services.put(shortUUID("181C"), "User Data");
        services.put(shortUUID("181D"), "Weight Scale");
    }

    // ADOPTED GATT CHARACTERISTICS AS OF BLE 4.0 (6/30/13) -- TODO: UPDATE TO 4.2

    private static HashMap<UUID, String> characteristics = new HashMap<UUID, String>();
    static {
        characteristics.put(shortUUID("2A43L"), "Alert Category ID");
        characteristics.put(shortUUID("2A42L"), "Alert Category ID Bit Mask");
        characteristics.put(shortUUID("2A06L"), "Alert Level");
        characteristics.put(shortUUID("2A44L"), "Alert Notification Control Point");
        characteristics.put(shortUUID("2A3FL"), "Alert Status");
        characteristics.put(shortUUID("2A01L"), "Appearance");
        characteristics.put(shortUUID("2A19L"), "Battery Level");
        characteristics.put(shortUUID("2A49L"), "Blood Pressure Feature");
        characteristics.put(shortUUID("2A35L"), "Blood Pressure Measurement");
        characteristics.put(shortUUID("2A38L"), "Body Sensor Location");
        characteristics.put(shortUUID("2A22L"), "Boot Keyboard Input Report");
        characteristics.put(shortUUID("2A32L"), "Boot Keyboard Output Report");
        characteristics.put(shortUUID("2A33L"), "Boot Mouse Input Report");
        characteristics.put(shortUUID("2A5CL"), "CSC Feature");
        characteristics.put(shortUUID("2A5BL"), "CSC Measurement");
        characteristics.put(shortUUID("2A2BL"), "Current Time");
        characteristics.put(shortUUID("2A66L"), "Cycling Power Control Point");
        characteristics.put(shortUUID("2A65L"), "Cycling Power Feature");
        characteristics.put(shortUUID("2A63L"), "Cycling Power Measurement");
        characteristics.put(shortUUID("2A64L"), "Cycling Power Vector");
        characteristics.put(shortUUID("2A08L"), "Date Time");
        characteristics.put(shortUUID("2A0AL"), "Day Date Time");
        characteristics.put(shortUUID("2A09L"), "Day of Week");
        characteristics.put(shortUUID("2A00L"), "Device Name");
        characteristics.put(shortUUID("2A0DL"), "DST Offset");
        characteristics.put(shortUUID("2A0CL"), "Exact Time 256");
        characteristics.put(shortUUID("2A26L"), "Firmware Revision String");
        characteristics.put(shortUUID("2A51L"), "Glucose Feature");
        characteristics.put(shortUUID("2A18L"), "Glucose Measurement");
        characteristics.put(shortUUID("2A34L"), "Glucose Measurement Context");
        characteristics.put(shortUUID("2A27L"), "Hardware Revision String");
        characteristics.put(shortUUID("2A39L"), "Heart Rate Control Point");
        characteristics.put(shortUUID("2A37L"), "Heart Rate Measurement");
        characteristics.put(shortUUID("2A4CL"), "HID Control Point");
        characteristics.put(shortUUID("2A4AL"), "HID Information");
        characteristics.put(shortUUID("2A2AL"), "IEEE 11073-20601 Regulatory Certification Data List");
        characteristics.put(shortUUID("2A36L"), "Intermediate Cuff Pressure");
        characteristics.put(shortUUID("2A1EL"), "Intermediate Temperature");
        characteristics.put(shortUUID("2A6BL"), "LN Control Point");
        characteristics.put(shortUUID("2A6AL"), "LN Feature");
        characteristics.put(shortUUID("2A0FL"), "Local Time Information");
        characteristics.put(shortUUID("2A67L"), "Location and Speed");
        characteristics.put(shortUUID("2A29L"), "Manufacturer Name String");
        characteristics.put(shortUUID("2A21L"), "Measurement Interval");
        characteristics.put(shortUUID("2A24L"), "Model Number String");
        characteristics.put(shortUUID("2A68L"), "Navigation");
        characteristics.put(shortUUID("2A46L"), "New Alert");
        characteristics.put(shortUUID("2A04L"), "Peripheral Preferred Connection Parameters");
        characteristics.put(shortUUID("2A02L"), "Peripheral Privacy Flag");
        characteristics.put(shortUUID("2A50L"), "PnP ID");
        characteristics.put(shortUUID("2A69L"), "Position Quality");
        characteristics.put(shortUUID("2A4EL"), "Protocol Mode");
        characteristics.put(shortUUID("2A03L"), "Reconnection Address");
        characteristics.put(shortUUID("2A52L"), "Record Access Control Point");
        characteristics.put(shortUUID("2A14L"), "Reference Time Information");
        characteristics.put(shortUUID("2A4DL"), "Report");
        characteristics.put(shortUUID("2A4BL"), "Report Map");
        characteristics.put(shortUUID("2A40L"), "Ringer Control Point");
        characteristics.put(shortUUID("2A41L"), "Ringer Setting");
        characteristics.put(shortUUID("2A54L"), "RSC Feature");
        characteristics.put(shortUUID("2A53L"), "RSC Measurement");
        characteristics.put(shortUUID("2A55L"), "SC Control Point");
        characteristics.put(shortUUID("2A4FL"), "Scan Interval Window");
        characteristics.put(shortUUID("2A31L"), "Scan Refresh");
        characteristics.put(shortUUID("2A5DL"), "Sensor Location");
        characteristics.put(shortUUID("2A25L"), "Serial Number String");
        characteristics.put(shortUUID("2A05L"), "Service Changed");
        characteristics.put(shortUUID("2A28L"), "Software Revision String");
        characteristics.put(shortUUID("2A47L"), "Supported New Alert Category");
        characteristics.put(shortUUID("2A48L"), "Supported Unread Alert Category");
        characteristics.put(shortUUID("2A23L"), "System ID");
        characteristics.put(shortUUID("2A1CL"), "Temperature Measurement");
        characteristics.put(shortUUID("2A1DL"), "Temperature Type");
        characteristics.put(shortUUID("2A12L"), "Time Accuracy");
        characteristics.put(shortUUID("2A13L"), "Time Source");
        characteristics.put(shortUUID("2A16L"), "Time Update Control Point");
        characteristics.put(shortUUID("2A17L"), "Time Update State");
        characteristics.put(shortUUID("2A11L"), "Time with DST");
        characteristics.put(shortUUID("2A0EL"), "Time Zone");
        characteristics.put(shortUUID("2A07L"), "Tx Power Level");
        characteristics.put(shortUUID("2A45L"), "Unread Alert Status");
        characteristics.put(shortUUID("2A5AL"), "Aggregate Input");
        characteristics.put(shortUUID("2A58L"), "Analog Input");
        characteristics.put(shortUUID("2A59L"), "Analog Output");
        characteristics.put(shortUUID("2A56L"), "Digital Input");
        characteristics.put(shortUUID("2A57L"), "Digital Output");
        characteristics.put(shortUUID("2A0BL"), "Exact Time 100");
        characteristics.put(shortUUID("2A3EL"), "Network Availability");
        characteristics.put(shortUUID("2A3CL"), "Scientific Temperature in Celsius");
        characteristics.put(shortUUID("2A10L"), "Secondary Time Zone");
        characteristics.put(shortUUID("2A3DL"), "String");
        characteristics.put(shortUUID("2A1FL"), "Temperature in Celsius");
        characteristics.put(shortUUID("2A20L"), "Temperature in Fahrenheit");
        characteristics.put(shortUUID("2A15L"), "Time Broadcast");
        characteristics.put(shortUUID("2A1BL"), "Battery Level State");
        characteristics.put(shortUUID("2A1AL"), "Battery Power State");
        characteristics.put(shortUUID("2A5FL"), "Pulse Oximetry Continuous Measurement");
        characteristics.put(shortUUID("2A62L"), "Pulse Oximetry Control Point");
        characteristics.put(shortUUID("2A61L"), "Pulse Oximetry Features");
        characteristics.put(shortUUID("2A60L"), "Pulse Oximetry Pulsatile Event");
        characteristics.put(shortUUID("2A5EL"), "Pulse Oximetry Spot-Check Measurement");
        characteristics.put(shortUUID("2A52L"), "Record Access Control point (Test Version)");
        characteristics.put(shortUUID("2A3AL"), "Removable");
        characteristics.put(shortUUID("2A3BL"), "Service Required");
    }

    // PUBLIC LOOKUP FUNCTIONS

    public static String serviceNameLookup(UUID uuid) {
        return services.get(uuid);
    }

    public static String characteristicNameLookup(UUID uuid) {
        return characteristics.get(uuid);
    }

    // SHORT UUID GENERATOR (WHERE s IS 4-DIGIT HEX STRING)

    public static UUID shortUUID(String s) {
        return UUID.fromString("0000" + s + "-0000-1000-8000-00805F9B34FB");
    }

}
