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
}