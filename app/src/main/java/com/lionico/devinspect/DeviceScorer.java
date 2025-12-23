package com.lionico.devinspect;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.StatFs;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.graphics.Color;
import java.util.HashMap;
import java.util.Map;

public class DeviceScorer {

    public static class ScoreResult {
        public int totalScore;
        public String ratingText;
        public int color;
        public String performanceLevel;
        public Map<String, Integer> categoryScores;
        public Map<String, String> categoryDetails;

        public ScoreResult() {
            categoryScores = new HashMap<String, Integer>();
            categoryDetails = new HashMap<String, String>();
        }
    }

    // Weight constants
    private static final int WEIGHT_ANDROID = 25;
    private static final int WEIGHT_RAM = 30;
    private static final int WEIGHT_STORAGE = 20;
    private static final int WEIGHT_SCREEN = 15;
    private static final int WEIGHT_CPU = 10;

    public static ScoreResult calculateDeviceScore(Context context, DisplayMetrics displayMetrics) {
        ScoreResult result = new ScoreResult();

        // Calculate individual category scores
        int androidScore = calculateAndroidScore();
        int ramScore = calculateRamScore(context);
        int storageScore = calculateStorageScore();
        int screenScore = calculateScreenScore(displayMetrics);
        int cpuScore = calculateCpuScore();

        // Store individual scores
        result.categoryScores.put("android", androidScore);
        result.categoryScores.put("ram", ramScore);
        result.categoryScores.put("storage", storageScore);
        result.categoryScores.put("screen", screenScore);
        result.categoryScores.put("cpu", cpuScore);

        // Store details
        result.categoryDetails.put("android", getAndroidDetails(androidScore));
        result.categoryDetails.put("ram", getRamDetails(ramScore, context));
        result.categoryDetails.put("storage", getStorageDetails(storageScore));
        result.categoryDetails.put("screen", getScreenDetails(screenScore, displayMetrics));
        result.categoryDetails.put("cpu", getCpuDetails(cpuScore));

        // Calculate weighted total
        int totalScore = (androidScore * WEIGHT_ANDROID / 25) +
			(ramScore * WEIGHT_RAM / 30) +
			(storageScore * WEIGHT_STORAGE / 20) +
			(screenScore * WEIGHT_SCREEN / 15) +
			(cpuScore * WEIGHT_CPU / 10);

        // Normalize to 0-100
        totalScore = Math.min(100, Math.max(0, totalScore));
        result.totalScore = totalScore;

        // Determine rating
        if (totalScore >= 85) {
            result.ratingText = "Excellent";
            result.color = Color.parseColor("#4CAF50");
            result.performanceLevel = "High-performance device";
        } else if (totalScore >= 70) {
            result.ratingText = "Good";
            result.color = Color.parseColor("#8BC34A");
            result.performanceLevel = "Capable device for most tasks";
        } else if (totalScore >= 55) {
            result.ratingText = "Average";
            result.color = Color.parseColor("#FFC107");
            result.performanceLevel = "Suitable for everyday use";
        } else if (totalScore >= 40) {
            result.ratingText = "Basic";
            result.color = Color.parseColor("#FF9800");
            result.performanceLevel = "Entry-level performance";
        } else {
            result.ratingText = "Limited";
            result.color = Color.parseColor("#F44336");
            result.performanceLevel = "May struggle with demanding apps";
        }

        return result;
    }

    private static int calculateAndroidScore() {
        int sdk = Build.VERSION.SDK_INT;

        if (sdk >= 33) return 25;      // Android 13+
        else if (sdk >= 31) return 22; // Android 12-12L
        else if (sdk >= 30) return 20; // Android 11
        else if (sdk >= 29) return 18; // Android 10
        else if (sdk >= 28) return 15; // Android 9
        else if (sdk >= 26) return 12; // Android 8
        else if (sdk >= 24) return 10; // Android 7
        else if (sdk >= 23) return 8;  // Android 6
        else return 5;                 // Older
    }

    private static int calculateRamScore(Context context) {
        try {
            if (context == null) return 10;

            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return 10;

            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long totalRamGB = mi.totalMem / (1024 * 1024 * 1024);

            if (totalRamGB >= 12) return 30;      // 12GB+
            else if (totalRamGB >= 8) return 27;  // 8GB
            else if (totalRamGB >= 6) return 24;  // 6GB
            else if (totalRamGB >= 4) return 20;  // 4GB
            else if (totalRamGB >= 3) return 16;  // 3GB
            else if (totalRamGB >= 2) return 12;  // 2GB
            else return 8;                        // <2GB

        } catch (Exception e) {
            return 10;
        }
    }

