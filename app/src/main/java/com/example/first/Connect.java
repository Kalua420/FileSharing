package com.example.first;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.lang.reflect.Method;
import java.util.Objects;

/** @noinspection ALL*/
@RequiresApi(api = Build.VERSION_CODES.P)
public class Connect extends AppCompatActivity {
    MyServerIP ServerIP = new MyServerIP();
    private LinearLayout send, receive;
    private Toolbar toolbar;
    @SuppressLint("StaticFieldLeak")
    static TextView speed;
    private FileTransferServer fileTransferService;
    private boolean isBound = false;
    @SuppressLint("StaticFieldLeak")
    public static TextView textViewContent, clientConnected;
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;
    private static String savedLogText = "";
    static String ipToConnect = "";
    static String myServerIP = "";
    private AppCompatButton stopServer;
    private View rootView;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        rootView = findViewById(android.R.id.content);
        init();
        checkAndRequestPermissions();
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);

        // Restore previous logs
        if (!savedLogText.isEmpty() && textViewContent != null) {
            textViewContent.setText(savedLogText);
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        send.setOnClickListener(v -> {
            if (ipToConnect.isEmpty()){
                handleSendClick(wifiManager);
            }else {
                showSnackbar("Connecting to: " + ipToConnect);
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
            }
        });
        receive.setOnClickListener(v -> handleReceiveClick(wifiManager));
        stopServer.setOnClickListener(v -> handleStopServerClick());
    }

    private void handleSendClick(WifiManager wifiManager) {
        if (ipToConnect.isEmpty()){
            if (myServerIP.isEmpty()){
                if (!isHotspotEnabled(getApplicationContext())){
                    if (!wifiManager.isWifiEnabled()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            showWifiSettingsDialog();
                        } else {
                            wifiManager.setWifiEnabled(true);
                        }
                        return;
                    }
                }
            }
            if (ipToConnect.isEmpty()) {
                startScanner();
            } else {
                showSnackbar("Connecting to: " + ipToConnect);
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
            }
        }else {
            showSnackbar("Connecting to: " + ipToConnect);
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
        }
    }

    private void handleReceiveClick(WifiManager wifiManager) {
        myServerIP = ServerIP.getIp();
        if (!wifiManager.isWifiEnabled()){
            if (!isHotspotEnabled(getApplicationContext())) {
                Toast.makeText(getApplicationContext(), "Please enable Hotspot", Toast.LENGTH_SHORT).show();
                enableHotspot();
                return;
            }
        }
        if (fileTransferService == null) {
            Intent intent = new Intent(this, FileTransferServer.class);
            startService(intent);

            if (!isBound) {
                bindService(intent, connection, Context.BIND_AUTO_CREATE);
            }
            showSnackbar("Starting file transfer server...");
            return;
        }
        if (!myServerIP.isEmpty()) {
            showSnackbar("Server IP: " + myServerIP);
            generateQRCode(myServerIP);
        } else {
            showSnackbar("Failed to get server IP. Please try again.");
            stopService(new Intent(this, FileTransferServer.class));
            if (isBound) {
                unbindService(connection);
                isBound = false;
            }
            fileTransferService = null;
        }
        try {
            assert fileTransferService != null;
            ipToConnect = fileTransferService.clientIp;
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleStopServerClick() {
        if (fileTransferService != null) {
            stopService(new Intent(this, FileTransferServer.class));
            if (isBound) {
                unbindService(connection);
                isBound = false;
            }
            fileTransferService = null;
            showSnackbar("Server stopped");
        } else {
            showSnackbar("Server is not running");
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }

    private void showWifiSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("WiFi Required")
                .setMessage("Please enable WiFi to continue")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) ->
                        showSnackbar("WiFi is required for file transfers"))
                .show();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                } catch (Exception ignored) {
                }
            }
        } else {
            // Below Android 11
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        STORAGE_PERMISSION_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // Permission granted for Android 11 and above
                    Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted for Android 11 and below
                Toast.makeText(getApplicationContext(), "Storage permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void init() {
        send = findViewById(R.id.send);
        receive = findViewById(R.id.recv);
        toolbar = findViewById(R.id.toolbar);
        textViewContent = findViewById(R.id.textViewContent);
        progressBar = findViewById(R.id.progressBar);
        speed = findViewById(R.id.transferSpeed);
        stopServer = findViewById(R.id.stopServer);
        clientConnected = findViewById(R.id.clientConnected);
        // Initialize progress bar
        if (progressBar != null) {
            progressBar.setMax(100);
            progressBar.setProgress(0);
        }
    }

    @SuppressLint("PrivateApi")
    public boolean isHotspotEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void enableHotspot() {
        Intent intent;
        intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            FileTransferServer.LocalBinder binder = (FileTransferServer.LocalBinder) service;
            fileTransferService = binder.getService();
            isBound = true;
            showSnackbar("Server connected");

            // Generate QR code automatically when service is connected
            if (!myServerIP.isEmpty()) {
                generateQRCode(myServerIP);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            fileTransferService = null;
            showSnackbar("Server disconnected");
        }
    };

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (textViewContent != null) {
            savedLogText = textViewContent.getText().toString();
        }
    }

    @Override
    protected void onDestroy() {
        if (textViewContent != null) {
            savedLogText = textViewContent.getText().toString();
        }
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        super.onDestroy();
    }

    private void generateQRCode(String content) {
        try {
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    500,
                    500
            );

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            showQrDialog(bitmap, content);
        } catch (Exception e) {
            e.printStackTrace();
            showSnackbar("QR Code generation failed");
        }
    }

    private final ActivityResultLauncher<ScanOptions> scanLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    ipToConnect = result.getContents();
                    showSnackbar("Connected to: " + ipToConnect);
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(i);
                }
            }
    );

    private void startScanner() {
        ScanOptions options = new ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan Server QR Code")
                .setBeepEnabled(true)
                .setBarcodeImageEnabled(true)
                .setOrientationLocked(true)
                .setCaptureActivity(CaptureActivityPortrait.class);
        scanLauncher.launch(options);
    }

    private void showQrDialog(Bitmap bitmap, String ip) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.generated_qrcode);
        ImageView imageView = dialog.findViewById(R.id.qrImageView);
        TextView setIp = dialog.findViewById(R.id.serverIp);
        imageView.setImageBitmap(bitmap);
        setIp.setText(ip);
        AppCompatButton closeQr = dialog.findViewById(R.id.closeQr);
        closeQr.setOnClickListener(v1 -> dialog.dismiss());
        dialog.setCancelable(true);
        dialog.show();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.option,menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId==R.id.logs){
            Intent i = new Intent(getApplicationContext(),LogsActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }
}