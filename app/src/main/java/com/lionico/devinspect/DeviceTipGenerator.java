package com.lionico.devinspect;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StatFs;
import android.os.Environment;
import android.os.Build;
import java.text.DecimalFormat;

public class DeviceTipGenerator {

    public static String generateStorageTips() {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            int percentageAvailable = (int) ((availableBlocks * 100) / totalBlocks);

            if (percentageAvailable < 10) {
                return "⚠️ Storage Critical! Less than 10% free. Clear cache, delete unused apps.";
            } else if (percentageAvailable < 20) {
                return "Storage getting low. Consider moving photos/videos to cloud.";
            } else if (percentageAvailable < 30) {
                return "Manage storage: Large files and apps take most space.";
            } else {
                return "Good storage availability. Keep at least 20% free for optimal performance.";
            }
        } catch (Exception e) {
            return "Unable to analyze storage usage.";
        }
    }

    public static String generateRamTips(Context context) {
        try {
            if (context == null) return "Cannot analyze RAM usage.";

            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return "Cannot analyze RAM usage.";

            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long totalRamGB = mi.totalMem / (1024 * 1024 * 1024);
            long availableRamGB = mi.availMem / (1024 * 1024 * 1024);
            int percentageUsed = (int) (((totalRamGB - availableRamGB) * 100) / totalRamGB);

            if (percentageUsed > 85) {
                return "⚠️ High RAM usage! Close background apps for better performance.";
            } else if (percentageUsed > 65) {
                return "Moderate RAM usage. Your device is managing memory well.";
            } else {
                return "Good RAM availability. No need to close apps.";
            }
        } catch (Exception e) {
            return "Cannot analyze RAM usage.";
        }
    }

    public static String generateAndroidTips() {
        int sdk = Build.VERSION.SDK_INT;

        if (sdk < 24) {
            return "⚠️ Security Risk: Android version very old. Consider upgrading device.";
        } else if (sdk < 28) {
            return "Android version outdated. Update for better security and features.";
        } else if (sdk < 30) {
            return "Recent Android version. Keep updated for security patches.";
        } else {
            return "✓ Modern Android version. You have good security and features.";
        }
    }

    public static String generateNetworkTips(Context context) {
        if (context == null) return "Cannot analyze network.";

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "Cannot analyze network.";

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
            return "⚠️ No internet connection. Connect to Wi-Fi or mobile data.";
        }

        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            return "✓ Connected to Wi-Fi. Best for downloads and streaming.";
        } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
            return "Using mobile data. Watch data usage for large files.";
        }

        return "Network connection active.";
    }

    public static String generateBatteryTips() {
        // Note: For actual battery info, you'd need BATTERY_STATS permission
        // This is a generic tip
        return "• Charge between 20-80% for battery health\n" +
			"• Avoid extreme temperatures\n" +
			"• Use original charger when possible";
    }

    public static String generateGeneralMaintenanceTips() {
        return "🛠️ Device Maintenance:\n" +
			"• Restart weekly for fresh start\n" +
			"• Keep apps updated\n" +
			"• Clear app cache monthly\n" +
			"• Uninstall unused apps\n" +
			"• Backup important data regularly";
    }

    public static String generateAllTips(Context context) {
        StringBuilder allTips = new StringBuilder();

        allTips.append("📱 Device Health Tips\n\n");
        allTips.append("Storage: ").append(generateStorageTips()).append("\n\n");
        allTips.append("RAM: ").append(generateRamTips(context)).append("\n\n");
        allTips.append("Android: ").append(generateAndroidTips()).append("\n\n");
        allTips.append("Network: ").append(generateNetworkTips(context)).append("\n\n");
        allTips.append("Battery Care:\n").append(generateBatteryTips()).append("\n\n");
        allTips.append(generateGeneralMaintenanceTips());

        return allTips.toString();
    }
}
