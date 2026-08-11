/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/spoof/sim/SpoofSimPatch.java
 */

package app.morphe.extension.tiktok.spoof.sim;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.tiktok.settings.Settings;

import java.util.Locale;
import java.util.TimeZone;

@SuppressWarnings("unused")
public class SpoofSimPatch {
    private static boolean isContextNotSet(String fieldSpoofed) {
        if (Utils.getContext() != null) {
            return false;
        }

        Logger.printException(() -> "Context is not yet set, cannot spoof: " + fieldSpoofed, null);
        return true;
    }

    private static boolean enabled() {
        return Utils.getContext() != null && Settings.SIM_SPOOF.get();
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

    /**
     * Keeps Java/Android locale signals consistent with the selected spoofed country.
     * This is intentionally limited to the TikTok process by the bytecode hook.
     */
    public static Locale getDefaultLocale(Locale value) {
        if (!enabled()) return value;

        String iso = Settings.SIM_SPOOF_ISO.get();
        if (iso == null || !iso.matches("[a-zA-Z]{2}")) return value;

        Locale spoofed = new Locale("en", iso.toUpperCase(Locale.ROOT));
        Logger.printDebug(() -> "Spoofing default locale from: " + value + " to: " + spoofed);
        return spoofed;
    }

    /**
     * Keeps the process timezone aligned with the spoofed region where a practical
     * single representative timezone exists. Unknown countries retain the device TZ.
     */
    public static TimeZone getDefaultTimeZone(TimeZone value) {
        if (!enabled()) return value;

        String iso = Settings.SIM_SPOOF_ISO.get();
        if (iso == null) return value;

        String zoneId = timezoneFor(iso.toUpperCase(Locale.ROOT));
        if (zoneId == null) return value;

        TimeZone spoofed = TimeZone.getTimeZone(zoneId);
        Logger.printDebug(() -> "Spoofing default timezone from: " + value.getID() + " to: " + zoneId);
        return spoofed;
    }

    private static String timezoneFor(String iso) {
        switch (iso) {
            case "US": return "America/New_York";
            case "CA": return "America/Toronto";
            case "GB": return "Europe/London";
            case "IE": return "Europe/Dublin";
            case "DE": return "Europe/Berlin";
            case "FR": return "Europe/Paris";
            case "IT": return "Europe/Rome";
            case "ES": return "Europe/Madrid";
            case "NL": return "Europe/Amsterdam";
            case "SE": return "Europe/Stockholm";
            case "NO": return "Europe/Oslo";
            case "FI": return "Europe/Helsinki";
            case "PL": return "Europe/Warsaw";
            case "TR": return "Europe/Istanbul";
            case "JP": return "Asia/Tokyo";
            case "KR": return "Asia/Seoul";
            case "CN": return "Asia/Shanghai";
            case "SG": return "Asia/Singapore";
            case "AU": return "Australia/Sydney";
            case "NZ": return "Pacific/Auckland";
            case "BR": return "America/Sao_Paulo";
            case "MX": return "America/Mexico_City";
            case "AR": return "America/Argentina/Buenos_Aires";
            case "CL": return "America/Santiago";
            case "IN": return "Asia/Kolkata";
            default: return null;
        }
    }

    public static CharSequence getCarrierIdName(CharSequence value) {
        if (!enabled()) return value;
        String operator = Settings.SIMSPOOF_OP_NAME.get();
        return operator == null || operator.isEmpty() ? value : operator;
    }

    public static int getCarrierId(int value) {
        // Carrier IDs are not country IDs. Do not invent one for a preset.
        // Returning the original value avoids creating an internally inconsistent carrier identity.
        return value;
    }
}
