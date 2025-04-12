package com.example.first;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MacAddressUtil {
    private static final String[] MAC_ADDRESS_INTERFACES = {
            "wlan0",     // WiFi interface
            "eth0",      // Ethernet interface
            "dummy0"     // Fallback interface
    };

    // Common Android device manufacturer prefixes
    private static final String[] COMMON_PREFIXES = {
            "00:23:76", // Samsung
            "40:4E:36", // HTC
            "9C:D9:17", // Motorola
            "F4:F5:D8", // Google
            "00:BB:3A", // Amazon
            "70:48:0F", // Sony
            "B4:F7:A1", // LG
            "18:F0:E4", // Xiaomi
            "94:65:2D", // OnePlus
            "48:A4:93", //realme
            "F8:DB:7F",
            "4C:24:57",
            "1C:39:47",
            "40:9C:28", //oppo
            "B0:D8:63",
            "E4:6F:13",
            "3C:28:6D",
            "5C:45:27", //vivos
            "44:23:7C",
            "88:C9:D0",
            "B4:A5:AC"
    };

    private static final String[] VALID_HEX = {
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "A", "B", "C", "D", "E", "F"
    };

    public static String getMacAddress() {
        String macAddress = getRealMacAddress();

        // If real MAC address couldn't be fetched, return a random one
        if (macAddress == null || macAddress.isEmpty()) {
            macAddress = generateRandomMac();
        }

        return macAddress;
    }

    private static String getRealMacAddress() {
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            // Try each known interface
            for (String interfaceName : MAC_ADDRESS_INTERFACES) {
                for (NetworkInterface networkInterface : networkInterfaces) {
                    if (interfaceName.equals(networkInterface.getName())) {
                        byte[] hardwareAddress = networkInterface.getHardwareAddress();
                        if (hardwareAddress != null) {
                            StringBuilder builder = new StringBuilder();
                            for (byte b : hardwareAddress) {
                                builder.append(String.format("%02X:", b));
                            }
                            if (builder.length() > 0) {
                                builder.deleteCharAt(builder.length() - 1);
                            }
                            return builder.toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;  // Couldn't get a real MAC address
    }

    private static String generateRandomMac() {
        StringBuilder mac = new StringBuilder();

        // Use a random manufacturer prefix
        mac.append(COMMON_PREFIXES[new Random().nextInt(COMMON_PREFIXES.length)]);

        // Generate the remaining 3 octets
        for (int i = 0; i < 3; i++) {
            mac.append(":");
            mac.append(generateRandomOctet());
        }

        return mac.toString();
    }

    private static String generateRandomOctet() {
        Random random = new Random();
        StringBuilder octet = new StringBuilder();

        // Generate two random hex digits
        for (int i = 0; i < 2; i++) {
            octet.append(VALID_HEX[random.nextInt(VALID_HEX.length)]);
        }

        return octet.toString();
    }
}