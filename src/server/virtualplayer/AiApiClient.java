package server.virtualplayer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Client for calling the external AI API.
 *
 * Request (JSON):
 *   { "character": "<botName>", "context": "<history>" }
 *
 * Response (JSON):
 *   { "messages": "<reply>" }
 *   Empty string means the bot should not reply.
 */
public class AiApiClient {

    private static final String API_URL = System.getenv("N8N_MAPLESTORY_CHARACTER_URL");
    private static final String AI_API_USER = System.getenv("N8N_MAPLESTORY_CHARACTER_USER");
    private static final String AI_API_PASSWORD = System.getenv("N8N_MAPLESTORY_CHARACTER_PASSWORD");
    private static final int TIMEOUT_MS = 30000;

    private AiApiClient() {}

    /**
     * Call AI API and return the reply text.
     *
     * @param role    Bot character name (角色)
     * @param history Concatenated chat history string (歷史對話)
     * @return Reply string, or empty string if bot should stay silent
     */
    public static String chat(String role, String history) {
        if (API_URL == null || API_URL.isEmpty()) {
            System.err.println("[AiApiClient] N8N_MAPLESTORY_CHARACTER_URL env var not set");
            return "";
        }
        try {
            String payload = buildPayload(role, history);

            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("User-Agent", "curl/7.88.1");
            if (AI_API_USER != null && AI_API_PASSWORD != null) {
                String credentials = AI_API_USER + ":" + AI_API_PASSWORD;
                String encoded = java.util.Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                conn.setRequestProperty("Authorization", "Basic " + encoded);
            }
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                System.err.println("[AiApiClient] API returned status " + status);
                return "";
            }

            String body = readStream(conn.getInputStream());
            return parseMessages(body);

        } catch (Exception e) {
            System.err.println("[AiApiClient] Request failed: " + e.getMessage());
            return "";
        }
    }

    private static String readStream(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int len;
        while ((len = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, len);
        }
        return buffer.toString("UTF-8");
    }

    private static String buildPayload(String role, String history) {
        return "{\"character\":\"" + escapeJson(role) + "\",\"context\":\"" + escapeJson(history) + "\"}";
    }

    /**
     * Parse the "messages" field from the JSON response.
     * Supports both flat JSON { "messages": "..." } and array [{ "messages": "..." }]
     */
    private static String parseMessages(String json) {
        String key = "\"messages\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx == -1) return "";

        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx == -1) return "";

        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart == -1) return "";

        int quoteEnd = quoteStart + 1;
        while (quoteEnd < json.length()) {
            if (json.charAt(quoteEnd) == '"' && json.charAt(quoteEnd - 1) != '\\') break;
            quoteEnd++;
        }

        if (quoteEnd >= json.length()) return "";
        return json.substring(quoteStart + 1, quoteEnd);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
