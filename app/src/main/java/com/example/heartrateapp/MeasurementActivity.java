package com.example.heartrateapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MeasurementActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private Camera camera;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private int currentHeartRate = 0; // Tu będziemy zapisywać wynik z algorytmu
    private TextView txtTimer;
    private android.os.CountDownTimer measurementTimer;

    private Button btnStart;
    private Button btnInstruction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);

        viewFinder = findViewById(R.id.viewFinder);
        txtTimer = findViewById(R.id.txtTimer);
        btnStart = findViewById(R.id.btnStartMeasurement);
        btnInstruction = findViewById(R.id.btnShowInstruction);

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        btnInstruction.setOnClickListener(v -> showInstructionDialog());

        btnStart.setOnClickListener(v -> {
            btnStart.setVisibility(View.GONE);
            btnInstruction.setVisibility(View.GONE);

            currentHeartRate = 0;
            txtTimer.setText("Przygotowanie...");
            viewFinder.postDelayed(() -> startTimer(), 1000);
        });
    }

    private void showInstructionDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.activity_instructions, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.show();

            Button btnClose = dialogView.findViewById(R.id.btnCloseDialog);
            btnClose.setOnClickListener(v -> dialog.dismiss());

        } catch (Exception e) {
            Toast.makeText(this, "Błąd otwarcia instrukcji: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                androidx.camera.core.ImageAnalysis imageAnalysis =
                        new androidx.camera.core.ImageAnalysis.Builder()
                                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setTargetResolution(new android.util.Size(640, 480))
                                .build();

                //Tutaj podpinamy PulseAnalyzer
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new PulseAnalyzer(bpm -> {
                    currentHeartRate = bpm;
                }));

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                try {
                    cameraProvider.unbindAll();
                    camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                    if (camera.getCameraInfo().hasFlashUnit()) {
                        camera.getCameraControl().enableTorch(true);
                    }
                    currentHeartRate = 0;

                } catch (Exception e) {
                    Toast.makeText(this, "Błąd kamery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startTimer() {
        measurementTimer = new android.os.CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                txtTimer.setText("Pozostało: " + secondsRemaining + " s");
            }

            @Override
            public void onFinish() {
                txtTimer.setText("Gotowe!");
                if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
                    camera.getCameraControl().enableTorch(false);
                }
                Intent intent = new Intent(MeasurementActivity.this, ResultActivity.class);
                if (currentHeartRate > 0) {
                    intent.putExtra("RESULT_TEXT", String.valueOf(currentHeartRate));
                } else {
                    intent.putExtra("RESULT_TEXT", "Błąd pomiaru");
                }
                startActivity(intent);
                finish();
            }
        };
        measurementTimer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (measurementTimer != null) { measurementTimer.cancel(); }
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(false);
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Wymagany dostęp do kamery!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}