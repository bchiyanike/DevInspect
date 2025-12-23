// com/lionico/myapp/MainActivity.java

package com.lionico.devinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;
import android.graphics.Typeface;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity 
{
    private TabHost tabHost;
    private float startX;
    private float startY;
    private long lastTabTapTime = 0;
    private int lastTabIndex = -1;
    private boolean isTabInitialized = false;
    private Map<String, Fragment> fragmentCache = new HashMap<>();
    private Handler animationHandler = new Handler();

    // Animation IDs
    private static final int ANIM_SLIDE_IN_LEFT = 1;
    private static final int ANIM_SLIDE_IN_RIGHT = 2;
    private static final int ANIM_SLIDE_OUT_LEFT = 3;
    private static final int ANIM_SLIDE_OUT_RIGHT = 4;

    // Tab constants
    private static class TabConstants {
        static final String TAB_DEVICE_INFO = "DeviceInfo";
        static final String TAB_BATTERY = "Battery";
        static final String TAB_FILE_EXPLORER = "FileExplorer";
        static final String TAB_QUICK_TOOLS = "QuickTools";

        static final int TAB_COUNT = 4;
        static final int SWIPE_THRESHOLD_DP = 100;
        static final int DOUBLE_TAP_THRESHOLD_MS = 500;
        static final int VERTICAL_THRESHOLD_DP = 50;
    }

    // Preference keys
    private static final String PREF_NAME = "MyAppPrefs";
    private static final String KEY_LAST_TAB = "last_tab_index";
    private static final String KEY_BADGE_COUNTS = "badge_counts_";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        setupTabs();

        // Restore last tab from preferences
        restoreLastTab();

        // Setup swipe detection on the content area
        setupSwipeDetection();
    }

    private void setupTabs()
    {
        if (isTabInitialized) {
            return; // Already initialized
        }

        try {
            // Get TabHost from layout
            tabHost = (TabHost) findViewById(android.R.id.tabhost);
            if (tabHost == null) {
                throw new IllegalStateException("TabHost not found in layout");
            }

            // Initialize TabHost
            tabHost.setup();

            // Create and add tabs
            createTabs();

            // Apply custom styling to all tabs
            styleTabs();

            // Set tab change listener
            setupTabListener();

            // Load initial fragment based on current tab
            loadFragmentForCurrentTab();

            isTabInitialized = true;

        } catch (Exception e) {
            showErrorDialog("Failed to setup tabs: " + e.getMessage());
        }
    }

    private void createTabs()
    {
        // Tab configuration arrays
        String[] tabIds = {
            TabConstants.TAB_DEVICE_INFO,
            TabConstants.TAB_BATTERY,
            TabConstants.TAB_FILE_EXPLORER,
            TabConstants.TAB_QUICK_TOOLS
        };

        String[] tabTitles = {
            getString(R.string.tab_device),
            getString(R.string.tab_battery),
            getString(R.string.tab_files),
            getString(R.string.tab_tools)
        };

        int[] tabContentIds = {
            R.id.tabDeviceInfo,
            R.id.tabBattery,
            R.id.tabFileExplorer,
            R.id.tabQuickTools
        };

        // Create each tab
        for (int i = 0; i < TabConstants.TAB_COUNT; i++) {
            TabHost.TabSpec tabSpec = tabHost.newTabSpec(tabIds[i]);
            tabSpec.setIndicator(createTabIndicator(tabTitles[i]));
            tabSpec.setContent(tabContentIds[i]);
            tabHost.addTab(tabSpec);

            // Restore badge counts
            restoreTabBadge(i);
        }

        // Set default tab (will be overridden by saved state if exists)
        tabHost.setCurrentTab(0);
    }

    private TextView createTabIndicator(String text)
    {
        // Create a TextView for the tab indicator with minimal spacing
        TextView tabView = new TextView(this);
        tabView.setText(text);
        tabView.setGravity(Gravity.CENTER);
        tabView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tabView.setTypeface(null, Typeface.BOLD);

        // Minimal padding
        int horizontalPadding = dpToPx(4);
        int verticalPadding = dpToPx(2);
        tabView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        // Set background selector
        tabView.setBackgroundResource(R.drawable.tab_selector);

        // Size constraints
        tabView.setMinimumWidth(dpToPx(60));
        tabView.setMaxWidth(dpToPx(100));

        return tabView;
    }

    private void styleTabs()
    {
        if (tabHost == null) return;

        TabWidget tabWidget = tabHost.getTabWidget();
        if (tabWidget == null) return;

        // Style each tab
        for (int i = 0; i < tabWidget.getChildCount(); i++)
        {
            TextView tabView = getTabTextView(i);
            if (tabView != null)
            {
                // Set text colors based on selection
                int textColor = (i == tabHost.getCurrentTab()) 
                    ? getResources().getColor(R.color.tab_text_selected)
                    : getResources().getColor(R.color.tab_text_unselected);

                tabView.setTextColor(textColor);
                tabView.setBackgroundResource(R.drawable.tab_selector);

                // Ensure size constraints
                tabView.setMinimumWidth(dpToPx(60));
                tabView.setMaxWidth(dpToPx(100));

                // Apply visual feedback for selected tab (scale effect)
                if (i == tabHost.getCurrentTab()) {
                    applyTabSelectionAnimation(tabView);
                } else {
                    resetTabVisual(tabView);
                }
            }
        }

        // Set overall tab widget background
        tabWidget.setBackgroundColor(getResources().getColor(R.color.primaryGreenDark));

        // Remove dividers between tabs for cleaner look
        tabWidget.setDividerDrawable(null);
    }

    private void applyTabSelectionAnimation(final TextView tabView) {
        // Use a scale animation for selected tab
        Animation scaleAnimation = new ScaleAnimation(
            1.0f, 1.1f, // X scale from->to
            1.0f, 1.1f, // Y scale from->to
            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot X
            Animation.RELATIVE_TO_SELF, 0.5f  // Pivot Y
        );
        scaleAnimation.setDuration(200);
        scaleAnimation.setFillAfter(true); // Keep the scaled state
        tabView.startAnimation(scaleAnimation);
    }

    private void resetTabVisual(TextView tabView) {
        // Clear any existing animation and reset scale
        tabView.clearAnimation();

        // Reset to original scale
        Animation resetAnimation = new ScaleAnimation(
            tabView.getScaleX(), 1.0f,
            tabView.getScaleY(), 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        resetAnimation.setDuration(100);
        resetAnimation.setFillAfter(true);
        tabView.startAnimation(resetAnimation);
    }

    private TextView getTabTextView(int tabIndex) {
        if (tabHost == null || tabIndex < 0) {
            return null;
        }

        TabWidget tabWidget = tabHost.getTabWidget();
        if (tabWidget == null || tabIndex >= tabWidget.getChildCount()) {
            return null;
        }

        View tabView = tabWidget.getChildAt(tabIndex);
        if (tabView instanceof TextView) {
            return (TextView) tabView;
        } else if (tabView != null) {
            // Try to find TextView inside the tab view
            View textView = tabView.findViewById(android.R.id.title);
            if (textView instanceof TextView) {
                return (TextView) textView;
            }
        }
        return null;
    }

    private void setupTabListener() {
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
				@Override
				public void onTabChanged(String tabId) {
					// Handle double-tap detection
					handleDoubleTap();

					// Refresh tab styling with animation
					styleTabs();

					// Update content visibility
					updateContentVisibility(tabId);

					// Load appropriate fragment
					loadFragmentForTab(tabId);

					// Save current tab
					saveCurrentTab();
				}
			});
    }

    private void updateContentVisibility(String currentTabId) {
        // Show only the current tab's content, hide others
        FrameLayout[] contentFrames = {
            (FrameLayout) findViewById(R.id.tabDeviceInfo),
            (FrameLayout) findViewById(R.id.tabBattery),
            (FrameLayout) findViewById(R.id.tabFileExplorer),
            (FrameLayout) findViewById(R.id.tabQuickTools)
        };

        String[] tabIds = {
            TabConstants.TAB_DEVICE_INFO,
            TabConstants.TAB_BATTERY,
            TabConstants.TAB_FILE_EXPLORER,
            TabConstants.TAB_QUICK_TOOLS
        };

        for (int i = 0; i < contentFrames.length; i++) {
            if (contentFrames[i] != null) {
                if (tabIds[i].equals(currentTabId)) {
                    contentFrames[i].setVisibility(View.VISIBLE);
                    // Add a simple fade-in animation
                    Animation fadeIn = android.view.animation.AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_in);
                    contentFrames[i].startAnimation(fadeIn);
                } else {
                    contentFrames[i].setVisibility(View.GONE);
                }
            }
        }
    }

    private void handleDoubleTap() {
        int currentTab = tabHost.getCurrentTab();
        long currentTime = System.currentTimeMillis();

        if (lastTabIndex == currentTab && 
            (currentTime - lastTabTapTime) < TabConstants.DOUBLE_TAP_THRESHOLD_MS) {
            // Double-tap detected - refresh current tab
            refreshCurrentTab();
        }

        lastTabIndex = currentTab;
        lastTabTapTime = currentTime;
    }

    private void refreshCurrentTab() {
        String currentTabId = tabHost.getCurrentTabTag();
        if (currentTabId != null) {
            showToast("Refreshing " + getTabDisplayName(currentTabId));
            loadFragmentForTab(currentTabId);

            // Add a visual feedback for refresh
            TextView currentTabView = getTabTextView(tabHost.getCurrentTab());
            if (currentTabView != null) {
                Animation refreshAnimation = android.view.animation.AnimationUtils.loadAnimation(
                    this, android.R.anim.fade_in);
                currentTabView.startAnimation(refreshAnimation);
            }
        }
    }

    private String getTabDisplayName(String tabId) {
        if (TabConstants.TAB_DEVICE_INFO.equals(tabId)) {
            return getString(R.string.tab_device);
        } else if (TabConstants.TAB_BATTERY.equals(tabId)) {
            return getString(R.string.tab_battery);
        } else if (TabConstants.TAB_FILE_EXPLORER.equals(tabId)) {
            return getString(R.string.tab_files);
        } else if (TabConstants.TAB_QUICK_TOOLS.equals(tabId)) {
            return getString(R.string.tab_tools);
        }
        return tabId;
    }

    private void loadFragmentForTab(String tabId) {
        if (TabConstants.TAB_DEVICE_INFO.equals(tabId)) {
            loadDeviceInfoFragment();
        } else if (TabConstants.TAB_BATTERY.equals(tabId)) {
            loadBatteryFragment();
        } else if (TabConstants.TAB_FILE_EXPLORER.equals(tabId)) {
            loadFileExplorerFragment();
        } else if (TabConstants.TAB_QUICK_TOOLS.equals(tabId)) {
            loadQuickToolsFragment();
        }
    }

    private void loadFragmentForCurrentTab() {
        String currentTabId = tabHost.getCurrentTabTag();
        if (currentTabId != null) {
            loadFragmentForTab(currentTabId);
            updateContentVisibility(currentTabId);
        }
    }

    private void loadDeviceInfoFragment() {
        loadFragment(TabConstants.TAB_DEVICE_INFO, R.id.tabDeviceInfo, 
                     DeviceInfoFragment.class);
    }

    private void loadBatteryFragment() {
        loadFragment(TabConstants.TAB_BATTERY, R.id.tabBattery, 
                     BatteryFragment.class);
    }

    private void loadFileExplorerFragment() {
        loadFragment(TabConstants.TAB_FILE_EXPLORER, R.id.tabFileExplorer, 
                     FileExplorerFragment.class);
    }

    private void loadQuickToolsFragment() {
        loadFragment(TabConstants.TAB_QUICK_TOOLS, R.id.tabQuickTools, 
                     QuickToolsFragment.class);
    }

    private void loadFragment(String tag, int containerId, Class<? extends Fragment> fragmentClass) {
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fragmentCache.get(tag);

        if (fragment == null) {
            try {
                fragment = fragmentClass.newInstance();
                fragmentCache.put(tag, fragment);
            } catch (Exception e) {
                showErrorDialog("Failed to create fragment: " + e.getMessage());
                return;
            }
        }

        // Check if fragment is already added
        Fragment existingFragment = fm.findFragmentByTag(tag);
        if (existingFragment == null || !existingFragment.isAdded()) {
            fm.beginTransaction()
				.replace(containerId, fragment, tag)
				.commit();
        }
    }

    // Setup swipe detection on the content area
    private void setupSwipeDetection() {
        final FrameLayout contentArea = (FrameLayout) findViewById(android.R.id.tabcontent);
        if (contentArea != null) {
            contentArea.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						return handleSwipeGesture(event);
					}
				});
        }

        // Also setup swipe on tab widget
        final TabWidget tabWidget = (TabWidget) findViewById(android.R.id.tabs);
        if (tabWidget != null) {
            tabWidget.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						return handleSwipeGesture(event);
					}
				});
        }
    }

    private boolean handleSwipeGesture(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
                float endX = event.getX();
                float endY = event.getY();
                float deltaX = endX - startX;
                float deltaY = endY - startY;

                // Check if it's primarily a horizontal swipe (not vertical)
                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    // Minimum horizontal swipe distance
                    int swipeThreshold = dpToPx(TabConstants.SWIPE_THRESHOLD_DP);
                    int verticalThreshold = dpToPx(TabConstants.VERTICAL_THRESHOLD_DP);

                    // Only process if horizontal movement is significant and vertical is minimal
                    if (Math.abs(deltaX) > swipeThreshold && Math.abs(deltaY) < verticalThreshold) {
                        if (deltaX > 0) {
                            // Swipe right - previous tab
                            switchToTabWithAnimation(tabHost.getCurrentTab() - 1, true);
                        } else {
                            // Swipe left - next tab
                            switchToTabWithAnimation(tabHost.getCurrentTab() + 1, false);
                        }
                        return true; // Consume the event
                    }
                }
                break;
        }
        return false;
    }

    private void switchToTabWithAnimation(final int tabIndex, final boolean swipeRight) {
        if (isValidTabIndex(tabIndex)) {
            // Get references to tabs
            final TextView currentTab = getTabTextView(tabHost.getCurrentTab());
            final TextView targetTab = getTabTextView(tabIndex);

            if (currentTab != null) {
                // Create slide out animation
                Animation slideOut = createSlideAnimation(
                    swipeRight ? ANIM_SLIDE_OUT_RIGHT : ANIM_SLIDE_OUT_LEFT);
                slideOut.setDuration(150);
                currentTab.startAnimation(slideOut);
            }

            // Switch tab after a short delay
            animationHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						tabHost.setCurrentTab(tabIndex);

						// Animate the new tab coming in
						if (targetTab != null) {
							Animation slideIn = createSlideAnimation(
								swipeRight ? ANIM_SLIDE_IN_LEFT : ANIM_SLIDE_IN_RIGHT);
							slideIn.setDuration(150);
							targetTab.startAnimation(slideIn);
						}
					}
				}, 100);
        } else {
            // Show visual feedback that can't swipe further
            TextView currentTab = getTabTextView(tabHost.getCurrentTab());
            if (currentTab != null) {
                Animation shake = android.view.animation.AnimationUtils.loadAnimation(
                    this, android.R.anim.fade_in);
                shake.setDuration(100);
                currentTab.startAnimation(shake);

                if (tabIndex < 0) {
                    showToast("First tab");
                } else {
                    showToast("Last tab");
                }
            }
        }
    }

    private Animation createSlideAnimation(int animationType) {
        switch (animationType) {
            case ANIM_SLIDE_IN_LEFT:
                return new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f
                );
            case ANIM_SLIDE_IN_RIGHT:
                return new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, -1.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f
                );
            case ANIM_SLIDE_OUT_LEFT:
                return new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, -1.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f
                );
            case ANIM_SLIDE_OUT_RIGHT:
                return new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f
                );
            default:
                return android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        }
    }

    // Badge management methods
    public void setTabBadge(int tabIndex, int count) {
        if (!isValidTabIndex(tabIndex)) return;

        TextView tabView = getTabTextView(tabIndex);
        if (tabView != null) {
            String originalText = getOriginalTabText(tabIndex);
            String displayText = originalText;

            if (count > 0) {
                displayText = originalText + " (" + count + ")";
            }

            tabView.setText(displayText);
            styleTabs(); // Re-apply styling

            // Save badge count
            saveTabBadge(tabIndex, count);
        }
    }

    public void incrementTabBadge(int tabIndex) {
        if (!isValidTabIndex(tabIndex)) return;

        int currentCount = getTabBadgeCount(tabIndex);
        setTabBadge(tabIndex, currentCount + 1);
    }

    public void decrementTabBadge(int tabIndex) {
        if (!isValidTabIndex(tabIndex)) return;

        int currentCount = getTabBadgeCount(tabIndex);
        if (currentCount > 0) {
            setTabBadge(tabIndex, currentCount - 1);
        }
    }

    public void clearTabBadge(int tabIndex) {
        setTabBadge(tabIndex, 0);
    }

    private String getOriginalTabText(int tabIndex) {
        String[] tabTitles = {
            getString(R.string.tab_device),
            getString(R.string.tab_battery),
            getString(R.string.tab_files),
            getString(R.string.tab_tools)
        };

        if (tabIndex >= 0 && tabIndex < tabTitles.length) {
            return tabTitles[tabIndex];
        }
        return "";
    }

    private int getTabBadgeCount(int tabIndex) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_BADGE_COUNTS + tabIndex, 0);
    }

    private void saveTabBadge(int tabIndex, int count) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putInt(KEY_BADGE_COUNTS + tabIndex, count).apply();
    }

    private void restoreTabBadge(int tabIndex) {
        int count = getTabBadgeCount(tabIndex);
        if (count > 0) {
            setTabBadge(tabIndex, count);
        }
    }

    // State persistence methods
    private void saveCurrentTab() {
        if (tabHost != null) {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            prefs.edit().putInt(KEY_LAST_TAB, tabHost.getCurrentTab()).apply();
        }
    }

    private void restoreLastTab() {
        if (tabHost != null) {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            int lastTab = prefs.getInt(KEY_LAST_TAB, 0);

            if (isValidTabIndex(lastTab)) {
                tabHost.setCurrentTab(lastTab);
                loadFragmentForCurrentTab();
            }
        }
    }

    private void switchToTab(int tabIndex) {
        if (isValidTabIndex(tabIndex)) {
            tabHost.setCurrentTab(tabIndex);
        }
    }

    private boolean isValidTabIndex(int index) {
        return tabHost != null && index >= 0 && index < TabConstants.TAB_COUNT;
    }

    // Utility methods
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    // Lifecycle methods
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (tabHost != null) {
            outState.putInt(KEY_LAST_TAB, tabHost.getCurrentTab());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && tabHost != null) {
            int lastTab = savedInstanceState.getInt(KEY_LAST_TAB, 0);
            if (isValidTabIndex(lastTab)) {
                tabHost.setCurrentTab(lastTab);
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (tabHost != null)
        {
            styleTabs();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentTab();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        fragmentCache.clear();
        animationHandler.removeCallbacksAndMessages(null);
        tabHost = null;
        isTabInitialized = false;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        fragmentCache.clear();
    }

    // Public API for fragments
    public String getCurrentTabName() {
        if (tabHost != null) {
            return tabHost.getCurrentTabTag();
        }
        return null;
    }

    public int getCurrentTabIndex() {
        if (tabHost != null) {
            return tabHost.getCurrentTab();
        }
        return -1;
    }
}
