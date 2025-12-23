package com.lionico.devinspect;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraAccessException;
import android.content.pm.PackageManager;
import android.os.Build;

public class QuickToolsFragment extends Fragment {

    private Button btnClipboardCopy, btnClipboardPaste, btnOpenSettings;
    private Button btnShareText, btnOpenBrowser, btnFlashlight;
    private TextView tvClipboardContent, tvFlashlightStatus, tvFlashlightInfo;
    private ClipboardManager clipboard;

    // Flashlight variables
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashlightOn = false;
    private boolean hasFlashlight = false;
    private boolean isFlashlightAvailable = false;

    public QuickToolsFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quick_tools, container, false);

        initializeViews(view);
        setupButtons();
        initializeFlashlight();

        return view;
    }

    private void initializeViews(View view) {
        btnClipboardCopy = view.findViewById(R.id.btnClipboardCopy);
        btnClipboardPaste = view.findViewById(R.id.btnClipboardPaste);
        btnOpenSettings = view.findViewById(R.id.btnOpenSettings);
        btnShareText = view.findViewById(R.id.btnShareText);
        btnOpenBrowser = view.findViewById(R.id.btnOpenBrowser);
        btnFlashlight = view.findViewById(R.id.btnFlashlight);
        tvClipboardContent = view.findViewById(R.id.tvClipboardContent);
        tvFlashlightStatus = view.findViewById(R.id.tvFlashlightStatus);
        tvFlashlightInfo = view.findViewById(R.id.tvFlashlightInfo);

        clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private void initializeFlashlight() {
        if (getActivity() != null) {
            hasFlashlight = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

            if (!hasFlashlight) {
                setFlashlightUnavailable("No flashlight hardware");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
                    if (cameraManager != null && cameraManager.getCameraIdList().length > 0) {
                        cameraId = cameraManager.getCameraIdList()[0];
                        isFlashlightAvailable = true;
                        updateFlashlightStatusText("Ready", R.color.primaryGreen);
                        // Initialize flashlight as OFF
                        isFlashlightOn = false;
                        updateFlashlightUI();
                    } else {
                        setFlashlightUnavailable("Camera service error");
                    }
                } catch (Exception e) {
                    setFlashlightUnavailable("Initialization error");
                    e.printStackTrace();
                }
            } else {
                setFlashlightUnavailable("Requires Android 6.0+");
            }
        }
    }

    private void setFlashlightUnavailable(String message) {
        btnFlashlight.setEnabled(false);
        btnFlashlight.setText("Flashlight Not Available");
        tvFlashlightStatus.setText("Status: " + message);
        tvFlashlightStatus.setTextColor(getResources().getColor(R.color.textHint));
        isFlashlightAvailable = false;
        tvFlashlightInfo.setVisibility(View.GONE);
    }

    private void updateFlashlightStatusText(String status, int colorResId) {
        if (getActivity() != null) {
            tvFlashlightStatus.setText("Status: " + status);
            tvFlashlightStatus.setTextColor(getResources().getColor(colorResId));
        }
    }

    private void setupButtons() {
        // Copy to Clipboard
        btnClipboardCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String textToCopy = "Device Inspector - " + 
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                    ClipData clip = ClipData.newPlainText("Device Inspector", textToCopy);
                    clipboard.setPrimaryClip(clip);
                    showToast("Text copied to clipboard");
                    updateClipboardDisplay();
                }
            });

        // Paste from Clipboard
        btnClipboardPaste.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateClipboardDisplay();
                    if (clipboard.hasPrimaryClip()) {
                        showToast("Clipboard content displayed");
                    }
                }
            });

        // Open Settings
        btnOpenSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_SETTINGS);
                        startActivity(intent);
                        showToast("Opening Settings");
                    } catch (Exception e) {
                        showToast("Cannot open settings");
                    }
                }
            });

        // Share Text
        btnShareText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        String shareText = "Device Information:\n";
                        shareText += "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n";
                        shareText += "Android: " + Build.VERSION.RELEASE + "\n";
                        shareText += "App: Device Inspector\n";
                        shareText += "Time: " + 
                            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Device Information");

                        startActivity(Intent.createChooser(shareIntent, "Share via"));
                    } catch (Exception e) {
                        showToast("No app available to share");
                    }
                }
            });

        // Open Browser
        btnOpenBrowser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                                                          Uri.parse("https://developer.android.com"));
                        startActivity(browserIntent);
                        showToast("Opening browser");
                    } catch (Exception e) {
                        showToast("No browser available");
                    }
                }
            });

        // Flashlight
        btnFlashlight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFlashlight();
                }
            });
    }

    private void toggleFlashlight() {
        if (!isFlashlightAvailable) {
            showToast("Flashlight is not available");
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraManager != null) {
                if (isFlashlightOn) {
                    turnOffFlashlight();
                } else {
                    turnOnFlashlight();
                }
            }
        } catch (Exception e) {
            showToast("Flashlight error");
            e.printStackTrace();
        }
    }

    private void turnOnFlashlight() throws CameraAccessException {
        cameraManager.setTorchMode(cameraId, true);
        isFlashlightOn = true;
        updateFlashlightUI();
        showToast("Flashlight turned ON");
    }

    private void turnOffFlashlight() throws CameraAccessException {
        cameraManager.setTorchMode(cameraId, false);
        isFlashlightOn = false;
        updateFlashlightUI();
        showToast("Flashlight turned OFF");
    }

    private void updateFlashlightUI() {
        if (isFlashlightOn) {
            btnFlashlight.setText("Turn Off Flashlight");
            btnFlashlight.setBackgroundResource(R.drawable.rounded_button_red);
            updateFlashlightStatusText("ON", R.color.lime_green);
            tvFlashlightInfo.setVisibility(View.VISIBLE);
        } else {
            btnFlashlight.setText("Turn On Flashlight");
            btnFlashlight.setBackgroundResource(R.drawable.rounded_button_green);
            updateFlashlightStatusText("OFF", R.color.textSecondary);
            tvFlashlightInfo.setVisibility(View.GONE);
        }
    }

    private void updateClipboardDisplay() {
        if (clipboard.hasPrimaryClip()) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            String text = item.getText().toString();

            if (text.length() > 60) {
                text = text.substring(0, 57) + "...";
            }

            tvClipboardContent.setText("📋 " + text);
            tvClipboardContent.setTextColor(getResources().getColor(R.color.textPrimary));
        } else {
            tvClipboardContent.setText("📋 Clipboard is empty");
            tvClipboardContent.setTextColor(getResources().getColor(R.color.textHint));
        }
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateClipboardDisplay();

        // When returning to fragment, ensure flashlight state is reset
        // We track it manually since we can't query it
        if (isFlashlightAvailable) {
            // For safety, assume flashlight is off when returning
            isFlashlightOn = false;
            updateFlashlightUI();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Turn off flashlight when leaving fragment
        if (isFlashlightOn && isFlashlightAvailable) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraId, false);
                }
                isFlashlightOn = false;
                // Update UI just in case we come back
                updateFlashlightUI();
            } catch (Exception e) {
                // Ignore errors when pausing
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Ensure flashlight is off
        if (isFlashlightOn && isFlashlightAvailable) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraId, false);
                }
                isFlashlightOn = false;
            } catch (Exception e) {
                // Ignore errors during destruction
            }
        }
    }
}
