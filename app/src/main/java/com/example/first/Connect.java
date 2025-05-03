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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/** @noinspection ALL*/
@RequiresApi(api = Build.VERSION_CODES.P)
public class Connect extends AppCompatActivity {
    MyServerIP ServerIP = new MyServerIP();
    private LinearLayout send, receive;
    private Toolbar toolbar;
    @SuppressLint("StaticFieldLeak")
    static TextView speed, userLogedInEmail;
    private FileTransferServer fileTransferService;
    private boolean isBound = false;
    @SuppressLint("StaticFieldLeak")
    public static TextView textViewContent, clientConnected;
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;
    private static String savedLogText = "";
    static String ipToConnect = "";
    static String myServerIP = "";
    static String serverMac = "";
    public static String myMacAddress = "";
    private View rootView;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 101;
    private AppCompatButton stopServer;
    // New UI components for transfer control
    @SuppressLint("StaticFieldLeak")
    public static Button btnPauseResume;
    @SuppressLint("StaticFieldLeak")
    public static Button btnCancel;
    @SuppressLint("StaticFieldLeak")
    public static TextView fileName;
    @SuppressLint("StaticFieldLeak")
    public static TextView transferStatus;
    public static String currentTransferId;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    public static int myUserId;
    public static int targetUserId;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        rootView = findViewById(android.R.id.content);
        init();
        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            // User is not logged in, redirect to login
            sessionManager.logout();
            return;
        }
        int userId = sessionManager.getUserId();
        String userEmail = sessionManager.getUserEmail();
        userLogedInEmail.setText(userEmail);

        myUserId = getIntent().getIntExtra("userId", -1);
        Log.d("userId", String.valueOf(myUserId));
        checkAndRequestPermissions();
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);

        // Restore previous logs
        if (!savedLogText.isEmpty() && textViewContent != null) {
            textViewContent.setText(savedLogText);
        }
        setupClickListeners();
        setupTransferControls();
    }

    // Setup transfer control buttons
    private void setupTransferControls() {
        btnPauseResume.setOnClickListener(v -> {
            if (isBound && fileTransferService != null && currentTransferId != null) {
                FileTransferServer.TransferStatus status =
                        fileTransferService.getActiveTransfers().get(currentTransferId);
                if (status != null) {
                    if (status.getState() == FileTransferServer.TransferState.RUNNING) {
                        fileTransferService.pauseTransfer(currentTransferId);
                        btnPauseResume.setText("Resume");
                    } else if (status.getState() == FileTransferServer.TransferState.PAUSED) {
                        fileTransferService.resumeTransfer(currentTransferId);
                        btnPauseResume.setText("Pause");
                    }
                }
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (isBound && fileTransferService != null && currentTransferId != null) {
                fileTransferService.cancelTransfer(currentTransferId);
                btnPauseResume.setEnabled(false);
                btnCancel.setEnabled(false);
            }
        });

        // Initially disable the buttons until a transfer starts
        btnPauseResume.setEnabled(false);
        btnCancel.setEnabled(false);
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
        receive.setOnClickListener(v -> {
            handleReceiveClick(wifiManager);
        });
    }

    private void handleSendClick(WifiManager wifiManager) {
        myMacAddress = MacAddressUtil.getMacAddress();
        if (ipToConnect.isEmpty()){
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
        // First, ensure we have both IP and MAC address
        myServerIP = ServerIP.getIp();
        myMacAddress = MacAddressUtil.getMacAddress();

        // If WiFi is enabled, continue with the process
        if (wifiManager.isWifiEnabled()) {
            // WiFi is enabled, proceed with the connection
            continueWithConnection();
            return;
        }else if (!isHotspotEnabled(this)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Network Required")
                    .setMessage("Please enable Hotspot to continue")
                    .setPositiveButton("Enable Hotspot", (dialog, which) -> {
                        // Enable hotspot
                        enableHotspot();
                        Toast.makeText(getApplicationContext(), "Please enable Hotspot", Toast.LENGTH_SHORT).show();
                    })
                    .setCancelable(true)
                    .show();
            return;
        }
        continueWithConnection();
    }

    // Helper method to continue with the connection process
    private void continueWithConnection() {
        // Check if we have valid values for IP and MAC
        if (myServerIP == null || myServerIP.isEmpty() || myMacAddress == null || myMacAddress.isEmpty()) {
            // Try to get IP again
            myServerIP = ServerIP.getIp();

            // If still not valid, show error
            if (myServerIP == null || myServerIP.isEmpty() || myMacAddress == null || myMacAddress.isEmpty()) {
                showSnackbar("Failed to get server information. Please try again.");
                return;
            }
        }

        // Start the service if not already started
        if (fileTransferService == null) {
            Intent intent = new Intent(this, FileTransferServer.class);
            startService(intent);
            if (!isBound) {
                bindService(intent, connection, Context.BIND_AUTO_CREATE);
            }
            showSnackbar("Starting file transfer server...");

            // Now we wait for the service connection callback to complete before generating QR
            return;
        }

        // Only generate QR code if we have all information available
        String ServerIpAndMac = myUserId + "/" + myServerIP + "/" + myMacAddress;
        showSnackbar(ServerIpAndMac);
        generateQRCode(ServerIpAndMac);

        try {
            if (fileTransferService != null) {
                ipToConnect = fileTransferService.clientIp;
            }
        } catch (RuntimeException e) {
            Log.e("Connect", "Error getting client IP", e);
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
        clientConnected = findViewById(R.id.clientConnected);
        userLogedInEmail = findViewById(R.id.logedInUserEmail);

        // Initialize new controls for file transfer
        fileName = findViewById(R.id.tv_file_name);
        transferStatus = findViewById(R.id.tv_transfer_status);
        btnPauseResume = findViewById(R.id.btn_pause_resume);
        btnCancel = findViewById(R.id.btn_cancel);

        // Initialize progress bar
        if (progressBar != null) {
            progressBar.setMax(100);
            progressBar.setProgress(0);
        }
    }

    @SuppressLint("PrivateApi")
    public boolean isHotspotEnabled(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // For Android 10 (Q) and above
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager != null) {
                try {
                    // Use TetheringManager for Android 10+
                    Class<?> tetheringManagerClass = Class.forName("android.net.TetheringManager");
                    Field startTetheringCallbackField = tetheringManagerClass.getDeclaredField("START_TETHERING_CALLBACK");
                    Object startTetheringCallback = startTetheringCallbackField.get(null);

                    Method getSystemServiceMethod = ConnectivityManager.class.getMethod("getSystemService", Class.class);
                    Object tetheringManager = getSystemServiceMethod.invoke(connectivityManager, tetheringManagerClass);

                    if (tetheringManager != null) {
                        // Get active tether interfaces
                        Method getTetheringInterfacesMethod = tetheringManagerClass.getMethod("getTetherableIfaces");
                        String[] tetherableIfaces = (String[]) getTetheringInterfacesMethod.invoke(tetheringManager);

                        Method getActiveTetheringInterfacesMethod = tetheringManagerClass.getMethod("getTetheredIfaces");
                        String[] tetheredIfaces = (String[]) getActiveTetheringInterfacesMethod.invoke(tetheringManager);

                        // If there are active tethered interfaces, hotspot is likely enabled
                        return tetheredIfaces != null && tetheredIfaces.length > 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Fallback to checking tethered interfaces directly via reflection
                    try {
                        Method method = connectivityManager.getClass().getDeclaredMethod("getTetheredIfaces");
                        method.setAccessible(true);
                        String[] tetheredIfaces = (String[]) method.invoke(connectivityManager);
                        return tetheredIfaces != null && tetheredIfaces.length > 0;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            return false;
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // For Android 8.0 (Oreo) to Android 9 (Pie)
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager != null) {
                try {
                    Method method = connectivityManager.getClass().getDeclaredMethod("getTetheredIfaces");
                    method.setAccessible(true);
                    String[] tetheredIfaces = (String[]) method.invoke(connectivityManager);
                    return tetheredIfaces != null && tetheredIfaces.length > 0;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        } else {
            // For versions below Android 8.0
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            try {
                Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
                method.setAccessible(true);
                return (Boolean) method.invoke(wifiManager);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
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

            // Generate QR code with both IP and MAC when service is connected
            if (!myServerIP.isEmpty() && !myMacAddress.isEmpty()) {
                String ServerIpAndMac = myUserId + "/" + myServerIP + "/" + myMacAddress;
                generateQRCode(ServerIpAndMac);
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
                    String scanResult = result.getContents();
                    ipToConnect = scanResult;
                    String[] parts = scanResult.split("/");
                    targetUserId = Integer.parseInt(parts[0]);
                    ipToConnect = parts[1];
                    if (serverMac.isEmpty())serverMac = parts[2];
                    showSnackbar("Connected to: " + ipToConnect+" "+serverMac);
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
            Toast.makeText(getApplicationContext(), "Logs", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(),LogsActivity.class);
            intent.putExtra("userId",myUserId);
            startActivity(intent);
        }if (item.getItemId() == R.id.menu_logout) { // Add this item in your menu XML
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        sessionManager.logout(); // This will clear session and redirect to login
    }
    public static void showTransferControlButton() {
        if (btnPauseResume != null && btnCancel != null) {
            btnPauseResume.setEnabled(true);
            btnCancel.setEnabled(true);
            btnPauseResume.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);
        }
    }

    public static void hideTransferControlButton() {
        if (btnPauseResume != null && btnCancel != null) {
            btnPauseResume.setEnabled(false);
            btnCancel.setEnabled(false);
            btnPauseResume.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
        }
    }
}