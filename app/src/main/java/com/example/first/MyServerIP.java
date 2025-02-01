package com.example.first;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

/** @noinspection CallToPrintStackTrace*/
public class MyServerIP extends AppCompatActivity {
    private String ip = null;
    public String getIp() {
        try {
            // Define a regex pattern to match IPv4 addresses
            Pattern ipv4Pattern = Pattern.compile("\\A(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\z");

            // Get all network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // Skip loopback and non-active interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                // Iterate through all the addresses associated with this interface
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    String ipAddress = inetAddress.getHostAddress();

                    assert ipAddress != null;
                    if (ipv4Pattern.matcher(ipAddress).matches()) {
                        Log.d("MainActivity", "Interface: " + networkInterface.getName() +" : "+ ipAddress);
                        if (networkInterface.getName().contains("ap0")) {
                            Log.d("MainActivity", "Interface: " + networkInterface.getName() + " :" + ipAddress);
                            ip = ipAddress;
                        }else if (networkInterface.getName().contains("wlan")){
                            ip = ipAddress;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ip;
    }
    @SuppressLint("HardwareIds")
    public String getMacAddress(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
        }

        String macAddress = null;

        // Try Hotspot MAC first
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                String name = intf.getName();
                // Check hotspot interfaces
                if (name.contains("ap") || name.contains("swlan") || name.contains("softap")) {
                    byte[] mac = intf.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (byte b : mac) {
                            sb.append(String.format("%02X:", b));
                        }
                        if (sb.length() > 0) {
                            sb.setLength(sb.length() - 1);
                            macAddress = sb.toString();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback: Try to get wlan0 MAC
        if (macAddress == null) {
            try {
                NetworkInterface wlanInterface = NetworkInterface.getByName("wlan0");
                if (wlanInterface != null && wlanInterface.getHardwareAddress() != null) {
                    byte[] mac = wlanInterface.getHardwareAddress();
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02X:", b));
                    }
                    if (sb.length() > 0) {
                        sb.setLength(sb.length() - 1);
                        macAddress = sb.toString();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return macAddress != null ? macAddress : "02:00:00:00:00:00";
    }
}