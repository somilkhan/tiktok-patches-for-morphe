/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/spoof/sim/SpoofSimPatch.java
 */

package app.morphe.extension.tiktok.spoof.sim;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.tiktok.settings.Settings;

import java.util.Map;

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

    /**
     * Region resolver hook for TikTok 46.2.3 X.C35590hVz.LIZ().
     * This method reads fake_region > carrier_region > sys_region > app_language
     * and returns the effective 2-letter region code.
     */
    public static String getRegion(String value) {
        return getCountryIso(value);
    }

    /**
     * Patch the region map produced by TikTok's global feature/config provider.
     * C379311yQ exposes region signals including fake_region, carrier_region,
     * region and op_region. Setting them at the map boundary is earlier and
     * more direct than relying only on a later String resolver.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map spoofRegionMap(Map value) {
        if (!enabled() || value == null) return value;

        String iso = Settings.SIM_SPOOF_ISO.get();
        if (iso == null || iso.isEmpty()) return value;
        final String region = iso.toUpperCase(java.util.Locale.US);

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
}
