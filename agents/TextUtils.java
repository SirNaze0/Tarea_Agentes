import java.text.Normalizer;

/**
 * Utilidades para normalización y limpieza de texto
 */
public class TextUtils {
    
    /**
     * Normaliza texto eliminando tildes y caracteres especiales
     * Ejemplos: "á" → "a", "é" → "e", "ñ" → "n"
     */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        
        // Eliminar tildes usando Normalizer
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        // Eliminar marcas diacríticas (tildes, diéresis, etc.)
        texto = texto.replaceAll("\\p{M}", "");
        
        return texto.trim();
    }
    
    /**
     * Normaliza texto y limpia espacios múltiples
     */
    public static String normalizarYLimpiar(String texto) {
        if (texto == null) return "";
        
        texto = normalizar(texto);
        // Limpiar espacios y saltos de línea
        texto = texto.replaceAll("\\r\\n", "\n");
        texto = texto.replaceAll("\\r", "\n");
        texto = texto.replaceAll("[ \\t]+", " ");
        texto = texto.replaceAll("\\s+", " ");
        
        return texto.trim();
    }
    
    /**
     * Normaliza texto y convierte a minúsculas
     */
    public static String normalizarMinusculas(String texto) {
        return normalizar(texto).toLowerCase();
    }
    
    /**
     * Limpia nombre eliminando tildes y espacios extra
     */
    public static String limpiarNombre(String nombre) {
        if (nombre == null) return "";
        
        nombre = normalizar(nombre);
        nombre = nombre.replaceAll("\\s+", " ").trim();
        
        return nombre;
    }
}
