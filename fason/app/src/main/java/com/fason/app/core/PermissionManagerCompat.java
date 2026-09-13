package com.fason.app.core;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/** Thin compat layer so AccessibilityGate stays decoupled from the permissions package. */
final class PermissionManagerCompat {
    private PermissionManagerCompat() {}

    static boolean hasAccessibility(Context ctx) {
        try {
            String enabled = Settings.Secure.getString(
                ctx.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabled == null || enabled.isEmpty()) return false;
            ComponentName svc = new ComponentName(ctx, FasonAccessibilityService.class);
            String flat = svc.flattenToString();
            for (String token : enabled.split(":")) {
                if (token.equals(flat)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    static List<String> denied(Context ctx) {
        List<String> out = new ArrayList<>();
        try {
            for (String p : com.fason.app.core.permissions.PermissionManager.getRequiredPerms()) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, p)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    out.add(p);
                }
            }
        } catch (Exception ignored) {}
        return out;
    }
}
