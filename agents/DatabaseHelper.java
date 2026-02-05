import java.sql.*;
import db.SQLiteManager;
public class DatabaseHelper {
    
    public int registrarCursoCompleto(SolicitudCursoDTO solicitud, Plan2018.Curso curso, 
                                     HorarioParser.HorarioDual horario) {
        Connection conn = null;
        
        try {
            conn = SQLiteManager.getConnection();
            conn.setAutoCommit(false);
            
            // VALIDAR LÍMITE DE SECCIONES
            if (!verificarLimiteSecciones(conn, curso.nombre)) {
                conn.rollback();
                System.err.println("El curso ya tiene 2 secciones registradas");
                return -3;
            }
            
            // 1. INSERTAR HORARIO
            int idHorario = insertarHorario(conn, horario);
            if (idHorario == -1) {
                conn.rollback();
                return -1;
            }
            System.out.println("Horario guardado (ID: " + idHorario + ")");
            
            // 2. BUSCAR PROFESOR Y VALIDAR LÍMITE
            int idProfesor = obtenerProfesor(conn, solicitud);
            if (idProfesor == -1) {
                conn.rollback();
                if (verificarSiEsLimiteCursos(conn, solicitud)) {
                    return -2;
                }
                return -1;
            }
            System.out.println("Profesor asignado (ID: " + idProfesor + ")");
            
            // 3. INSERTAR CURSO
            int idCurso = insertarCurso(conn, curso, idProfesor, idHorario);
            if (idCurso == -1) {
                conn.rollback();
                return -1;
            }
            System.out.println("Curso registrado (ID: " + idCurso + ")");
            
            conn.commit();
            return idCurso;
            
        } catch (SQLException e) {
            System.err.println("Error en transacción:");
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return -1;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private boolean verificarLimiteSecciones(Connection conn, String nombreCurso) {
        String sql = "SELECT COUNT(*) as total FROM Curso WHERE UPPER(Nombre_Curso) = UPPER(?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreCurso);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int total = rs.getInt("total");
                System.out.println("Secciones actuales del curso '" + nombreCurso + "': " + total + "/2");
                return total < 2;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar límite de secciones:");
            e.printStackTrace();
        }
        
        return false;
    }
    
    private int insertarHorario(Connection conn, HorarioParser.HorarioDual horario) {
        String sql = "INSERT INTO Horario_Curso " +
                     "(Dia_Semana_1, Hora_Inicio_1, Hora_Fin_1, " +
                     " Dia_Semana_2, Hora_Inicio_2, Hora_Fin_2) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, horario.diaSemana1);
            pstmt.setString(2, horario.horaInicio1);
            pstmt.setString(3, horario.horaFin1);
            pstmt.setString(4, horario.diaSemana2);
            pstmt.setString(5, horario.horaInicio2);
            pstmt.setString(6, horario.horaFin2);
            
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar horario:");
            e.printStackTrace();
        }
        
        return -1;
    }
    
    private int obtenerProfesor(Connection conn, SolicitudCursoDTO solicitud) {
        String nombreCompleto = solicitud.docente.nombre;
        
        if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
            System.err.println("Nombre de docente vacío");
            return -1;
        }
        
        System.out.println("Buscando profesor: " + nombreCompleto);
        
        String[] partes = NombreProfesorUtils.prepararNombreParaBusqueda(nombreCompleto);
        String nombres = partes[0];
        String apellidos = partes[1];
        
        System.out.println("   Nombres: " + nombres);
        System.out.println("   Apellidos: " + apellidos);
        
        String sql = "SELECT ID_Profesor_Nombrado FROM Profesor_Nombrado " +
                     "WHERE UPPER(Nombres) = UPPER(?) AND UPPER(Apellidos) = UPPER(?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombres);
            pstmt.setString(2, apellidos);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int idProfesor = rs.getInt("ID_Profesor_Nombrado");
                System.out.println("Profesor encontrado (ID: " + idProfesor + ")");
                
                if (verificarLimiteCursos(conn, idProfesor)) {
                    return idProfesor;
                } else {
                    System.err.println("El profesor ya tiene 2 cursos asignados");
                    return -1;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar profesor:");
            e.printStackTrace();
        }
        
        System.err.println("Profesor NO encontrado en BD");
        return -1;
    }
    
    private boolean verificarLimiteCursos(Connection conn, int idProfesor) {
        String sql = "SELECT COUNT(*) as total FROM Curso WHERE ID_Profesor_Nombrado = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idProfesor);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int total = rs.getInt("total");
                System.out.println("Cursos actuales del profesor: " + total + "/2");
                return total < 2;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar límite de cursos:");
            e.printStackTrace();
        }
        
        return false;
    }
    
    private boolean verificarSiEsLimiteCursos(Connection conn, SolicitudCursoDTO solicitud) {
        String[] partes = NombreProfesorUtils.prepararNombreParaBusqueda(solicitud.docente.nombre);
        
        String sql = "SELECT ID_Profesor_Nombrado FROM Profesor_Nombrado " +
                     "WHERE UPPER(Nombres) = UPPER(?) AND UPPER(Apellidos) = UPPER(?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, partes[0]);
            pstmt.setString(2, partes[1]);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int idProfesor = rs.getInt("ID_Profesor_Nombrado");
                return !verificarLimiteCursos(conn, idProfesor);
            }
        } catch (SQLException e) {
            // Ignorar
        }
        
        return false;
    }
    
    private int insertarCurso(Connection conn, Plan2018.Curso curso, int idProfesor, int idHorario) {
        String sql = "INSERT INTO Curso (ID_Profesor_Nombrado, ID_Horario_Curso, Codigo_Curso, Nombre_Curso, Ciclo) " +
             "VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, idProfesor);
            pstmt.setInt(2, idHorario);
            pstmt.setString(3, curso.codigo);  // Inserta el código del curso
            pstmt.setString(4, curso.nombre);
            pstmt.setString(5, String.valueOf(curso.ciclo));
            
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar curso:");
            e.printStackTrace();
        }
        
        return -1;
    }
}