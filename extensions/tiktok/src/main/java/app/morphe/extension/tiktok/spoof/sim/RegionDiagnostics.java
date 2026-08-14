package app.morphe.extension.tiktok.spoof.sim;

import android.util.Log;

/**
 * Opt-in diagnostics for investigating region/network decisions without changing
 * the values returned by Android APIs.
 *
 * Only coarse metadata is logged; raw identifiers are deliberately omitted.
 */
@SuppressWarnings("unused")
public final class RegionDiagnostics {
    private static final String TAG = "MorpheRegionDiag";

    private RegionDiagnostics() {}

    public static String observeString(String source, String value) {
        if (value == null) {
            Log.d(TAG, source + "=null");
        } else {
            Log.d(TAG, source + "=present length=" + value.length());
        }
        return value;
    }

    public static int observeInt(String source, int value) {
        Log.d(TAG, source + "=present");
        return value;
    }

    public static boolean observeBoolean(String source, boolean value) {
        Log.d(TAG, source + "=" + value);
        return value;
    }

    public static Object observeObject(String source, Object value) {
        Log.d(TAG, source + "=" + (value == null ? "null" : "present"));
        return value;
    }
}
