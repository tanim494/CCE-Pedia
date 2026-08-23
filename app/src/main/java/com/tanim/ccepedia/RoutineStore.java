package com.tanim.ccepedia;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static helpers shared by the routine editor and the Home "Today's Classes" card, so both agree on
 * one storage format, one day model, and one time format.
 *
 * <p>The routine lives as an array of maps on {@code users/{uid}.routine} (server-side, so it
 * survives logout/reinstall). A copy is mirrored into per-app {@link SharedPreferences} as JSON so
 * the Home card paints instantly and offline — the same render-from-cache-then-refresh pattern the
 * prayer header uses. minSdk 24 with no desugaring, so all date math is {@link Calendar}-based.
 */
public final class RoutineStore {

    private RoutineStore() {
    }

    // ---- Storage keys ----
    public static final String PREFS = "class_routine";
    private static final String KEY_JSON = "routine_json";
    private static final String KEY_VISIBLE = "routine_visible";

    // ---- Day model: IIUC week runs Sat–Wed; Thu & Fri are off. ----
    public static final String[] DAY_NAMES = {"Saturday", "Sunday", "Monday", "Tuesday", "Wednesday"};

    /** Today's routine-day index (0=Sat … 4=Wed), or -1 on Thu/Fri (no classes). */
    public static int todayIndex() {
        switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SATURDAY:  return 0;
            case Calendar.SUNDAY:    return 1;
            case Calendar.MONDAY:    return 2;
            case Calendar.TUESDAY:   return 3;
            case Calendar.WEDNESDAY: return 4;
            default:                 return -1; // Thursday / Friday
        }
    }

    /** Safe day-name lookup; returns "" for an out-of-range index. */
    public static String dayName(int day) {
        return (day >= 0 && day < DAY_NAMES.length) ? DAY_NAMES[day] : "";
    }

    /** Minutes since midnight for now, for "is this class still upcoming" checks. */
    public static int nowMinutes() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
    }

    /** Seconds since midnight for now — drives the Home card's live countdown. */
    public static int nowSeconds() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND);
    }

    // ---- Show/hide toggle (the user's "button to show or hide the routine") ----
    public static boolean isVisible(Context ctx) {
        return prefs(ctx).getBoolean(KEY_VISIBLE, true); // shown by default
    }

    public static void setVisible(Context ctx, boolean visible) {
        prefs(ctx).edit().putBoolean(KEY_VISIBLE, visible).apply();
    }

    // ---- Local cache (JSON in SharedPreferences) ----
    public static List<RoutineEntry> loadCache(Context ctx) {
        List<RoutineEntry> out = new ArrayList<>();
        String json = prefs(ctx).getString(KEY_JSON, null);
        if (json == null) return out;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new RoutineEntry(
                        o.optInt("day", 0),
                        o.optString("start", ""),
                        o.optString("end", ""),
                        o.optString("course", "")));
            }
        } catch (JSONException e) {
            return new ArrayList<>(); // corrupt cache → treat as empty
        }
        sort(out);
        return out;
    }

    public static void saveCache(Context ctx, List<RoutineEntry> entries) {
        JSONArray arr = new JSONArray();
        if (entries != null) {
            for (RoutineEntry e : entries) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("day", e.getDay());
                    o.put("start", e.getStart() == null ? "" : e.getStart());
                    o.put("end", e.getEnd() == null ? "" : e.getEnd());
                    o.put("course", e.getCourse() == null ? "" : e.getCourse());
                    arr.put(o);
                } catch (JSONException ignored) {
                    // Skip any single malformed entry rather than losing the whole cache.
                }
            }
        }
        prefs(ctx).edit().putString(KEY_JSON, arr.toString()).apply();
    }

    // ---- Firestore mapping ----

    /**
     * Reads the {@code routine} field from a user document into entries. Firestore hands array
     * fields back as {@code List<Map<String,Object>>} with numbers boxed as {@link Long}, so this
     * is defensive about types; a missing field (older accounts) yields an empty list.
     */
    public static List<RoutineEntry> fromFirestore(Object raw) {
        List<RoutineEntry> out = new ArrayList<>();
        if (!(raw instanceof List)) return out;
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) item;
            Object dayObj = m.get("day");
            int day = (dayObj instanceof Number) ? ((Number) dayObj).intValue() : 0;
            String start = m.get("start") != null ? String.valueOf(m.get("start")) : "";
            String end = m.get("end") != null ? String.valueOf(m.get("end")) : "";
            String course = m.get("course") != null ? String.valueOf(m.get("course")) : "";
            out.add(new RoutineEntry(day, start, end, course));
        }
        sort(out);
        return out;
    }

    /** Builds the plain {@code List<Map>} written back to {@code users/{uid}.routine}. */
    public static List<Map<String, Object>> toFirestore(List<RoutineEntry> entries) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (entries == null) return out;
        for (RoutineEntry e : entries) {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("day", e.getDay());
            m.put("start", e.getStart() == null ? "" : e.getStart());
            m.put("end", e.getEnd() == null ? "" : e.getEnd());
            m.put("course", e.getCourse() == null ? "" : e.getCourse());
            out.add(m);
        }
        return out;
    }

    // ---- Time helpers (same minute/12h logic proven in the prayer header) ----

    /** "HH:mm" → minutes since midnight, or -1 if unparseable (used for sorting/highlighting). */
    public static int startMinutes(String hhmm) {
        if (hhmm == null) return -1;
        String[] p = hhmm.trim().split(":");
        if (p.length < 2) return -1;
        try {
            return Integer.parseInt(p[0].trim()) * 60 + Integer.parseInt(p[1].trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** "HH:mm" → "h:mm AM/PM", or the raw input if it can't be parsed. */
    public static String format12(String hhmm) {
        int m = startMinutes(hhmm);
        if (m < 0) return hhmm == null ? "" : hhmm;
        int h = m / 60, min = m % 60;
        String ampm = h >= 12 ? "PM" : "AM";
        int h12 = h % 12;
        if (h12 == 0) h12 = 12;
        return String.format(Locale.getDefault(), "%d:%02d %s", h12, min, ampm);
    }

    /** Sorts entries by day, then start time — the display order everywhere. */
    public static void sort(List<RoutineEntry> entries) {
        if (entries == null) return;
        Collections.sort(entries, new Comparator<RoutineEntry>() {
            @Override
            public int compare(RoutineEntry a, RoutineEntry b) {
                if (a.getDay() != b.getDay()) return Integer.compare(a.getDay(), b.getDay());
                return Integer.compare(startMinutes(a.getStart()), startMinutes(b.getStart()));
            }
        });
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
