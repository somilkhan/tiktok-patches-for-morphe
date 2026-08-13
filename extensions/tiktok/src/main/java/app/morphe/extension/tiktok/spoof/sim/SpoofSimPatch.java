/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/spoof/sim/SpoofSimPatch.java
 */

package app.morphe.extension.tiktok.spoof.sim;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.tiktok.settings.Settings;
import app.morphe.extension.tiktok.settings.SettingsStatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;

@SuppressWarnings("unused")
public class SpoofSimPatch {
    private static boolean enabled() {
        return SettingsStatus.simSpoofEnabled ||
                (Utils.getContext() != null && Settings.SIM_SPOOF.get());
    }

    private static String iso() {
        String value = Settings.SIM_SPOOF_ISO.get();
        return value == null || value.isEmpty() ? "US" : value.toUpperCase(Locale.US);
    }

    public static boolean isInTikTokRegion() {
        return enabled();
    }

    public static boolean isInTikTokRegion(boolean value) {
        return enabled() ? true : value;
    }

    public static boolean isUS(boolean value) {
        return enabled() ? "US".equals(iso()) : value;
    }

    public static boolean isUK(boolean value) {
        if (!enabled()) return value;
        String region = iso();
        return "GB".equals(region) || "UK".equals(region);
    }

    public static String getRegion(String value) {
        return enabled() ? iso() : value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map spoofRegionMap(Map value) {
        if (!enabled() || value == null) return value;
        final String region = iso();
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
        if (!enabled()) return value;
        String result = iso();
        Logger.printDebug(() -> "Spoofing countryIso from: " + value + " to: " + result);
        return result;
    }

    public static String getOperator(String value) {
        if (!enabled()) return value;
        String result = Settings.SIMSPOOF_MCCMNC.get();
        return result == null || result.isEmpty() ? value : result;
    }

    public static String getOperatorName(String value) {
        if (!enabled()) return value;
        String result = Settings.SIMSPOOF_OP_NAME.get();
        return result == null || result.isEmpty() ? value : result;
    }

    public static CharSequence getCarrierIdName(CharSequence value) {
        if (!enabled()) return value;
        String operator = Settings.SIMSPOOF_OP_NAME.get();
        return operator == null || operator.isEmpty() ? value : operator;
    }

    public static int getCarrierId(int value) {
        return value;
    }

    public static Locale getLocale(Locale value) {
        return enabled() ? Locale.US : value;
    }

    public static TimeZone getTimeZone(TimeZone value) {
        return enabled() ? TimeZone.getTimeZone("America/New_York") : value;
    }

    /**
     * Resolves TikTok hostnames through HTTPS DNS instead of the device's configured DNS.
     * This is intentionally app-scoped: it does not change Android's DNS settings and does
     * not create a VPN interface. If DoH fails, the original result is preserved.
     */
    public static InetAddress[] resolveAll(String host, InetAddress[] original) {
        if (!enabled() || host == null || host.isEmpty() || isDohHost(host)) return original;
        try {
            List<InetAddress> addresses = doh(host, "A");
            if (!addresses.isEmpty()) return addresses.toArray(new InetAddress[0]);
        } catch (Throwable ignored) {
            Logger.printDebug(() -> "DoH resolution failed for " + host);
        }
        return original;
    }

    public static InetAddress resolveOne(String host, InetAddress original) {
        if (!enabled() || host == null || host.isEmpty() || isDohHost(host)) return original;
        try {
            List<InetAddress> addresses = doh(host, "A");
            if (!addresses.isEmpty()) return addresses.get(0);
        } catch (Throwable ignored) {
            Logger.printDebug(() -> "DoH resolution failed for " + host);
        }
        return original;
    }

    private static boolean isDohHost(String host) {
        String h = host.toLowerCase(Locale.US);
        return h.equals("cloudflare-dns.com") || h.endsWith(".cloudflare-dns.com") ||
                h.equals("dns.google") || h.endsWith(".dns.google");
    }

    private static List<InetAddress> doh(String host, String type) throws Exception {
        String encoded = URLEncoder.encode(host, "UTF-8");
        String[] endpoints = {
                "https://cloudflare-dns.com/dns-query?name=" + encoded + "&type=" + type,
                "https://dns.google/resolve?name=" + encoded + "&type=" + type
        };
        for (String endpoint : endpoints) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(4000);
                connection.setReadTimeout(4000);
                connection.setRequestProperty("Accept", "application/dns-json");
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) continue;
                String json = read(connection.getInputStream());
                JSONObject root = new JSONObject(json);
                JSONArray answers = root.optJSONArray("Answer");
                if (answers == null) continue;
                List<InetAddress> result = new ArrayList<>();
                for (int i = 0; i < answers.length(); i++) {
                    JSONObject answer = answers.optJSONObject(i);
                    if (answer == null) continue;
                    int answerType = answer.optInt("type", -1);
                    if (answerType != 1) continue;
                    String data = answer.optString("data", "");
                    if (!data.isEmpty()) result.add(InetAddress.getByAddress(host, parseIpv4(data)));
                }
                if (!result.isEmpty()) return result;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return new ArrayList<>();
    }

    private static byte[] parseIpv4(String value) {
        String[] parts = value.split("\\.");
        if (parts.length != 4) throw new IllegalArgumentException("Not IPv4: " + value);
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            int part = Integer.parseInt(parts[i]);
            if (part < 0 || part > 255) throw new IllegalArgumentException("Not IPv4: " + value);
            bytes[i] = (byte) part;
        }
        return bytes;
    }

    private static String read(InputStream input) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) result.append(line);
        reader.close();
        return result.toString();
    }
}
