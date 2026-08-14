/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/spoof/sim/SpoofSimPatch.java
 */
package app.morphe.extension.tiktok.spoof.sim;

import app.morphe.extension.shared.Utils;
import app.morphe.extension.tiktok.settings.Settings;
import app.morphe.extension.tiktok.settings.SettingsStatus;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@SuppressWarnings("unused")
public class SpoofSimPatch {
    private static boolean enabled() { return SettingsStatus.simSpoofEnabled || (Utils.getContext() != null && Settings.SIM_SPOOF.get()); }
    private static String iso() {
        String value = Settings.SIM_SPOOF_ISO.get();
        return value == null || value.isEmpty() ? "US" : value.toUpperCase(Locale.US);
    }
    public static boolean isInTikTokRegion() { return enabled(); }
    public static boolean isInTikTokRegion(boolean value) { return enabled() ? true : value; }
    public static boolean isUS(boolean value) { if (!enabled()) return value; String region = iso(); return "US".equals(region); }
    public static boolean isUK(boolean value) { if (!enabled()) return value; String region = iso(); return "GB".equals(region) || "UK".equals(region); }
    public static String getRegion(String value) {
        RegionDiagnostics.observeString("region.original", value);
        return enabled() ? iso() : value;
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map spoofRegionMap(Map value) {
        RegionDiagnostics.observeObject("region.map.original", value);
        if (!enabled() || value == null) return value;
        String region = iso();
        value.put("fake_region", region); value.put("carrier_region", region); value.put("region", region);
        value.put("op_region", region); value.put("sys_region", region); value.put("current_region", region);
        return value;
    }
    public static String getCountryIso(String value) {
        RegionDiagnostics.observeString("countryIso.original", value);
        return enabled() ? iso() : value;
    }
    public static String getOperator(String value) {
        RegionDiagnostics.observeString("operator.original", value);
        if (!enabled()) return value;
        String result = Settings.SIMSPOOF_MCCMNC.get();
        return result == null || result.isEmpty() ? value : result;
    }
    public static String getOperatorName(String value) {
        RegionDiagnostics.observeString("operatorName.original", value);
        if (!enabled()) return value;
        String result = Settings.SIMSPOOF_OP_NAME.get();
        return result == null || result.isEmpty() ? value : result;
    }
    public static CharSequence getCarrierIdName(CharSequence value) {
        RegionDiagnostics.observeObject("carrierIdName.original", value);
        if (!enabled()) return value;
        String operator = Settings.SIMSPOOF_OP_NAME.get();
        return operator == null || operator.isEmpty() ? value : operator;
    }
    public static int getCarrierId(int value) {
        return RegionDiagnostics.observeInt("carrierId.original", value);
    }

    // Step 1: exact Jaggu native getNetworkSpecifier behavior.
    public static String getNetworkSpecifier(String value) {
        RegionDiagnostics.observeString("networkSpecifier.original", value);
        return enabled() ? null : value;
    }

    // Step 2: retained as-is while diagnostics establish the real runtime path.
    public static String getNetworkCountryIso(String value) {
        RegionDiagnostics.observeString("networkCountryIso.original", value);
        return enabled() ? null : value;
    }
    public static String getNetworkOperator(String value) {
        RegionDiagnostics.observeString("networkOperator.original", value);
        return enabled() ? null : value;
    }
    public static String getNetworkOperatorName(String value) {
        RegionDiagnostics.observeString("networkOperatorName.original", value);
        return enabled() ? null : value;
    }

    public static Locale getLocale(Locale value) { return enabled() ? Locale.US : value; }
    public static TimeZone getTimeZone(TimeZone value) { return enabled() ? TimeZone.getTimeZone("America/New_York") : value; }
    public static String fixNetworkHost(String value) { return value; }
    public static String fixNetworkDns(String value) { return value; }
}
