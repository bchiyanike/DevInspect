package com.lionico.devinspect;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.util.DisplayMetrics;
import android.os.Build;
import android.os.StatFs;
import android.os.Environment;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.app.ActivityManager;
import android.graphics.Color;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeviceInfoFragment extends Fragment {

    // UI Components
    private TextView tvDeviceModel, tvManufacturer, tvAndroidVersion;
    private TextView tvScreenResolution, tvScreenDensity;
    private TextView tvInternalStorage, tvRamInfo;
    private TextView tvKernelVersion, tvBuildNumber;
    private TextView tvNetworkType, tvWifiSSID, tvWifiIP, tvLinkSpeed;

    // Tips TextViews
    private TextView tvDeviceModelTip, tvManufacturerTip, tvAndroidVersionTip;
    private TextView tvScreenResolutionTip, tvScreenDensityTip;
    private TextView tvStorageTip, tvRamTip;
    private TextView tvNetworkTypeTip, tvWifiTip, tvIpTip, tvSpeedTip;
    private TextView tvKernelTip, tvBuildTip;

    // Rating and time
    private TextView tvDeviceRating, tvDeviceRatingTip, tvLastUpdated;

    // Progress bars
    private ProgressBar storageProgressBar, ramProgressBar;
    private TextView tvStoragePercentage, tvRamPercentage;

    // Buttons
    private Button btnRefreshDeviceInfo, btnDeviceTips;

    // Display metrics for scoring
    private DisplayMetrics displayMetrics;

    public DeviceInfoFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_info, container, false);

        // Get display metrics
        displayMetrics = new DisplayMetrics();
        if (getActivity() != null) {
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        }

        initializeViews(view);
        setupButtonListeners();
        populateDeviceInfo();
        updateLastUpdatedTime();

        return view;
    }

    private void initializeViews(View view) {
        // Initialize all views (same as before, but shortened)
        tvDeviceModel = (TextView) view.findViewById(R.id.tvDeviceModel);
        tvManufacturer = (TextView) view.findViewById(R.id.tvManufacturer);
        tvAndroidVersion = (TextView) view.findViewById(R.id.tvAndroidVersion);
        tvScreenResolution = (TextView) view.findViewById(R.id.tvScreenResolution);
        tvScreenDensity = (TextView) view.findViewById(R.id.tvScreenDensity);
        tvInternalStorage = (TextView) view.findViewById(R.id.tvInternalStorage);
        tvRamInfo = (TextView) view.findViewById(R.id.tvRamInfo);
        tvKernelVersion = (TextView) view.findViewById(R.id.tvKernelVersion);
        tvBuildNumber = (TextView) view.findViewById(R.id.tvBuildNumber);
        tvNetworkType = (TextView) view.findViewById(R.id.tvNetworkType);
        tvWifiSSID = (TextView) view.findViewById(R.id.tvWifiSSID);
        tvWifiIP = (TextView) view.findViewById(R.id.tvWifiIP);
        tvLinkSpeed = (TextView) view.findViewById(R.id.tvLinkSpeed);

        tvDeviceModelTip = (TextView) view.findViewById(R.id.tvDeviceModelTip);
        tvManufacturerTip = (TextView) view.findViewById(R.id.tvManufacturerTip);
        tvAndroidVersionTip = (TextView) view.findViewById(R.id.tvAndroidVersionTip);
        tvScreenResolutionTip = (TextView) view.findViewById(R.id.tvScreenResolutionTip);
        tvScreenDensityTip = (TextView) view.findViewById(R.id.tvScreenDensityTip);
        tvStorageTip = (TextView) view.findViewById(R.id.tvStorageTip);
        tvRamTip = (TextView) view.findViewById(R.id.tvRamTip);
        tvNetworkTypeTip = (TextView) view.findViewById(R.id.tvNetworkTypeTip);
        tvWifiTip = (TextView) view.findViewById(R.id.tvWifiTip);
        tvIpTip = (TextView) view.findViewById(R.id.tvIpTip);
        tvSpeedTip = (TextView) view.findViewById(R.id.tvSpeedTip);
        tvKernelTip = (TextView) view.findViewById(R.id.tvKernelTip);
        tvBuildTip = (TextView) view.findViewById(R.id.tvBuildTip);

        tvDeviceRating = (TextView) view.findViewById(R.id.tvDeviceRating);
        tvDeviceRatingTip = (TextView) view.findViewById(R.id.tvDeviceRatingTip);
        tvLastUpdated = (TextView) view.findViewById(R.id.tvLastUpdated);

        storageProgressBar = (ProgressBar) view.findViewById(R.id.storageProgressBar);
        ramProgressBar = (ProgressBar) view.findViewById(R.id.ramProgressBar);
        tvStoragePercentage = (TextView) view.findViewById(R.id.tvStoragePercentage);
        tvRamPercentage = (TextView) view.findViewById(R.id.tvRamPercentage);

        btnRefreshDeviceInfo = (Button) view.findViewById(R.id.btnRefreshDeviceInfo);
        btnDeviceTips = (Button) view.findViewById(R.id.btnDeviceTips);
    }

    private void setupButtonListeners() {
        btnRefreshDeviceInfo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					refreshAllInformation();
					Toast.makeText(getActivity(), "Information refreshed", Toast.LENGTH_SHORT).show();
				}
			});

        btnDeviceTips.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showDeviceTips();
				}
			});
    }

    private void populateDeviceInfo() {
        // Basic device info
        tvDeviceModel.setText(Build.MODEL);
        tvDeviceModelTip.setText("Your phone's specific model");

        tvManufacturer.setText(Build.MANUFACTURER);
        tvManufacturerTip.setText(getManufacturerContext(Build.MANUFACTURER));

        String androidVersion = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        tvAndroidVersion.setText(androidVersion);
        tvAndroidVersionTip.setText(getAndroidVersionContext(Build.VERSION.SDK_INT));

        // Screen info
        if (displayMetrics != null) {
            int width = displayMetrics.widthPixels;
            int height = displayMetrics.heightPixels;
            float density = displayMetrics.density;

            tvScreenResolution.setText(width + " × " + height);
            tvScreenResolutionTip.setText(getResolutionContext(width, height));

            tvScreenDensity.setText(getDensityName(density) + " (" + String.format("%.1f", density) + "x)");
            tvScreenDensityTip.setText(getDensityContext(density));
        }

        // System info
        tvKernelVersion.setText(System.getProperty("os.version"));
        tvKernelTip.setText("Core system managing hardware and software");

        tvBuildNumber.setText(Build.DISPLAY);
        tvBuildTip.setText("Unique software identifier");

        // Dynamic info
        updateStorageInfo();
        updateRamInfo();
        updateNetworkInfo();
        updateDeviceRating();
    }

    private void updateStorageInfo() {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();

            long totalSize = totalBlocks * blockSize;
            long usedSize = totalSize - (availableBlocks * blockSize);
            int percentageUsed = (int) ((usedSize * 100) / totalSize);

            DecimalFormat df = new DecimalFormat("#.##");
            double totalGB = totalSize / (1024.0 * 1024.0 * 1024.0);
            double usedGB = usedSize / (1024.0 * 1024.0 * 1024.0);

            tvInternalStorage.setText(df.format(usedGB) + " GB used / " + df.format(totalGB) + " GB total");
            storageProgressBar.setProgress(percentageUsed);
            tvStoragePercentage.setText(percentageUsed + "%");
            tvStorageTip.setText(DeviceTipGenerator.generateStorageTips());

            // Color code storage tip based on usage
            if (percentageUsed > 90) {
                tvStorageTip.setTextColor(Color.RED);
            } else if (percentageUsed > 70) {
                tvStorageTip.setTextColor(Color.parseColor("#FF9800")); // Orange
            } else {
                tvStorageTip.setTextColor(Color.parseColor("#4CAF50")); // Green
            }

        } catch (Exception e) {
            tvInternalStorage.setText("Storage info unavailable");
            tvStorageTip.setText("Unable to read storage");
            tvStorageTip.setTextColor(Color.GRAY);
        }
    }

    private void updateRamInfo() {
        if (getActivity() == null) return;

        try {
            ActivityManager am = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;

            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);

            long totalRam = mi.totalMem;
            long availableRam = mi.availMem;
            long usedRam = totalRam - availableRam;
            int percentageUsed = (int) ((usedRam * 100) / totalRam);

            if (totalRam > 0) {
                double totalGB = totalRam / (1024.0 * 1024.0 * 1024.0);
                DecimalFormat df = new DecimalFormat("#.##");

                tvRamInfo.setText(df.format(totalGB) + " GB total RAM");
                ramProgressBar.setProgress(percentageUsed);
                tvRamPercentage.setText(percentageUsed + "%");
                tvRamTip.setText(DeviceTipGenerator.generateRamTips(getActivity()));

                // Color code RAM tip based on usage
                if (percentageUsed > 85) {
                    tvRamTip.setTextColor(Color.RED);
                } else if (percentageUsed > 65) {
                    tvRamTip.setTextColor(Color.parseColor("#FF9800")); // Orange
                } else {
                    tvRamTip.setTextColor(Color.parseColor("#4CAF50")); // Green
                }
            }
        } catch (Exception e) {
            tvRamInfo.setText("RAM info unavailable");
            tvRamTip.setText("Unable to read RAM");
            tvRamTip.setTextColor(Color.GRAY);
        }
    }

    private void updateNetworkInfo() {
        if (getActivity() == null) return;

        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            tvNetworkType.setText("Not Connected");
            tvNetworkTypeTip.setText("Connect to Wi-Fi or mobile data");
            tvWifiSSID.setText("N/A");
            tvWifiIP.setText("N/A");
            tvLinkSpeed.setText("N/A");
            tvWifiTip.setText("No network connection");
            tvIpTip.setText("IP unavailable offline");
            tvSpeedTip.setText("Speed unavailable");
            return;
        }

        String type = activeNetwork.getTypeName();
        tvNetworkType.setText(type + " Connected");
        tvNetworkTypeTip.setText(getNetworkTypeContext(activeNetwork.getType()));

        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    String ssid = wifiInfo.getSSID();
                    if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    tvWifiSSID.setText(ssid != null ? ssid : "Unknown");
                    tvWifiTip.setText("Connected wireless network");

                    String ip = intToIp(wifiInfo.getIpAddress());
                    tvWifiIP.setText(ip);
                    tvIpTip.setText("Device network address");

                    int speed = wifiInfo.getLinkSpeed();
                    tvLinkSpeed.setText(speed + " " + WifiInfo.LINK_SPEED_UNITS);
                    tvSpeedTip.setText(getWifiSpeedContext(speed));
                    return;
                }
            }
        }

        // Mobile data
        tvWifiSSID.setText(activeNetwork.getSubtypeName());
        tvWifiTip.setText("Mobile data network");
        tvWifiIP.setText("Mobile Data IP");
        tvIpTip.setText("Dynamic mobile IP");

        int speedClass = getMobileNetworkClass(activeNetwork.getSubtype());
        tvLinkSpeed.setText(getMobileSpeedText(speedClass));
        tvSpeedTip.setText(getMobileNetworkTip(speedClass));
    }

    private void updateDeviceRating() {
        if (getActivity() == null || displayMetrics == null) return;

        DeviceScorer.ScoreResult result = DeviceScorer.calculateDeviceScore(getActivity(), displayMetrics);

        tvDeviceRating.setText(result.ratingText + " (" + result.totalScore + "/100)");
        tvDeviceRating.setTextColor(result.color);
        tvDeviceRatingTip.setText(result.performanceLevel);
    }

    private void refreshAllInformation() {
        updateStorageInfo();
        updateRamInfo();
        updateNetworkInfo();
        updateDeviceRating();
        updateLastUpdatedTime();
    }

    private void updateLastUpdatedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
        tvLastUpdated.setText("Last updated: " + sdf.format(new Date()));
    }

    private void showDeviceTips() {
        if (getActivity() == null) return;

        String tips = DeviceTipGenerator.generateAllTips(getActivity());
        Toast.makeText(getActivity(), tips, Toast.LENGTH_LONG).show();
    }

    // Helper methods (kept concise)
    private String getManufacturerContext(String manufacturer) {
        manufacturer = manufacturer.toLowerCase();
        if (manufacturer.contains("samsung")) return "World's largest smartphone maker";
        else if (manufacturer.contains("google")) return "Android creator, Pixel maker";
        else if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) return "Chinese tech giant";
        else if (manufacturer.contains("oneplus")) return "Known for fast performance";
        else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) return "Chinese telecommunications";
        else return "Device manufacturer";
    }

    private String getAndroidVersionContext(int sdk) {
        if (sdk >= 30) return "Latest with best security";
        else if (sdk >= 28) return "Modern with good features";
        else if (sdk >= 24) return "Functional but aging";
        else return "Consider updating";
    }

    private String getResolutionContext(int width, int height) {
        int totalPixels = width * height;
        if (totalPixels >= 4000000) return "4K+ excellent display";
        else if (totalPixels >= 2000000) return "Full HD clarity";
        else if (totalPixels >= 1000000) return "HD good for daily use";
        else return "Basic resolution";
    }

    private String getDensityContext(float density) {
        if (density >= 3.0) return "Super sharp display";
        else if (density >= 2.0) return "High clarity";
        else if (density >= 1.5) return "Standard sharpness";
        else return "Basic sharpness";
    }

    private String getNetworkTypeContext(int type) {
        if (type == ConnectivityManager.TYPE_WIFI) return "Wi-Fi for fast data";
        else if (type == ConnectivityManager.TYPE_MOBILE) return "Mobile data on the go";
        else return "Network connection";
    }

    private String getWifiSpeedContext(int speed) {
        if (speed >= 300) return "Very fast connection";
        else if (speed >= 150) return "Fast connection";
        else if (speed >= 50) return "Moderate speed";
        else return "Basic speed";
    }

    private String intToIp(int ip) {
        return ((ip >> 0) & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." +
			((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }

    private int getMobileNetworkClass(int networkType) {
        switch (networkType) {
            case 1: // GPRS
            case 2: // EDGE
                return 1; // 2G
            case 3: // UMTS
            case 8: // HSDPA
            case 9: // HSUPA
            case 10: // HSPA
                return 2; // 3G
            case 13: // LTE
                return 3; // 4G
            case 20: // 5G NR
                return 4; // 5G
            default:
                return 0; // Unknown
        }
    }

    private String getMobileSpeedText(int speedClass) {
        switch (speedClass) {
            case 1: return "2G";
            case 2: return "3G";
            case 3: return "4G";
            case 4: return "5G";
            default: return "Unknown";
        }
    }

    private String getMobileNetworkTip(int speedClass) {
        switch (speedClass) {
            case 1: return "2G for calls/texts";
            case 2: return "3G basic browsing";
            case 3: return "4G fast data";
            case 4: return "5G ultra-fast";
            default: return "Mobile data";
        }
    }

    private String getDensityName(float density) {
        if (density >= 4.0) return "xxxhdpi";
        else if (density >= 3.0) return "xxhdpi";
        else if (density >= 2.0) return "xhdpi";
        else if (density >= 1.5) return "hdpi";
        else if (density >= 1.0) return "mdpi";
        else return "ldpi";
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAllInformation();
    }
}
