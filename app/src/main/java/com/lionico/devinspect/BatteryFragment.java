package com.lionico.devinspect;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.graphics.Color;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BatteryFragment extends Fragment {

    // UI Components
    private TextView tvBatteryLevel, tvChargingStatus, tvBatteryHealth;
    private TextView tvBatteryTemperature, tvBatteryTechnology, tvBatteryVoltage;
    private TextView tvBatteryCapacity, tvChargeTimeRemaining, tvBatteryStatus;
    private TextView tvLastUpdated, tvEstimatedRemainingTime, tvBatteryUsageToday;
    private ProgressBar pbBatteryLevel;
    private Button btnRefreshBattery, btnBatteryOptimization, btnBatteryHistory;

    private BroadcastReceiver batteryReceiver;
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private Runnable periodicUpdateRunnable;
    private static final long UPDATE_INTERVAL = 30000; // 30 seconds

    // Statistics tracking
    private float lastBatteryLevel = 0;
    private long sessionStartTime = 0;
    private float sessionStartBattery = 0;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public BatteryFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_battery, container, false);

        initializeViews(view);
        setupRefreshButton();
        setupAdditionalButtons();
        registerBatteryReceiver();
        updateBatteryInfo();
        startPeriodicUpdates();

        // Start session tracking
        sessionStartTime = System.currentTimeMillis();

        return view;
    }

    private void initializeViews(View view) {
        // Existing views
        tvBatteryLevel = view.findViewById(R.id.tvBatteryLevel);
        tvChargingStatus = view.findViewById(R.id.tvChargingStatus);
        tvBatteryHealth = view.findViewById(R.id.tvBatteryHealth);
        tvBatteryTemperature = view.findViewById(R.id.tvBatteryTemperature);
        tvBatteryTechnology = view.findViewById(R.id.tvBatteryTechnology);
        tvBatteryVoltage = view.findViewById(R.id.tvBatteryVoltage);
        btnRefreshBattery = view.findViewById(R.id.btnRefreshBattery);

        // New views
        pbBatteryLevel = view.findViewById(R.id.pbBatteryLevel);
        tvBatteryCapacity = view.findViewById(R.id.tvBatteryCapacity);
        tvChargeTimeRemaining = view.findViewById(R.id.tvChargeTimeRemaining);
        tvBatteryStatus = view.findViewById(R.id.tvBatteryStatus);
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated);
        tvEstimatedRemainingTime = view.findViewById(R.id.tvEstimatedRemainingTime);
        tvBatteryUsageToday = view.findViewById(R.id.tvBatteryUsageToday);
        btnBatteryOptimization = view.findViewById(R.id.btnBatteryOptimization);
        btnBatteryHistory = view.findViewById(R.id.btnBatteryHistory);
    }

    private void setupRefreshButton() {
        btnRefreshBattery.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					updateBatteryInfo();
					animateRefreshButton();
				}
			});
    }

    private void setupAdditionalButtons() {
        btnBatteryOptimization.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showBatteryOptimizationTips();
				}
			});

        btnBatteryHistory.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showBatteryHistory();
				}
			});
    }

    private void animateRefreshButton() {
        btnRefreshBattery.animate()
            .rotationBy(360)
            .setDuration(500)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    private void registerBatteryReceiver() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateBatteryInfoFromIntent(intent);
                updateLastUpdatedTime();
            }
        };

        if (getActivity() != null) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            getActivity().registerReceiver(batteryReceiver, filter);
        }
    }

    private void startPeriodicUpdates() {
        periodicUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateBatteryInfo();
                updateHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        updateHandler.postDelayed(periodicUpdateRunnable, UPDATE_INTERVAL);
    }

    private void updateBatteryInfo() {
        if (getActivity() != null) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = getActivity().registerReceiver(null, filter);
            updateBatteryInfoFromIntent(batteryStatus);
            updateLastUpdatedTime();
        }
    }

    private void updateBatteryInfoFromIntent(Intent intent) {
        if (intent == null) return;

        // Battery Level with animation
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = (level / (float) scale) * 100;

        updateBatteryLevelUI(batteryPct);
        updateBatteryChargingStatus(intent);
        updateBatteryHealth(intent);
        updateBatteryTemperature(intent);
        updateBatteryTechnology(intent);
        updateBatteryVoltage(intent);
        updateBatteryCapacity(intent);
        updateChargeTimeRemaining(intent, batteryPct);
        updateEstimatedRemainingTime(intent, batteryPct);
        updateBatteryUsage(batteryPct);
    }

    private void updateBatteryLevelUI(float batteryPct) {
        if (tvBatteryLevel != null) {
            tvBatteryLevel.setText(String.format("%.1f%%", batteryPct));

            // Animate progress bar
            if (pbBatteryLevel != null) {
                ValueAnimator animator = ValueAnimator.ofInt(pbBatteryLevel.getProgress(), (int) batteryPct);
                animator.setDuration(800);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
						@Override
						public void onAnimationUpdate(ValueAnimator animation) {
							pbBatteryLevel.setProgress((int) animation.getAnimatedValue());
						}
					});
                animator.start();
            }

            // Color coding with theme colors
            if (batteryPct < 15) {
                tvBatteryLevel.setTextColor(getResources().getColor(R.color.error_red));
                tvBatteryStatus.setText("Critical - Charge Now");
                tvBatteryStatus.setTextColor(getResources().getColor(R.color.error_red));
            } else if (batteryPct < 30) {
                tvBatteryLevel.setTextColor(getResources().getColor(R.color.warning_orange));
                tvBatteryStatus.setText("Low - Consider Charging");
                tvBatteryStatus.setTextColor(getResources().getColor(R.color.warning_orange));
            } else if (batteryPct < 80) {
                tvBatteryLevel.setTextColor(getResources().getColor(R.color.primaryGreen));
                tvBatteryStatus.setText("Good");
                tvBatteryStatus.setTextColor(getResources().getColor(R.color.primaryGreen));
            } else {
                tvBatteryLevel.setTextColor(getResources().getColor(R.color.lime_green));
                tvBatteryStatus.setText("Excellent");
                tvBatteryStatus.setTextColor(getResources().getColor(R.color.lime_green));
            }
        }
    }

    private void updateBatteryChargingStatus(Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        String chargingStatus = "Unknown";

        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB) {
                    chargingStatus = "Charging (USB)";
                } else if (chargePlug == BatteryManager.BATTERY_PLUGGED_AC) {
                    chargingStatus = "Charging (AC)";
                } else if (chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                    chargingStatus = "Charging (Wireless)";
                } else {
                    chargingStatus = "Charging";
                }
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                chargingStatus = "Discharging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                chargingStatus = "Fully Charged";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                chargingStatus = "Not Charging";
                break;
        }

        if (tvChargingStatus != null) {
            tvChargingStatus.setText(chargingStatus);
            // Use theme colors for status
            if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                tvChargingStatus.setTextColor(getResources().getColor(R.color.lime_green));
            } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                tvChargingStatus.setTextColor(getResources().getColor(R.color.primaryGreen));
            } else {
                tvChargingStatus.setTextColor(getResources().getColor(R.color.textSecondary));
            }
        }
    }

    private void updateBatteryHealth(Intent intent) {
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        String healthText = "Unknown";

        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                healthText = "Good ✓";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                healthText = "Overheat ⚠";
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                healthText = "Dead ✗";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                healthText = "Over Voltage ⚠";
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                healthText = "Failure ✗";
                break;
            case BatteryManager.BATTERY_HEALTH_COLD:
                healthText = "Cold ⚠";
                break;
        }

        if (tvBatteryHealth != null) {
            tvBatteryHealth.setText(healthText);
            // Color health status
            if (health == BatteryManager.BATTERY_HEALTH_GOOD) {
                tvBatteryHealth.setTextColor(getResources().getColor(R.color.primaryGreen));
            } else if (health == BatteryManager.BATTERY_HEALTH_COLD) {
                tvBatteryHealth.setTextColor(getResources().getColor(R.color.light_blue));
            } else {
                tvBatteryHealth.setTextColor(getResources().getColor(R.color.error_red));
            }
        }
    }

    private void updateBatteryTemperature(Intent intent) {
        int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        if (temperature != -1 && tvBatteryTemperature != null) {
            float tempCelsius = temperature / 10.0f;
            float tempFahrenheit = (tempCelsius * 9/5) + 32;

            String tempText = String.format("%.1f°C / %.1f°F", tempCelsius, tempFahrenheit);

            // Color code temperature using theme
            if (tempCelsius > 40) {
                tvBatteryTemperature.setTextColor(getResources().getColor(R.color.error_red));
                tempText += " (Hot)";
            } else if (tempCelsius < 10) {
                tvBatteryTemperature.setTextColor(getResources().getColor(R.color.light_blue));
                tempText += " (Cold)";
            } else {
                tvBatteryTemperature.setTextColor(getResources().getColor(R.color.textSecondary));
                tempText += " (Normal)";
            }

            tvBatteryTemperature.setText(tempText);
        }
    }

    private void updateBatteryTechnology(Intent intent) {
        String technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        if (technology != null && tvBatteryTechnology != null) {
            tvBatteryTechnology.setText(technology);
            tvBatteryTechnology.setTextColor(getResources().getColor(R.color.textSecondary));
        }
    }

    private void updateBatteryVoltage(Intent intent) {
        int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        if (voltage != -1 && tvBatteryVoltage != null) {
            float voltageV = voltage / 1000.0f;
            tvBatteryVoltage.setText(String.format("%.3f V (%d mV)", voltageV, voltage));

            // Normal voltage range is 3.7V - 4.2V for lithium batteries
            if (voltageV < 3.5 || voltageV > 4.3) {
                tvBatteryVoltage.setTextColor(getResources().getColor(R.color.error_red));
            } else {
                tvBatteryVoltage.setTextColor(getResources().getColor(R.color.textSecondary));
            }
        }
    }

    private void updateBatteryCapacity(Intent intent) {
        // Note: This requires API level 21+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager bm = (BatteryManager) getActivity().getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                int capacity = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if (capacity > 0 && tvBatteryCapacity != null) {
                    tvBatteryCapacity.setText(String.format("%d%%", capacity));

                    // Color capacity based on health
                    if (capacity < 80) {
                        tvBatteryCapacity.setTextColor(getResources().getColor(R.color.error_red));
                    } else if (capacity < 90) {
                        tvBatteryCapacity.setTextColor(getResources().getColor(R.color.warning_orange));
                    } else {
                        tvBatteryCapacity.setTextColor(getResources().getColor(R.color.primaryGreen));
                    }
                }
            }
        } else {
            // For older APIs, show placeholder
            if (tvBatteryCapacity != null) {
                tvBatteryCapacity.setText("N/A");
                tvBatteryCapacity.setTextColor(getResources().getColor(R.color.textHint));
            }
        }
    }

    private void updateChargeTimeRemaining(Intent intent, float batteryPct) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            BatteryManager bm = (BatteryManager) getActivity().getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                long chargeTimeRemaining = bm.computeChargeTimeRemaining();
                if (chargeTimeRemaining > 0 && tvChargeTimeRemaining != null) {
                    String time = formatDuration(chargeTimeRemaining);
                    tvChargeTimeRemaining.setText(String.format("%s", time));
                    tvChargeTimeRemaining.setTextColor(getResources().getColor(R.color.primaryGreen));
                } else if (tvChargeTimeRemaining != null) {
                    tvChargeTimeRemaining.setText("Calculating...");
                    tvChargeTimeRemaining.setTextColor(getResources().getColor(R.color.textHint));
                }
            }
        } else {
            // For older APIs
            if (tvChargeTimeRemaining != null) {
                tvChargeTimeRemaining.setText("Feature requires Android 9+");
                tvChargeTimeRemaining.setTextColor(getResources().getColor(R.color.textHint));
            }
        }
    }

    private void updateEstimatedRemainingTime(Intent intent, float batteryPct) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        if (status == BatteryManager.BATTERY_STATUS_DISCHARGING && lastBatteryLevel > 0) {
            float dischargeRate = lastBatteryLevel - batteryPct;
            if (dischargeRate > 0.1) { // At least 0.1% drop for calculation
                float hoursRemaining = batteryPct / dischargeRate;
                long minutes = (long) (hoursRemaining * 60);

                if (tvEstimatedRemainingTime != null && minutes > 0) {
                    String time = formatDuration(minutes * 60000); // Convert to milliseconds
                    tvEstimatedRemainingTime.setText(String.format("%s", time));
                    tvEstimatedRemainingTime.setTextColor(getResources().getColor(R.color.primaryGreen));
                }
            }
        } else if (tvEstimatedRemainingTime != null) {
            tvEstimatedRemainingTime.setText("N/A");
            tvEstimatedRemainingTime.setTextColor(getResources().getColor(R.color.textHint));
        }
        lastBatteryLevel = batteryPct;
    }

    private void updateBatteryUsage(float currentBatteryPct) {
        if (tvBatteryUsageToday != null) {
            // Simple session-based tracking
            if (sessionStartBattery == 0) {
                sessionStartBattery = currentBatteryPct;
            }

            float usage = sessionStartBattery - currentBatteryPct;
            if (usage < 0) usage = 0; // If charging increased battery

            tvBatteryUsageToday.setText(String.format("%.1f%%", usage));
            tvBatteryUsageToday.setTextColor(getResources().getColor(R.color.textSecondary));
        }
    }

    private void updateLastUpdatedTime() {
        if (tvLastUpdated != null) {
            String currentTime = timeFormat.format(new Date());
            tvLastUpdated.setText(String.format("Updated: %s", currentTime));
            tvLastUpdated.setTextColor(getResources().getColor(R.color.textHint));
        }
    }

    private String formatDuration(long millis) {
        if (millis <= 0) return "N/A";

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;

        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    private void showBatteryOptimizationTips() {
        // Simple tips in a toast-like manner using the status field
        String[] tips = {
            "Tip: Lower screen brightness to save battery",
            "Tip: Close unused apps running in background",
            "Tip: Use dark mode to save power on OLED screens",
            "Tip: Turn off Wi-Fi/Bluetooth when not needed",
            "Tip: Enable battery saver mode when low"
        };

        int randomTip = (int) (Math.random() * tips.length);
        tvBatteryStatus.setText(tips[randomTip]);
        tvBatteryStatus.setTextColor(getResources().getColor(R.color.lime_green));

        // Reset after 5 seconds
        new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					if (getActivity() != null) {
						IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
						Intent batteryStatus = getActivity().registerReceiver(null, filter);
						if (batteryStatus != null) {
							updateBatteryInfoFromIntent(batteryStatus);
						}
					}
				}
			}, 5000);
    }

    private void showBatteryHistory() {
        // Placeholder for history feature
        tvBatteryStatus.setText("Battery History: View detailed stats in Settings");
        tvBatteryStatus.setTextColor(getResources().getColor(R.color.primaryGreen));

        new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					if (getActivity() != null) {
						IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
						Intent batteryStatus = getActivity().registerReceiver(null, filter);
						if (batteryStatus != null) {
							updateBatteryInfoFromIntent(batteryStatus);
						}
					}
				}
			}, 3000);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBatteryInfo();
        if (periodicUpdateRunnable != null) {
            updateHandler.post(periodicUpdateRunnable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (periodicUpdateRunnable != null) {
            updateHandler.removeCallbacks(periodicUpdateRunnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (batteryReceiver != null && getActivity() != null) {
            getActivity().unregisterReceiver(batteryReceiver);
        }
        if (periodicUpdateRunnable != null) {
            updateHandler.removeCallbacks(periodicUpdateRunnable);
        }
    }
}
