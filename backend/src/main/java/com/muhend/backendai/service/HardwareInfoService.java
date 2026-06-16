package com.muhend.backendai.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class HardwareInfoService {

    public static String getHardwareId() {
        String hwId = getWindowsUuid();
        if (hwId != null && !hwId.isEmpty()) {
            return hwId;
        }
        return getMacAddress();
    }

    private static String getWindowsUuid() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"wmic", "csproduct", "get", "uuid"});
            process.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.equalsIgnoreCase("UUID")) {
                    sb.append(line);
                }
            }
            String uuid = sb.toString().trim();
            return uuid.isEmpty() ? null : uuid;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                byte[] hardwareAddress = ni.getHardwareAddress();
                if (hardwareAddress != null) {
                    String[] hexadecimal = new String[hardwareAddress.length];
                    for (int i = 0; i < hardwareAddress.length; i++) {
                        hexadecimal[i] = String.format("%02X", hardwareAddress[i]);
                    }
                    return String.join("-", hexadecimal);
                }
            }
        } catch (Exception e) {
            // Ignorer
        }
        return "UNKNOWN-HW-ID-" + System.currentTimeMillis();
    }
}
