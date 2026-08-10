package app.morphe.extension.tiktok.region;

/**
 * Small diagnostic helper for region-spoof development.
 * Keeps region normalization in one place without modifying authentication.
 */
public final class RegionSpoofDiagnostics {
    private RegionSpoofDiagnostics() {}

    public static String normalizeIso(String iso) {
        if (iso == null) return "";
        return iso.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static boolean isValidIso(String iso) {
        String value = normalizeIso(iso);
        return value.length() == 2
                && Character.isLetter(value.charAt(0))
                && Character.isLetter(value.charAt(1));
    }
}
