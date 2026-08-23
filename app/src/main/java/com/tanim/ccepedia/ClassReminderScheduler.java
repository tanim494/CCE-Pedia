package com.tanim.ccepedia;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

/**
 * Schedules a one-off reminder a fixed lead time before each class in the student's routine, so they
 * get a heads-up even when the app is closed. Reminders are gated on {@link DisplayPrefs#isShowRoutine}
 * — the same toggle that shows the Home routine card — so turning the routine off silences them too.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Uses inexact {@link AlarmManager#setAndAllowWhileIdle} (Doze-friendly, no
 *       {@code SCHEDULE_EXACT_ALARM} permission needed on Android 12+). A class reminder that lands a
 *       few minutes early/late is fine; exact alarms aren't worth the permission cost.</li>
 *   <li>Each class gets its own {@link PendingIntent} keyed by a compact request code. {@link #sync}
 *       cancels the previous set (tracked by count) and rebuilds from the cached routine, so it's
 *       idempotent — safe to call on every routine change, toggle flip, app launch, or reboot.</li>
 *   <li>Alarms are one-shot. When one fires, {@link ClassReminderReceiver} re-runs {@link #sync},
 *       which rolls the fired class forward to next week and keeps the rest scheduled.</li>
 * </ul>
 */
public final class ClassReminderScheduler {

    private ClassReminderScheduler() {
    }

    /** Fire the reminder this many minutes before the class start time. */
    public static final int LEAD_MINUTES = 5;

    static final String ACTION_FIRE = "com.tanim.ccepedia.CLASS_REMINDER";
    static final String EXTRA_COURSE = "course";
    static final String EXTRA_TIME = "time_label";
    static final String EXTRA_INDEX = "req_index";

    private static final String PREFS = "class_reminders";
    private static final String KEY_COUNT = "scheduled_count";
    private static final long MS_PER_MIN = 60_000L;

    /**
     * Cancels any previously scheduled reminders and, if the routine is enabled, schedules a fresh
     * one before each class. Idempotent — call it whenever the routine or its toggle changes.
     */
    public static void sync(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        cancelAll(app, am);

        // Routine hidden → no reminders. cancelAll already cleared the old set; record zero and stop.
        if (!DisplayPrefs.isShowRoutine(app)) {
            saveCount(app, 0);
            return;
        }

        List<RoutineEntry> entries = RoutineStore.loadCache(app);
        int scheduled = 0;
        for (RoutineEntry e : entries) {
            int startMin = RoutineStore.startMinutes(e.getStart());
            if (startMin < 0) continue; // skip unparseable times

            long triggerAt = nextTriggerMillis(e.getDay(), startMin, LEAD_MINUTES);
            PendingIntent pi = buildPendingIntent(app, scheduled,
                    e.getCourse(), RoutineStore.format12(e.getStart()),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            scheduled++;
        }
        saveCount(app, scheduled);
    }

    /** Cancels the whole previously scheduled set, tracked by the saved count. */
    private static void cancelAll(Context app, AlarmManager am) {
        int prev = prefs(app).getInt(KEY_COUNT, 0);
        for (int i = 0; i < prev; i++) {
            // Extras don't affect PendingIntent matching (action + component + request code do), so a
            // bare intent recovers the existing one for cancellation.
            PendingIntent pi = buildPendingIntent(app, i, null, null, PendingIntent.FLAG_NO_CREATE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }
    }

    /**
     * Next wall-clock instant, {@code lead} minutes before the given routine day/start, that is still
     * in the future — rolling to next week if this week's slot has already passed.
     */
    private static long nextTriggerMillis(int dayIndex, int startMinutes, int lead) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, startMinutes / 60);
        cal.set(Calendar.MINUTE, startMinutes % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int diff = (dayOfWeek(dayIndex) - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7;
        cal.add(Calendar.DAY_OF_YEAR, diff);

        long triggerAt = cal.getTimeInMillis() - lead * MS_PER_MIN;
        if (triggerAt <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 7);
            triggerAt = cal.getTimeInMillis() - lead * MS_PER_MIN;
        }
        return triggerAt;
    }

    /** RoutineStore day index (0=Sat…4=Wed) → {@link Calendar} day-of-week constant. */
    private static int dayOfWeek(int dayIndex) {
        switch (dayIndex) {
            case 0:  return Calendar.SATURDAY;
            case 1:  return Calendar.SUNDAY;
            case 2:  return Calendar.MONDAY;
            case 3:  return Calendar.TUESDAY;
            case 4:  return Calendar.WEDNESDAY;
            default: return Calendar.SATURDAY;
        }
    }

    private static PendingIntent buildPendingIntent(Context app, int requestCode,
                                                    String course, String timeLabel, int baseFlags) {
        Intent intent = new Intent(app, ClassReminderReceiver.class)
                .setAction(ACTION_FIRE)
                .putExtra(EXTRA_COURSE, course)
                .putExtra(EXTRA_TIME, timeLabel)
                .putExtra(EXTRA_INDEX, requestCode);

        int flags = baseFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(app, requestCode, intent, flags);
    }

    private static void saveCount(Context app, int count) {
        prefs(app).edit().putInt(KEY_COUNT, count).apply();
    }

    private static SharedPreferences prefs(Context app) {
        return app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
