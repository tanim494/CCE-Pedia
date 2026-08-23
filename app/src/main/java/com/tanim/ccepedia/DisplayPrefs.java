package com.tanim.ccepedia;

import android.content.Context;
import android.content.SharedPreferences;

public class DisplayPrefs {

    private static final String PREFS_DISPLAY = "display_prefs";
    private static final String KEY_SHOW_DATE = "show_date";
    private static final String KEY_SHOW_SALAT = "show_salat";
    private static final String KEY_SHOW_ROUTINE = "show_routine";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS_DISPLAY, Context.MODE_PRIVATE);
    }

    public static boolean isShowDate(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SHOW_DATE, true);
    }

    public static boolean isShowSalat(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SHOW_SALAT, true);
    }

    public static boolean isShowRoutine(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SHOW_ROUTINE, true);
    }

    public static void setShowDate(Context ctx, boolean show) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_DATE, show).apply();
    }

    public static void setShowSalat(Context ctx, boolean show) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_SALAT, show).apply();
    }

    public static void setShowRoutine(Context ctx, boolean show) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_ROUTINE, show).apply();
    }
}