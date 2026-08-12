/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/spoof/sim/SpoofSimPatch.java
 */

package app.morphe.extension.tiktok.spoof.sim;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.tiktok.settings.Settings;

import java.util.Locale;

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
     * Primary region resolver hook for TikTok 46.2.3.
     * Called from X.C35590hVz.LIZ() which reads
     * fake_region > carrier_region > sys_region > app_language.
     */
    public static String getRegion(String value) {
        return getCountryIso(value);
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
