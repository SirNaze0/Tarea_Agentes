import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import db.SQLiteManager;

public class SolicitudValidator {

    public static String validarSolicitud(SolicitudCursoDTO s) {

        if (s.escuela_profesional == null || s.escuela_profesional.isEmpty()) {
            return "OBSERVADO POR: escuela profesional no especificada";
        }

        String escuela = TextUtils.normalizarMinusculas(s.escuela_profesional);

        if (!(escuela.contains("sistemas") && escuela.contains("ingenier"))) {
            System.out.println(escuela);
            return "OBSERVADO POR: escuela profesional incorrecta o mal escrita";
        }

        if (s.tipo_curso == null || !s.tipo_curso.toLowerCase().contains("verano")) {
            return "OBSERVADO POR: tipo de curso no corresponde a curso de verano";
        }

        if (s.numero_alumnos_detectados < 25) {
            return "OBSERVADO POR: número mínimo de alumnos no alcanzado (mínimo 25)";
        }

        if (s.docente == null || s.docente.nombre == null || s.docente.nombre.isEmpty()) {
            return "OBSERVADO POR: docente no identificado en el documento";
        }

        // 🔹 NUEVA REGLA: profesor nombrado
        if (!esProfesorNombrado(s.docente.nombre)) {
            System.out.println(s.docente.nombre);
            return "OBSERVADO POR: docente no registrado como profesor nombrado";
        }

        return "OK";
    }

    private static boolean esProfesorNombrado(String nombreDocente) {
        String[] partes = NombreProfesorUtils.prepararNombreParaBusqueda(nombreDocente);
        
        String nombres = TextUtils.normalizarMinusculas(partes[0]);
        String apellidos = TextUtils.normalizarMinusculas(partes[1]);

        String sql =
            "SELECT 1 FROM Profesor_Nombrado " +
            "WHERE lower(apellidos) = ? AND lower(nombres) = ? " +
            "LIMIT 1";

        try (Connection conn = SQLiteManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, apellidos);
            ps.setString(2, nombres);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            return false; // conservador
        }
    }


    public static int aplicarEstrategiaConservadora(int alumnosRegex, int alumnosIA) {
        if (alumnosIA <= 0) return alumnosRegex;
        return Math.min(alumnosRegex, alumnosIA);
    }
}
