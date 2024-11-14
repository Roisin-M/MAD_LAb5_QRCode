package com.example.qrcode;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import android.os.AsyncTask;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText etAddress, etName;
    private Button buttonScan;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    // Register the launcher and result handler
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    String scannedUrl = result.getContents();
                    Toast.makeText(MainActivity.this, "Scanned: " + scannedUrl, Toast.LENGTH_LONG).show();

                    // Populate the EditText with the scanned URL
                    etAddress.setText(scannedUrl);

                    // Fetch and display the website title in etName
                    fetchWebsiteTitle(scannedUrl);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        etAddress = findViewById(R.id.etAddress);
        etName = findViewById(R.id.etName);
        buttonScan = findViewById(R.id.buttonScan);

        // Check camera permission
        checkCameraPermission();

        // Set the button click listener to start the scanner
        buttonScan.setOnClickListener(this::onButtonClick);
        // Handle click on the EditText to open the scanned URL
        etAddress.setOnClickListener(this::onAddressClick);
    }

    // Method to check and request camera permission
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to launch the barcode scanner
    public void onButtonClick(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan a QR code");
            options.setCameraId(0); // Use the default camera
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(false);
            barcodeLauncher.launch(options);
        } else {
            Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show();
        }
    }

    public void onAddressClick(View view){
        // Get the text from the EditText (which is the scanned URL)
        String url = etAddress.getText().toString();

        // Check if the URL is not empty
        if (!url.isEmpty()) {
            // Ensure the URL starts with "http://" or "https://"
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            // Create an Intent to open the URL in a browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(browserIntent);
        } else {
            Toast.makeText(MainActivity.this, "No URL to open", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to fetch and set the title in etName using Executors
    public void fetchWebsiteTitle(String url) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executorService.execute(() -> {
            try {
                // Connect to the URL and fetch the document
                Document document = Jsoup.connect(url).get();
                String title = document.title();

                // Post the result to the main thread
                handler.post(() -> {
                    if (title != null && !title.isEmpty()) {
                        etName.setText(title);
                    } else {
                        etName.setText("No title found");
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> etName.setText("Error fetching title"));
            }
        });
    }

}