    private static int calculateStorageScore() {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long totalBytes = stat.getBlockCountLong() * stat.getBlockSizeLong();
            long totalGB = totalBytes / (1024 * 1024 * 1024);

            if (totalGB >= 256) return 20;    // 256GB+
            else if (totalGB >= 128) return 18; // 128GB
            else if (totalGB >= 64) return 16;  // 64GB
            else if (totalGB >= 32) return 14;  // 32GB
            else if (totalGB >= 16) return 12;  // 16GB
            else return 10;                     // <16GB

        } catch (Exception e) {
            return 10;
        }
    }

    private static int calculateScreenScore(DisplayMetrics displayMetrics) {
        try {
            if (displayMetrics == null) return 8;

            int width = displayMetrics.widthPixels;
            int height = displayMetrics.heightPixels;
            int totalPixels = width * height;
            float density = displayMetrics.density;

            int score = 0;

            // Resolution score
            if (totalPixels >= 4000000) score += 10;    // 4K+
            else if (totalPixels >= 2000000) score += 8; // Full HD
            else if (totalPixels >= 1000000) score += 6; // HD
            else score += 4;                            // Lower

            // Density bonus
            if (density >= 3.0) score += 5;    // xxhdpi+
            else if (density >= 2.0) score += 3; // xhdpi
            else if (density >= 1.5) score += 2; // hdpi
            else score += 1;                    // mdpi or lower

            return Math.min(15, score);

        } catch (Exception e) {
            return 8;
        }
    }

    private static int calculateCpuScore() {
        String cpuAbi = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String[] abis = Build.SUPPORTED_ABIS;
            if (abis != null && abis.length > 0) {
                cpuAbi = abis[0].toLowerCase();
            }
        } else {
            cpuAbi = Build.CPU_ABI.toLowerCase();
        }

        if (cpuAbi.contains("arm64-v8a")) return 10;
        else if (cpuAbi.contains("armeabi-v7a")) return 7;
        else if (cpuAbi.contains("x86_64")) return 8;
        else if (cpuAbi.contains("x86")) return 6;
        else return 5;
    }

    private static String getAndroidDetails(int score) {
        if (score >= 22) return "Latest Android version";
        else if (score >= 18) return "Modern Android version";
        else if (score >= 12) return "Recent Android version";
        else if (score >= 8) return "Older Android version";
        else return "Outdated Android version";
    }

    private static String getRamDetails(int score, Context context) {
        try {
            if (context == null) return "Unknown RAM";

            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return "Unknown RAM";

            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long totalRamGB = mi.totalMem / (1024 * 1024 * 1024);

            return totalRamGB + " GB RAM";

        } catch (Exception e) {
            return "Unknown RAM";
        }
    }

    private static String getStorageDetails(int score) {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long totalBytes = stat.getBlockCountLong() * stat.getBlockSizeLong();
            long totalGB = totalBytes / (1024 * 1024 * 1024);

            return totalGB + " GB storage";

        } catch (Exception e) {
            return "Unknown storage";
        }
    }

    private static String getScreenDetails(int score, DisplayMetrics displayMetrics) {
        if (displayMetrics == null) return "Unknown display";

        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        return width + "x" + height + " display";
    }

    private static String getCpuDetails(int score) {
        if (score >= 8) return "64-bit processor";
        else if (score >= 6) return "Modern 32-bit processor";
        else return "Basic processor";
    }

    public static String generatePerformanceTips(ScoreResult result, Context context) {
        StringBuilder tips = new StringBuilder();
        tips.append("Performance Analysis:\n\n");

        // Android tips
        Integer androidScore = result.categoryScores.get("android");
        if (androidScore != null && androidScore < 15) {
            tips.append("• Update Android: Your version is outdated for security and features\n");
        } else if (androidScore != null && androidScore >= 20) {
            tips.append("• ✓ Android: You're running a recent, secure version\n");
        }

        // RAM tips
        Integer ramScore = result.categoryScores.get("ram");
        if (ramScore != null && ramScore < 16) {
            tips.append("• Manage RAM: Close unused apps, limit background processes\n");
        } else if (ramScore != null && ramScore >= 24) {
            tips.append("• ✓ RAM: Plenty of memory for multitasking\n");
        }

        // Storage tips
        Integer storageScore = result.categoryScores.get("storage");
        if (storageScore != null && storageScore < 14) {
            tips.append("• Storage: Consider cloud services or external storage\n");
        }

        // Screen tips
        Integer screenScore = result.categoryScores.get("screen");
        if (screenScore != null && screenScore < 10) {
            tips.append("• Display: Lower resolution - good for battery life\n");
        } else if (screenScore != null && screenScore >= 12) {
            tips.append("• ✓ Display: Excellent screen quality\n");
        }

        // CPU tips
        Integer cpuScore = result.categoryScores.get("cpu");
        if (cpuScore != null && cpuScore < 7) {
            tips.append("• Processor: May struggle with heavy apps/games\n");
        }

        // Overall performance level
        tips.append("\nOverall: ").append(result.performanceLevel);

        return tips.toString();
    }
}
