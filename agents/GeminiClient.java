import java.net.URI;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.*;
import java.util.Set;
import java.util.HashSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GeminiClient {
    private static final String GEMINI_API_KEY = "INGRESAR TU API KEY ACA :V";

    public GeminiClient() {
    }

    public String llamarIA(String prompt) {try {
            URI uri = new URI(
                "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key="
                + GEMINI_API_KEY
            );

            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String body =
                "{"
              + "  \"contents\": ["
              + "    {"
              + "      \"role\": \"user\","
              + "      \"parts\": ["
              + "        { \"text\": " + JsonUtils.escapeJson(prompt) + " }"
              + "      ]"
              + "    }"
              + "  ],"
              + "  \"generationConfig\": {"
              + "    \"temperature\": 0"
              + "  }"
              + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String extraerJsonDeRespuesta(String respuestaIA) {
        try {
            JsonObject root = JsonParser.parseString(respuestaIA).getAsJsonObject();
            String text = root
                .getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();

            // 🔹 Limpiar ```json y ```
            text = text.replaceAll("(?s)```json", "").replaceAll("```", "").trim();

            return text;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
