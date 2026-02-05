import java.util.ArrayList;
import java.util.List;

/**
 * Sistema centralizado de logging y notificaciones para agentes
 */
public class NotificationLogger {
    
    public enum TipoNotificacion {
        ERROR("[ERROR]"),
        LISTO("[LISTO]"),
        MSJ("[MSJ]");
        
        private final String prefijo;
        
        TipoNotificacion(String prefijo) {
            this.prefijo = prefijo;
        }
        
        public String getPrefijo() {
            return prefijo;
        }
    }
    
    private static List<String> notificaciones = new ArrayList<>();
    private static final int MAX_NOTIFICACIONES = 100;
    
    /**
     * Agrega una notificación al log
     */
    public static void agregar(String agente, TipoNotificacion tipo, String mensaje) {
        String notificacion = String.format("%s %s → %s", 
            tipo.getPrefijo(), agente, mensaje);
        notificaciones.add(notificacion);
        
        // Limitar tamaño para evitar consumo excesivo de memoria
        if (notificaciones.size() > MAX_NOTIFICACIONES) {
            notificaciones.remove(0);
        }
    }
    
    /**
     * Agrega un error
     */
    public static void error(String agente, String mensaje) {
        agregar(agente, TipoNotificacion.ERROR, mensaje);
    }
    
    /**
     * Agrega un mensaje de éxito/aprobación
     */
    public static void listo(String agente, String mensaje) {
        agregar(agente, TipoNotificacion.LISTO, mensaje);
    }
    
    /**
     * Agrega un mensaje entre agentes
     */
    public static void mensaje(String agenteOrigen, String agenteDestino, String mensaje) {
        // Formatear directamente sin usar agregar() para evitar duplicación
        String notificacion = String.format("%s %s -> %s: %s", 
            TipoNotificacion.MSJ.getPrefijo(), 
            capitalizar(agenteOrigen), 
            capitalizar(agenteDestino), 
            mensaje);
        notificaciones.add(notificacion);
        
        // Limitar tamaño para evitar consumo excesivo de memoria
        if (notificaciones.size() > MAX_NOTIFICACIONES) {
            notificaciones.remove(0);
        }
    }
    
    /**
     * Capitaliza la primera letra del nombre del agente
     */
    private static String capitalizar(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            return nombre;
        }
        // Si ya está capitalizado o tiene formato especial, mantenerlo
        if (nombre.length() > 0 && Character.isUpperCase(nombre.charAt(0))) {
            return nombre;
        }
        return nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
    }
    
    /**
     * Obtiene todas las notificaciones
     */
    public static List<String> obtenerNotificaciones() {
        return new ArrayList<>(notificaciones);
    }
    
    /**
     * Obtiene las últimas N notificaciones
     */
    public static List<String> obtenerUltimas(int cantidad) {
        int inicio = Math.max(0, notificaciones.size() - cantidad);
        return new ArrayList<>(notificaciones.subList(inicio, notificaciones.size()));
    }
    
    /**
     * Limpia todas las notificaciones
     */
    public static void limpiar() {
        notificaciones.clear();
    }
    
    /**
     * Obtiene el número total de notificaciones
     */
    public static int contar() {
        return notificaciones.size();
    }
}
