/**
 * Utilidades para procesamiento de nombres de profesores
 */
public class NombreProfesorUtils {
    
    /**
     * Divide un nombre completo de profesor en nombres y apellidos
     * Soporta formatos:
     * - "APELLIDOS, NOMBRES"
     * - "NOMBRES APELLIDOS"
     */
    public static String[] dividirNombreProfesor(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
            return new String[]{"", ""};
        }
        
        // Si contiene coma, asumir formato "APELLIDOS, NOMBRES"
        if (nombreCompleto.contains(",")) {
            String[] partes = nombreCompleto.split(",");
            if (partes.length == 2) {
                return new String[]{partes[1].trim(), partes[0].trim()};
            }
        }
        
        // Si no tiene coma, dividir por espacios
        String[] palabras = nombreCompleto.split("\\s+");
        
        if (palabras.length == 1) {
            return new String[]{palabras[0], ""};
        } else if (palabras.length == 2) {
            return new String[]{palabras[0], palabras[1]};
        } else if (palabras.length == 3) {
            return new String[]{palabras[0], palabras[1] + " " + palabras[2]};
        } else {
            // 4 o más palabras: primeras 2 son nombres, resto son apellidos
            String nombres = palabras[0] + " " + palabras[1];
            StringBuilder apellidos = new StringBuilder();
            for (int i = 2; i < palabras.length; i++) {
                apellidos.append(palabras[i]);
                if (i < palabras.length - 1) apellidos.append(" ");
            }
            return new String[]{nombres, apellidos.toString()};
        }
    }
    
    /**
     * Prepara un nombre de profesor para búsqueda en BD
     * Normaliza y divide en nombres y apellidos
     */
    public static String[] prepararNombreParaBusqueda(String nombreCompleto) {
        String nombreLimpio = TextUtils.limpiarNombre(nombreCompleto);
        return dividirNombreProfesor(nombreLimpio);
    }
}
