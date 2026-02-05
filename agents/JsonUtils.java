/**
 * Utilidades para manipulación de JSON
 */
public class JsonUtils {
    
    /**
     * Escapa caracteres especiales en un texto para uso en JSON
     */
    public static String escapeJson(String text) {
        if (text == null) return "\"\"";
        
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "") + "\"";
    }
}
