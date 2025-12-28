package com.cyberdeck.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private LinearLayout pinnedAppsContainer;
    private TextView batteryText, wifiText, timeText;
    private EditText commandInput;
    private List<String> pinnedPackages;
    private List<AppInfo> allApps;

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
        batteryText = findViewById(R.id.batteryText);
        wifiText = findViewById(R.id.wifiText);
        timeText = findViewById(R.id.timeText);
        commandInput = findViewById(R.id.commandInput);

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

        // Add "add app" button
        TextView addButton = new TextView(this);
        addButton.setText("[+] add app...");
        addButton.setTextSize(16);
        addButton.setTextColor(0xFF00FFFF); // Cyan
        addButton.setPadding(20, 10, 20, 10);
        addButton.setTypeface(android.graphics.Typeface.MONOSPACE);
        addButton.setOnClickListener(v -> showAppPicker());
        pinnedAppsContainer.addView(addButton);
    }

    private void updateSystemInfo() {
        // Update time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
        timeText.setText(sdf.format(new Date()));

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

        // Update every second
        timeText.postDelayed(this::updateSystemInfo, 1000);
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
                loadPinnedApps();
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

    private void showAppPicker() {
        // Simple implementation - show toast for now
        Toast.makeText(this, "App picker - coming soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSystemInfo();
    }
}
