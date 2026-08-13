package app.morphe.extension.tiktok.spoof.sim;

/**
 * Narrow network compatibility helpers used by the 46.x spoof layer.
 * This intentionally does not replace the process-wide DNS resolver.
 */
@SuppressWarnings("unused")
public final class JagguNetworkPatch {
    private JagguNetworkPatch() {}

    public static String preserveDnsValue(String value) {
        return value;
    }

    public static String preserveHost(String value) {
        return value;
    }

    public static boolean preserveNetworkState(boolean value) {
        return value;
    }
}
