package com.cyberdeck.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private LinearLayout pinnedAppsContainer, allAppsContainer;
    private TextView batteryText, wifiText, timeText, dateText, ramText, storageText, sdCardText, uptimeText, ipText, tempText, networkText;
    private EditText commandInput;
    private List<String> pinnedPackages;
    private List<AppInfo> allApps;
    private long startRxBytes = 0;
    private long startTxBytes = 0;

    private static class AppInfo {
        String name;
        String packageName;
        AppInfo(String name, String packageName) {
            this.name = name;
            this.packageName = packageName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        pinnedAppsContainer = findViewById(R.id.pinnedAppsContainer);
        allAppsContainer = findViewById(R.id.allAppsContainer);
        batteryText = findViewById(R.id.batteryText);
        wifiText = findViewById(R.id.wifiText);
        timeText = findViewById(R.id.timeText);
        dateText = findViewById(R.id.dateText);
        ramText = findViewById(R.id.ramText);
        storageText = findViewById(R.id.storageText);
        sdCardText = findViewById(R.id.sdCardText);
        uptimeText = findViewById(R.id.uptimeText);
        ipText = findViewById(R.id.ipText);
        tempText = findViewById(R.id.tempText);
        networkText = findViewById(R.id.networkText);
        commandInput = findViewById(R.id.commandInput);

        // Initialize network stats
        startRxBytes = TrafficStats.getTotalRxBytes();
        startTxBytes = TrafficStats.getTotalTxBytes();

        // Initialize pinned apps
        pinnedPackages = new ArrayList<>();
        pinnedPackages.add("com.spotify.music");
        pinnedPackages.add("org.connectbot");
        pinnedPackages.add("dev.imranr.obtainium.fdroid");
        pinnedPackages.add("com.termux");
        pinnedPackages.add("com.android.settings");

        // Load all installed apps
        loadAllApps();

        loadPinnedApps();
        loadAllAppsView();
        updateSystemInfo();
        setupCommandInput();
    }

    private void loadPinnedApps() {
        pinnedAppsContainer.removeAllViews();
        PackageManager pm = getPackageManager();

        for (String packageName : pinnedPackages) {
            try {
                String appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
                TextView appView = new TextView(this);
                appView.setText("[●] " + appName.toLowerCase());
                appView.setTextSize(16);
                appView.setTextColor(0xFF00FF00); // Green
                appView.setPadding(20, 10, 20, 10);
                appView.setTypeface(android.graphics.Typeface.MONOSPACE);

                appView.setOnClickListener(v -> launchApp(packageName));

                pinnedAppsContainer.addView(appView);
            } catch (Exception e) {
                // App not installed, skip
            }
        }

    }

    private void updateSystemInfo() {
        // Update time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
        timeText.setText(sdf.format(new Date()));

        // Update date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        dateText.setText("[" + dateFormat.format(new Date()) + "]");

        // Update battery
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int)((level / (float)scale) * 100);

            String bars = "";
            int numBars = batteryPct / 10;
            for (int i = 0; i < 10; i++) {
                bars += i < numBars ? "█" : "░";
            }
            batteryText.setText("Battery: [" + batteryPct + "%] " + bars);
        }

        // Update WiFi
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            wifiText.setText("WiFi: [Connected]");
        } else {
            wifiText.setText("WiFi: [Disconnected]");
        }

        // Update RAM
        updateRAM();

        // Update Storage
        updateStorage();

        // Update SD Card
        updateSDCard();

        // Update Uptime
        updateUptime();

        // Update IP Address
        updateIPAddress(wifiManager);

        // Update CPU Temperature
        updateTemperature();

        // Update Network Stats
        updateNetworkStats();

        // Update every second
        timeText.postDelayed(this::updateSystemInfo, 1000);
    }

    private void updateRAM() {
        try {
            android.app.ActivityManager activityManager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo memoryInfo = new android.app.ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);

            long totalMem = memoryInfo.totalMem / (1024 * 1024); // Convert to MB
            long availMem = memoryInfo.availMem / (1024 * 1024); // Convert to MB
            long usedMem = totalMem - availMem;

            int usedPct = (int)((usedMem / (float)totalMem) * 100);
            String bars = "";
            int numBars = usedPct / 10;
            for (int i = 0; i < 10; i++) {
                bars += i < numBars ? "█" : "░";
            }

            ramText.setText("RAM: [" + usedMem + "MB/" + totalMem + "MB] " + bars);
        } catch (Exception e) {
            ramText.setText("RAM: [N/A]");
        }
    }

    private void updateStorage() {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long availBytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            long totalBytes = stat.getBlockCountLong() * stat.getBlockSizeLong();

            float availGB = availBytes / (1024.0f * 1024.0f * 1024.0f);
            float totalGB = totalBytes / (1024.0f * 1024.0f * 1024.0f);

            storageText.setText(String.format(Locale.US, "Storage: [%.1fGB/%.1fGB free]", availGB, totalGB));
        } catch (Exception e) {
            storageText.setText("Storage: [N/A]");
        }
    }

    private void updateSDCard() {
        try {
            // Check for removable storage (SD card)
            File[] externalDirs = getExternalFilesDirs(null);

            if (externalDirs != null && externalDirs.length > 1 && externalDirs[1] != null) {
                // SD card is at index 1 (index 0 is internal storage)
                String sdPath = externalDirs[1].getAbsolutePath();
                // Go up to the root of the SD card
                while (!sdPath.endsWith("/Android")) {
                    sdPath = new File(sdPath).getParent();
                }
                sdPath = new File(sdPath).getParent();

                StatFs stat = new StatFs(sdPath);
                long availBytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
                long totalBytes = stat.getBlockCountLong() * stat.getBlockSizeLong();

                float availGB = availBytes / (1024.0f * 1024.0f * 1024.0f);
                float totalGB = totalBytes / (1024.0f * 1024.0f * 1024.0f);

                sdCardText.setText(String.format(Locale.US, "SD Card: [%.1fGB/%.1fGB free]", availGB, totalGB));
            } else {
                sdCardText.setText("SD Card: [Not detected]");
            }
        } catch (Exception e) {
            sdCardText.setText("SD Card: [Not detected]");
        }
    }

    private void updateUptime() {
        try {
            long uptimeMillis = SystemClock.elapsedRealtime();
            long days = uptimeMillis / (1000 * 60 * 60 * 24);
            long hours = (uptimeMillis / (1000 * 60 * 60)) % 24;
            long minutes = (uptimeMillis / (1000 * 60)) % 60;

            uptimeText.setText(String.format(Locale.US, "Uptime: [%dd %dh %dm]", days, hours, minutes));
        } catch (Exception e) {
            uptimeText.setText("Uptime: [N/A]");
        }
    }

    private void updateIPAddress(WifiManager wifiManager) {
        try {
            if (wifiManager.isWifiEnabled()) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ip = wifiInfo.getIpAddress();

                String ipString = String.format(Locale.US, "%d.%d.%d.%d",
                    (ip & 0xff),
                    (ip >> 8 & 0xff),
                    (ip >> 16 & 0xff),
                    (ip >> 24 & 0xff));

                ipText.setText("IP: [" + ipString + "]");
            } else {
                ipText.setText("IP: [No WiFi]");
            }
        } catch (Exception e) {
            ipText.setText("IP: [N/A]");
        }
    }

    private void updateTemperature() {
        try {
            // Try to read CPU temperature from thermal zone
            File tempFile = new File("/sys/class/thermal/thermal_zone0/temp");
            if (tempFile.exists()) {
                RandomAccessFile reader = new RandomAccessFile(tempFile, "r");
                String tempStr = reader.readLine();
                reader.close();

                int temp = Integer.parseInt(tempStr) / 1000; // Convert to Celsius
                tempText.setText("Temp: [" + temp + "°C]");
            } else {
                tempText.setText("Temp: [--°C]");
            }
        } catch (Exception e) {
            tempText.setText("Temp: [--°C]");
        }
    }

    private void updateNetworkStats() {
        try {
            long currentRx = TrafficStats.getTotalRxBytes();
            long currentTx = TrafficStats.getTotalTxBytes();

            long rxBytes = currentRx - startRxBytes;
            long txBytes = currentTx - startTxBytes;

            float rxMB = rxBytes / (1024.0f * 1024.0f);
            float txMB = txBytes / (1024.0f * 1024.0f);

            networkText.setText(String.format(Locale.US, "Network: [TX: %.1fMB RX: %.1fMB]", txMB, rxMB));
        } catch (Exception e) {
            networkText.setText("Network: [N/A]");
        }
    }

    private void setupCommandInput() {
        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            String cmd = commandInput.getText().toString().trim();
            if (!cmd.isEmpty()) {
                executeCommand(cmd);
                commandInput.setText("");
            }
            return true;
        });
    }

    private void loadAllApps() {
        allApps = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo app : apps) {
            String name = app.loadLabel(pm).toString();
            String pkg = app.activityInfo.packageName;
            allApps.add(new AppInfo(name, pkg));
        }

        // Sort alphabetically
        java.util.Collections.sort(allApps, (a, b) -> a.name.compareToIgnoreCase(b.name));
    }

    private void loadAllAppsView() {
        allAppsContainer.removeAllViews();

        for (AppInfo app : allApps) {
            TextView appView = new TextView(this);
            appView.setText("[*] " + app.name.toLowerCase());
            appView.setTextSize(14);
            appView.setTextColor(0xFF00FF00); // Green
            appView.setPadding(20, 8, 20, 8);
            appView.setTypeface(android.graphics.Typeface.MONOSPACE);

            appView.setOnClickListener(v -> launchApp(app.packageName));

            allAppsContainer.addView(appView);
        }
    }

    private void executeCommand(String cmd) {
        String[] parts = cmd.trim().split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "help":
            case "?":
                showHelp();
                break;
            case "open":
            case "launch":
                if (parts.length > 1) {
                    String appName = cmd.substring(command.length()).trim().toLowerCase();
                    openApp(appName);
                } else {
                    Toast.makeText(this, "Usage: open <app>", Toast.LENGTH_SHORT).show();
                }
                break;
            case "list":
            case "ls":
                listApps();
                break;
            case "reset":
            case "clear":
                loadAllApps();
                loadPinnedApps();
                loadAllAppsView();
                Toast.makeText(this, "Launcher reset", Toast.LENGTH_SHORT).show();
                break;
            case "exit":
            case "quit":
                moveTaskToBack(true);
                break;
            case "time":
                Toast.makeText(this, new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()), Toast.LENGTH_SHORT).show();
                break;
            case "battery":
            case "bat":
                Toast.makeText(this, batteryText.getText(), Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(this, "Unknown command: " + command + "\nType 'help' for commands", Toast.LENGTH_SHORT).show();
        }
    }

    private void showHelp() {
        String helpText = "Available commands:\n\n" +
            "help, ?          - Show this help\n" +
            "open <app>       - Launch an app\n" +
            "list, ls         - List all apps\n" +
            "reset, clear     - Reset launcher\n" +
            "exit, quit       - Exit launcher\n" +
            "time             - Show current time\n" +
            "battery, bat     - Show battery info";

        Toast.makeText(this, helpText, Toast.LENGTH_LONG).show();
    }

    private void openApp(String query) {
        // Try to find matching app
        for (AppInfo app : allApps) {
            if (app.name.toLowerCase().contains(query)) {
                launchApp(app.packageName);
                return;
            }
        }
        Toast.makeText(this, "App not found: " + query, Toast.LENGTH_SHORT).show();
    }

    private void listApps() {
        StringBuilder sb = new StringBuilder("Installed apps:\n\n");
        for (int i = 0; i < Math.min(allApps.size(), 10); i++) {
            sb.append("• ").append(allApps.get(i).name).append("\n");
        }
        if (allApps.size() > 10) {
            sb.append("\n... and ").append(allApps.size() - 10).append(" more");
        }
        Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
    }

    private void launchApp(String packageName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to launch app", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSystemInfo();
    }
}
