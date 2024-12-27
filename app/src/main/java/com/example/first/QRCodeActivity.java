package com.example.first;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class QRCodeActivity extends AppCompatActivity {
    private ImageView qrImageView;

    // Permission request launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startScanner();
                } else {
                    Toast.makeText(this, "Camera permission required for scanning", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // Scanner result launcher
    private final ActivityResultLauncher<ScanOptions> scanLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String content = result.getContents();
                    Toast.makeText(this, "Scanned: " + content, Toast.LENGTH_LONG).show();
                }
            }
    );

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        qrImageView = findViewById(R.id.qrImageView);
        Button generateButton = findViewById(R.id.generateButton);
        Button scanButton = findViewById(R.id.scanButton);

        // Generate QR code button
        generateButton.setOnClickListener(v -> generateQRCode("https://example.com"));

        // Scan QR code button
        scanButton.setOnClickListener(v -> checkCameraPermissionAndStartScanner());
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
            qrImageView.setImageBitmap(bitmap);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "QR Code generation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCameraPermissionAndStartScanner() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            startScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startScanner() {
        ScanOptions options = new ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan a QR Code")
                .setBeepEnabled(true)
                .setBarcodeImageEnabled(true)
                .setOrientationLocked(true)
                .setOrientationLocked(true)
                .setCaptureActivity(CaptureActivityPortrait.class);
        scanLauncher.launch(options);// Add this line
    }
}