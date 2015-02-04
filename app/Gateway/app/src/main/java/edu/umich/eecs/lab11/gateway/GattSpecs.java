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
        characteristics.put(shortUUID("2A43"), "Alert Category ID");
        characteristics.put(shortUUID("2A42"), "Alert Category ID Bit Mask");
        characteristics.put(shortUUID("2A06"), "Alert Level");
        characteristics.put(shortUUID("2A44"), "Alert Notification Control Point");
        characteristics.put(shortUUID("2A3F"), "Alert Status");
        characteristics.put(shortUUID("2A01"), "Appearance");
        characteristics.put(shortUUID("2A19"), "Battery Level");
        characteristics.put(shortUUID("2A49"), "Blood Pressure Feature");
        characteristics.put(shortUUID("2A35"), "Blood Pressure Measurement");
        characteristics.put(shortUUID("2A38"), "Body Sensor Location");
        characteristics.put(shortUUID("2A22"), "Boot Keyboard Input Report");
        characteristics.put(shortUUID("2A32"), "Boot Keyboard Output Report");
        characteristics.put(shortUUID("2A33"), "Boot Mouse Input Report");
        characteristics.put(shortUUID("2A5C"), "CSC Feature");
        characteristics.put(shortUUID("2A5B"), "CSC Measurement");
        characteristics.put(shortUUID("2A2B"), "Current Time");
        characteristics.put(shortUUID("2A66"), "Cycling Power Control Point");
        characteristics.put(shortUUID("2A65"), "Cycling Power Feature");
        characteristics.put(shortUUID("2A63"), "Cycling Power Measurement");
        characteristics.put(shortUUID("2A64"), "Cycling Power Vector");
        characteristics.put(shortUUID("2A08"), "Date Time");
        characteristics.put(shortUUID("2A0A"), "Day Date Time");
        characteristics.put(shortUUID("2A09"), "Day of Week");
        characteristics.put(shortUUID("2A00"), "Device Name");
        characteristics.put(shortUUID("2A0D"), "DST Offset");
        characteristics.put(shortUUID("2A0C"), "Exact Time 256");
        characteristics.put(shortUUID("2A26"), "Firmware Revision String");
        characteristics.put(shortUUID("2A51"), "Glucose Feature");
        characteristics.put(shortUUID("2A18"), "Glucose Measurement");
        characteristics.put(shortUUID("2A34"), "Glucose Measurement Context");
        characteristics.put(shortUUID("2A27"), "Hardware Revision String");
        characteristics.put(shortUUID("2A39"), "Heart Rate Control Point");
        characteristics.put(shortUUID("2A37"), "Heart Rate Measurement");
        characteristics.put(shortUUID("2A4C"), "HID Control Point");
        characteristics.put(shortUUID("2A4A"), "HID Information");
        characteristics.put(shortUUID("2A2A"), "IEEE 11073-20601 Regulatory Certification Data List");
        characteristics.put(shortUUID("2A36"), "Intermediate Cuff Pressure");
        characteristics.put(shortUUID("2A1E"), "Intermediate Temperature");
        characteristics.put(shortUUID("2A6B"), "LN Control Point");
        characteristics.put(shortUUID("2A6A"), "LN Feature");
        characteristics.put(shortUUID("2A0F"), "Local Time Information");
        characteristics.put(shortUUID("2A67"), "Location and Speed");
        characteristics.put(shortUUID("2A29"), "Manufacturer Name String");
        characteristics.put(shortUUID("2A21"), "Measurement Interval");
        characteristics.put(shortUUID("2A24"), "Model Number String");
        characteristics.put(shortUUID("2A68"), "Navigation");
        characteristics.put(shortUUID("2A46"), "New Alert");
        characteristics.put(shortUUID("2A04"), "Peripheral Preferred Connection Parameters");
        characteristics.put(shortUUID("2A02"), "Peripheral Privacy Flag");
        characteristics.put(shortUUID("2A50"), "PnP ID");
        characteristics.put(shortUUID("2A69"), "Position Quality");
        characteristics.put(shortUUID("2A4E"), "Protocol Mode");
        characteristics.put(shortUUID("2A03"), "Reconnection Address");
        characteristics.put(shortUUID("2A52"), "Record Access Control Point");
        characteristics.put(shortUUID("2A14"), "Reference Time Information");
        characteristics.put(shortUUID("2A4D"), "Report");
        characteristics.put(shortUUID("2A4B"), "Report Map");
        characteristics.put(shortUUID("2A40"), "Ringer Control Point");
        characteristics.put(shortUUID("2A41"), "Ringer Setting");
        characteristics.put(shortUUID("2A54"), "RSC Feature");
        characteristics.put(shortUUID("2A53"), "RSC Measurement");
        characteristics.put(shortUUID("2A55"), "SC Control Point");
        characteristics.put(shortUUID("2A4F"), "Scan Interval Window");
        characteristics.put(shortUUID("2A31"), "Scan Refresh");
        characteristics.put(shortUUID("2A5D"), "Sensor Location");
        characteristics.put(shortUUID("2A25"), "Serial Number String");
        characteristics.put(shortUUID("2A05"), "Service Changed");
        characteristics.put(shortUUID("2A28"), "Software Revision String");
        characteristics.put(shortUUID("2A47"), "Supported New Alert Category");
        characteristics.put(shortUUID("2A48"), "Supported Unread Alert Category");
        characteristics.put(shortUUID("2A23"), "System ID");
        characteristics.put(shortUUID("2A1C"), "Temperature Measurement");
        characteristics.put(shortUUID("2A1D"), "Temperature Type");
        characteristics.put(shortUUID("2A12"), "Time Accuracy");
        characteristics.put(shortUUID("2A13"), "Time Source");
        characteristics.put(shortUUID("2A16"), "Time Update Control Point");
        characteristics.put(shortUUID("2A17"), "Time Update State");
        characteristics.put(shortUUID("2A11"), "Time with DST");
        characteristics.put(shortUUID("2A0E"), "Time Zone");
        characteristics.put(shortUUID("2A07"), "Tx Power Level");
        characteristics.put(shortUUID("2A45"), "Unread Alert Status");
        characteristics.put(shortUUID("2A5A"), "Aggregate Input");
        characteristics.put(shortUUID("2A58"), "Analog Input");
        characteristics.put(shortUUID("2A59"), "Analog Output");
        characteristics.put(shortUUID("2A56"), "Digital Input");
        characteristics.put(shortUUID("2A57"), "Digital Output");
        characteristics.put(shortUUID("2A0B"), "Exact Time 100");
        characteristics.put(shortUUID("2A3E"), "Network Availability");
        characteristics.put(shortUUID("2A3C"), "Scientific Temperature in Celsius");
        characteristics.put(shortUUID("2A10"), "Secondary Time Zone");
        characteristics.put(shortUUID("2A3D"), "String");
        characteristics.put(shortUUID("2A1F"), "Temperature in Celsius");
        characteristics.put(shortUUID("2A20"), "Temperature in Fahrenheit");
        characteristics.put(shortUUID("2A15"), "Time Broadcast");
        characteristics.put(shortUUID("2A1B"), "Battery Level State");
        characteristics.put(shortUUID("2A1A"), "Battery Power State");
        characteristics.put(shortUUID("2A5F"), "Pulse Oximetry Continuous Measurement");
        characteristics.put(shortUUID("2A62"), "Pulse Oximetry Control Point");
        characteristics.put(shortUUID("2A61"), "Pulse Oximetry Features");
        characteristics.put(shortUUID("2A60"), "Pulse Oximetry Pulsatile Event");
        characteristics.put(shortUUID("2A5E"), "Pulse Oximetry Spot-Check Measurement");
        characteristics.put(shortUUID("2A52"), "Record Access Control point (Test Version)");
        characteristics.put(shortUUID("2A3A"), "Removable");
        characteristics.put(shortUUID("2A3B"), "Service Required");
    }

    // PUBLIC LOOKUP FUNCTIONS

    public static String serviceNameLookup(UUID uuid) {
        String service = services.get(uuid);
        return (service==null) ? uuid.toString() : service;
    }

    public static String characteristicNameLookup(UUID uuid) {
        String characteristic = characteristics.get(uuid);
        return (characteristic==null) ? uuid.toString() : characteristic;
    }

    // SHORT UUID GENERATOR (WHERE s IS 4-DIGIT HEX STRING)

    public static UUID shortUUID(String s) {
        return UUID.fromString("0000" + s + "-0000-1000-8000-00805F9B34FB");
    }

}
