import java.util.regex.*;

public class HorarioParser {
    
    public static class HorarioDual {
        public String diaSemana1;
        public String horaInicio1;
        public String horaFin1;
        public String diaSemana2;
        public String horaInicio2;
        public String horaFin2;
        
        @Override
        public String toString() {
            return String.format("%s %s-%s / %s %s-%s",
                diaSemana1, horaInicio1, horaFin1,
                diaSemana2, horaInicio2, horaFin2);
        }
    }
    
    /**
     * Parsea horarios en los formatos:
     * - "Martes 14:00-18:00 / Jueves 14:00-18:00"
     * - "Martes - Jueves 18:00 - 22:00"
     * - "Viernes 18:00-22:00 / Sabado 08:00-12:00"
     */
    public static HorarioDual parsear(String horarioTexto) {
        if (horarioTexto == null || horarioTexto.trim().isEmpty()) {
            return null;
        }
        
        HorarioDual horario = new HorarioDual();
        
        // Formato 1: "Martes 14:00-18:00 / Jueves 14:00-18:00"
        Pattern patron1 = Pattern.compile(
            "(\\w+)\\s+(\\d{2}:\\d{2})-(\\d{2}:\\d{2})\\s*/\\s*(\\w+)\\s+(\\d{2}:\\d{2})-(\\d{2}:\\d{2})"
        );
        Matcher matcher1 = patron1.matcher(horarioTexto);
        
        if (matcher1.find()) {
            horario.diaSemana1 = matcher1.group(1);
            horario.horaInicio1 = matcher1.group(2);
            horario.horaFin1 = matcher1.group(3);
            horario.diaSemana2 = matcher1.group(4);
            horario.horaInicio2 = matcher1.group(5);
            horario.horaFin2 = matcher1.group(6);
            return horario;
        }
        
        // Formato 2: "Martes - Jueves 18:00 - 22:00"
        Pattern patron2 = Pattern.compile(
            "(\\w+)\\s*-\\s*(\\w+)\\s+(\\d{2}:\\d{2})\\s*-\\s*(\\d{2}:\\d{2})"
        );
        Matcher matcher2 = patron2.matcher(horarioTexto);
        
        if (matcher2.find()) {
            horario.diaSemana1 = matcher2.group(1);
            horario.diaSemana2 = matcher2.group(2);
            horario.horaInicio1 = matcher2.group(3);
            horario.horaFin1 = matcher2.group(4);
            horario.horaInicio2 = matcher2.group(3); // Mismo horario
            horario.horaFin2 = matcher2.group(4);    // Mismo horario
            return horario;
        }
        
        System.err.println("⚠️ No se pudo parsear horario: " + horarioTexto);
        return null;
    }
}