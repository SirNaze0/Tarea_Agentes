import java.sql.*;
import db.SQLiteManager;

/**
 * Utilidades para operaciones de base de datos relacionadas con estudiantes
 */
public class EstudianteDBUtils {
    
    /**
     * Obtiene el ID de un estudiante por su código
     */
    public static Integer obtenerIdEstudiante(int codigo) {
        String sql = "SELECT ID_Estudiante FROM Estudiante WHERE Codigo = ?";
        
        try (Connection conn = SQLiteManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, codigo);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("ID_Estudiante");
            }
            
        } catch (SQLException e) {
            System.out.println("Error al buscar estudiante: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Obtiene información completa de un estudiante por su código
     */
    public static EstudianteInfo obtenerEstudiante(int codigo) {
        String sql = "SELECT ID_Estudiante, Codigo, Ciclo, Cantidad_Habilitada " +
                     "FROM Estudiante WHERE Codigo = ?";
        
        try (Connection conn = SQLiteManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, codigo);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                EstudianteInfo est = new EstudianteInfo();
                est.id = rs.getInt("ID_Estudiante");
                est.codigo = rs.getInt("Codigo");
                est.ciclo = rs.getInt("Ciclo");
                est.cantidadHabilitada = rs.getInt("Cantidad_Habilitada");
                return est;
            }
            
        } catch (SQLException e) {
            System.out.println("Error al buscar estudiante: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Cuenta los pagos de un estudiante
     */
    public static int contarPagos(int idEstudiante) {
        String sql = "SELECT COUNT(*) as total FROM Pago WHERE ID_Estudiante = ?";
        
        try (Connection conn = SQLiteManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idEstudiante);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            System.out.println("Error al contar pagos: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Verifica si un estudiante ya está matriculado
     */
    public static boolean yaEstaMatriculado(int idEstudiante) {
        String sql = "SELECT COUNT(*) as total FROM Matricula WHERE ID_Estudiante = ?";
        
        try (Connection conn = SQLiteManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idEstudiante);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int total = rs.getInt("total");
                return total > 0;
            }
            
        } catch (SQLException e) {
            System.out.println("Error al verificar matrícula: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Actualiza la cantidad habilitada de un estudiante
     */
    public static boolean actualizarCantidadHabilitada(int idEstudiante, int cantidadPagos) {
        String sql = "UPDATE Estudiante SET Cantidad_Habilitada = ? WHERE ID_Estudiante = ?";
        
        try (Connection conn = SQLiteManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, cantidadPagos);
            pstmt.setInt(2, idEstudiante);
            
            int filasActualizadas = pstmt.executeUpdate();
            return filasActualizadas > 0;
            
        } catch (SQLException e) {
            System.out.println("Error al actualizar cantidad habilitada: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Clase para almacenar información de estudiante
     */
    public static class EstudianteInfo {
        public int id;
        public int codigo;
        public int ciclo;
        public int cantidadHabilitada;
    }
}
