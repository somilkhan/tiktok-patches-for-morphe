/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/spoof/sim/SpoofSimPatch.java
 */

package app.morphe.extension.tiktok.spoof.sim;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.tiktok.settings.Settings;

import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;

@SuppressWarnings("unused")
public class SpoofSimPatch {
    private static boolean isContextNotSet(String fieldSpoofed) {
        if (Utils.getContext() != null) return false;
        Logger.printException(() -> "Context is not yet set, cannot spoof: " + fieldSpoofed, null);
        return true;
    }

    private static boolean enabled() {
        return Utils.getContext() != null && Settings.SIM_SPOOF.get();
    }

    /** Force TikTok's internal region gate to report that the current region is supported. */
    public static boolean isInTikTokRegion(boolean value) {
        if (!enabled()) return value;
        return true;
    }

    public static String getRegion(String value) {
        return getCountryIso(value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map spoofRegionMap(Map value) {
        if (value == null) return value;

        String iso = Settings.SIM_SPOOF_ISO.get();
        if (iso == null || iso.isEmpty()) return value;
        final String region = iso.toUpperCase(Locale.US);

        value.put("fake_region", region);
        value.put("carrier_region", region);
        value.put("region", region);
        value.put("op_region", region);
        value.put("sys_region", region);
        value.put("current_region", region);

        Logger.printDebug(() -> "Spoofing TikTok region map to: " + region);
        return value;
    }

    public static String getCountryIso(String value) {
        if (isContextNotSet("countryIso")) return value;
        if (Settings.SIM_SPOOF.get()) {
            String iso = Settings.SIM_SPOOF_ISO.get();
            Logger.printDebug(() -> "Spoofing countryIso from: " + value + " to: " + iso);
            return iso;
        }
        return value;
    }

    public static String getOperator(String value) {
        if (isContextNotSet("MCC-MNC")) return value;
        if (Settings.SIM_SPOOF.get()) {
            String mccMnc = Settings.SIMSPOOF_MCCMNC.get();
            Logger.printDebug(() -> "Spoofing sim MCC-MNC from: " + value + " to: " + mccMnc);
            return mccMnc;
        }
        return value;
    }

    public static String getOperatorName(String value) {
        if (isContextNotSet("operatorName")) return value;
        if (Settings.SIM_SPOOF.get()) {
            String operator = Settings.SIMSPOOF_OP_NAME.get();
            Logger.printDebug(() -> "Spoofing sim operatorName from: " + value + " to: " + operator);
            return operator;
        }
        return value;
    }

    public static CharSequence getCarrierIdName(CharSequence value) {
        if (!enabled()) return value;
        String operator = Settings.SIMSPOOF_OP_NAME.get();
        return operator == null || operator.isEmpty() ? value : operator;
    }

    public static int getCarrierId(int value) {
        return value;
    }

    /** Replace Locale.getDefault() result with a US locale when SIM spoof is enabled. */
    public static Locale getLocale(Locale value) {
        if (!enabled()) return value;
        return Locale.US;
    }

    /** Replace TimeZone.getDefault() result with a US timezone when SIM spoof is enabled. */
    public static TimeZone getTimeZone(TimeZone value) {
        if (!enabled()) return value;
        return TimeZone.getTimeZone("America/New_York");
    }
}
