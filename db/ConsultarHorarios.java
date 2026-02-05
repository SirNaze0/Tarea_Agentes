package db;

import java.sql.*;

public class ConsultarHorarios {
    
    private static final String DB_PATH = "jdbc:sqlite:data/escuela.db";
    
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📅 SISTEMA DE CONSULTA DE HORARIOS");
        System.out.println("═══════════════════════════════════════════════\n");
        
        if (args.length > 0) {
            if (args[0].equals("--todas")) {
                mostrarTodasLasTablas();
            } else if (args[0].equals("--estudiantes")) {
                insertarEstudiantes();
            }
        } else {
            consultarHorarios();
        }
    }
    
    private static void insertarEstudiantes() {
        System.out.println("👥 INSERTANDO ESTUDIANTES DE PRUEBA");
        System.out.println("═══════════════════════════════════════════════\n");
        
        int[][] estudiantes = {
            {22200025, 9},
            {22200022, 8},
            {22200023, 7},
            {22200024, 6},
            {22200021, 5}
        };
        
        String sql = "INSERT OR IGNORE INTO Estudiante (Codigo, Cantidad_Habilitada, Ciclo) VALUES (?, 0, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_PATH);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int insertados = 0;
            
            for (int[] est : estudiantes) {
                pstmt.setInt(1, est[0]); // Código
                pstmt.setInt(2, est[1]); // Ciclo
                
                int resultado = pstmt.executeUpdate();
                
                if (resultado > 0) {
                    System.out.println("✅ Estudiante " + est[0] + " (Ciclo " + est[1] + ") insertado");
                    insertados++;
                } else {
                    System.out.println("⚠️  Estudiante " + est[0] + " ya existe");
                }
            }
            
            System.out.println("\n═══════════════════════════════════════════════");
            System.out.println("✅ Total de estudiantes insertados: " + insertados);
            System.out.println("═══════════════════════════════════════════════\n");
            
            // Mostrar todos los estudiantes
            mostrarEstudiantes(conn);
            
        } catch (SQLException e) {
            System.err.println("❌ Error al insertar estudiantes:");
            e.printStackTrace();
        }
    }
    
    private static void mostrarEstudiantes(Connection conn) throws SQLException {
        System.out.println("📋 ESTUDIANTES REGISTRADOS:");
        System.out.println("─────────────────────────────────────────");
        
        String sql = "SELECT * FROM Estudiante ORDER BY Codigo";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                int id = rs.getInt("ID_Estudiante");
                int codigo = rs.getInt("Codigo");
                int cantidad = rs.getInt("Cantidad_Habilitada");
                int ciclo = rs.getInt("Ciclo");
                
                System.out.println("ID: " + id + " | Código: " + codigo + 
                                   " | Ciclo: " + ciclo + " | Cursos habilitados: " + cantidad);
            }
        }
        
        System.out.println("─────────────────────────────────────────\n");
    }
    
    private static void consultarHorarios() {
        String sql = "SELECT * FROM Horario_Curso ORDER BY ID_Horario_Curso";
        
        try (Connection conn = DriverManager.getConnection(DB_PATH);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            boolean hayDatos = false;
            int contador = 0;
            
            while (rs.next()) {
                hayDatos = true;
                contador++;
                
                int id = rs.getInt("ID_Horario_Curso");
                String dia1 = rs.getString("Dia_Semana_1");
                String horaInicio1 = rs.getString("Hora_Inicio_1");
                String horaFin1 = rs.getString("Hora_Fin_1");
                String dia2 = rs.getString("Dia_Semana_2");
                String horaInicio2 = rs.getString("Hora_Inicio_2");
                String horaFin2 = rs.getString("Hora_Fin_2");
                
                System.out.println("┌─────────────────────────────────────────────┐");
                System.out.println("│ Horario #" + id);
                System.out.println("├─────────────────────────────────────────────┤");
                System.out.println("│ 📅 Día 1: " + dia1);
                System.out.println("│    ⏰ " + horaInicio1 + " - " + horaFin1);
                System.out.println("│");
                System.out.println("│ 📅 Día 2: " + dia2);
                System.out.println("│    ⏰ " + horaInicio2 + " - " + horaFin2);
                System.out.println("└─────────────────────────────────────────────┘");
                System.out.println();
            }
            
            if (!hayDatos) {
                System.out.println("╔═══════════════════════════════════════════╗");
                System.out.println("║  ⚠️  No hay horarios registrados aún      ║");
                System.out.println("╚═══════════════════════════════════════════╝");
            } else {
                System.out.println("═══════════════════════════════════════════════");
                System.out.println("✅ Total de horarios: " + contador);
                System.out.println("═══════════════════════════════════════════════");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error al consultar horarios:");
            e.printStackTrace();
        }
    }
    
    private static void mostrarTodasLasTablas() {
        System.out.println("📊 MOSTRANDO TODAS LAS TABLAS\n");
        
        String[] tablas = {
            "Profesor_Nombrado",
            "Horario_Curso", 
            "Curso",
            "Estudiante",
            "Matricula",
            "Pago"
        };
        
        for (String tabla : tablas) {
            mostrarTabla(tabla);
        }
    }
    
    private static void mostrarTabla(String nombreTabla) {
        System.out.println("─────────────────────────────────────────");
        System.out.println("📄 Tabla: " + nombreTabla);
        System.out.println("─────────────────────────────────────────");
        
        try (Connection conn = DriverManager.getConnection(DB_PATH);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + nombreTabla)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int numColumnas = metaData.getColumnCount();
            
            boolean hayDatos = false;
            while (rs.next()) {
                hayDatos = true;
                for (int i = 1; i <= numColumnas; i++) {
                    System.out.print(metaData.getColumnName(i) + "=");
                    System.out.print(rs.getString(i) + " | ");
                }
                System.out.println();
            }
            
            if (!hayDatos) {
                System.out.println("(vacía)");
            }
            
        } catch (SQLException e) {
            System.out.println("(vacía o error)");
        }
        
        System.out.println();
    }
}