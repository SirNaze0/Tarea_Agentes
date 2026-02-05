import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import java.sql.*;
import java.util.*;
import db.SQLiteManager;

public class MatriculaService {
    
    private Agent agent;
    private Scanner scanner;
    
    public MatriculaService(Agent agent) {
        this.agent = agent;
        this.scanner = new Scanner(System.in);
    }
    
    public void procesar(ACLMessage msg) {
        try {
            // 1. Solicitar código del estudiante
            System.out.print("\nIngrese su código de estudiante: ");
            int codigo = scanner.nextInt();
            scanner.nextLine(); // Consumir salto de línea
            
            // 2. Validar estudiante
            Estudiante estudiante = obtenerEstudiante(codigo);
            
            if (estudiante == null) {
                NotificationLogger.error("UnidadMatricula", 
                    "Estudiante no encontrado con código " + codigo);
                enviarRespuesta(msg, "Estudiante no encontrado");
                return;
            }
            
            if (estudiante.cantidadHabilitada <= 0) {
                NotificationLogger.error("UnidadMatricula", 
                    "Estudiante " + codigo + " no tiene cursos habilitados");
                enviarRespuesta(msg, "No tiene cursos habilitados");
                return;
            }
            
            
            // 3. Verificar si ya está matriculado
            if (yaEstaMatriculado(estudiante.id)) {
                NotificationLogger.error("UnidadMatricula", 
                    "Estudiante " + codigo + " ya está matriculado");
                enviarRespuesta(msg, "Ya te has matriculado");
                return;
            }
            
            // 4. Obtener cursos disponibles
            List<CursoDisponible> cursosDisponibles = obtenerCursosDisponibles(estudiante.ciclo);
            
            if (cursosDisponibles.isEmpty()) {
                NotificationLogger.error("UnidadMatricula", 
                    "No hay cursos disponibles para ciclo " + estudiante.ciclo);
                enviarRespuesta(msg, "No hay cursos disponibles");
                return;
            }
            
            // 5. Mostrar cursos disponibles (solo en consola del agente, no en notificaciones)
            mostrarCursosDisponibles(cursosDisponibles);
            
            // 6. Permitir selección de cursos
            List<CursoDisponible> cursosSeleccionados = seleccionarCursos(
                cursosDisponibles, 
                estudiante.cantidadHabilitada
            );
            
            if (cursosSeleccionados.isEmpty()) {
                NotificationLogger.error("UnidadMatricula", 
                    "Estudiante " + codigo + " no seleccionó cursos");
                enviarRespuesta(msg, "Matrícula cancelada");
                return;
            }
            
            // 7. Validar créditos (máximo 11)
            double totalCreditos = calcularTotalCreditos(cursosSeleccionados);
            if (totalCreditos > 11) {
                NotificationLogger.error("UnidadMatricula", 
                    "Estudiante " + codigo + " excede límite de créditos: " + totalCreditos);
                enviarRespuesta(msg, "Excede límite de créditos");
                return;
            }
            
            // 8. Validar cruces de horario
            if (tieneCruceHorarios(cursosSeleccionados)) {
                NotificationLogger.error("UnidadMatricula", 
                    "Estudiante " + codigo + " tiene cruce de horarios");
                enviarRespuesta(msg, "Cruce de horarios detectado");
                return;
            }
            
            // 9. Registrar matrícula
            boolean registrado = registrarMatricula(estudiante.id, cursosSeleccionados);
            
            if (registrado) {
                NotificationLogger.listo("UnidadMatricula", 
                    "MATRÍCULA EXITOSA - Estudiante: " + codigo + 
                    " | Cursos: " + cursosSeleccionados.size() + 
                    " | Créditos: " + totalCreditos);
                
                enviarRespuesta(msg, "Matrícula completada: " + cursosSeleccionados.size() + 
                                    " cursos, " + totalCreditos + " créditos");
            } else {
                NotificationLogger.error("UnidadMatricula", 
                    "No se pudo registrar la matrícula del estudiante " + codigo);
                enviarRespuesta(msg, "Error al registrar matrícula");
            }
            
        } catch (Exception e) {
            NotificationLogger.error("UnidadMatricula", 
                "Error en proceso de matrícula: " + e.getMessage());
            e.printStackTrace();
            enviarRespuesta(msg, "Error en el proceso de matrícula");
        }
    }
    
    // ========== CLASES AUXILIARES ==========
    
    private static class Estudiante {
        int id;
        int codigo;
        int ciclo;
        int cantidadHabilitada;
    }
    
    private static class CursoDisponible {
        int idCurso;
        String codigoCurso;
        String nombreCurso;
        String ciclo;
        double creditos;
        String dia1;
        String horaInicio1;
        String horaFin1;
        String dia2;
        String horaInicio2;
        String horaFin2;
    }
    
    // ========== MÉTODOS DE BASE DE DATOS ==========
    
    private Estudiante obtenerEstudiante(int codigo) {
        EstudianteDBUtils.EstudianteInfo info = EstudianteDBUtils.obtenerEstudiante(codigo);
        
        if (info == null) {
            return null;
        }
        
        Estudiante est = new Estudiante();
        est.id = info.id;
        est.codigo = info.codigo;
        est.ciclo = info.ciclo;
        est.cantidadHabilitada = info.cantidadHabilitada;
        return est;
    }
    
    private boolean yaEstaMatriculado(int idEstudiante) {
        return EstudianteDBUtils.yaEstaMatriculado(idEstudiante);
    }
    
    private List<CursoDisponible> obtenerCursosDisponibles(int cicloEstudiante) {
        List<CursoDisponible> cursos = new ArrayList<>();
        
        String sql = "SELECT c.ID_Curso, c.Codigo_Curso, c.Nombre_Curso, c.Ciclo, " +
                     "h.Dia_Semana_1, h.Hora_Inicio_1, h.Hora_Fin_1, " +
                     "h.Dia_Semana_2, h.Hora_Inicio_2, h.Hora_Fin_2 " +
                     "FROM Curso c " +
                     "INNER JOIN Horario_Curso h ON c.ID_Horario_Curso = h.ID_Horario_Curso " +
                     "WHERE CAST(c.Ciclo AS INTEGER) <= ?";
        
        try (Connection conn = SQLiteManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, cicloEstudiante);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                CursoDisponible curso = new CursoDisponible();
                curso.idCurso = rs.getInt("ID_Curso");
                curso.codigoCurso = rs.getString("Codigo_Curso");
                curso.nombreCurso = rs.getString("Nombre_Curso");
                curso.ciclo = rs.getString("Ciclo");
                curso.creditos = obtenerCreditosPorCodigo(curso.codigoCurso);
                curso.dia1 = rs.getString("Dia_Semana_1");
                curso.horaInicio1 = rs.getString("Hora_Inicio_1");
                curso.horaFin1 = rs.getString("Hora_Fin_1");
                curso.dia2 = rs.getString("Dia_Semana_2");
                curso.horaInicio2 = rs.getString("Hora_Inicio_2");
                curso.horaFin2 = rs.getString("Hora_Fin_2");
                
                cursos.add(curso);
            }
            
        } catch (SQLException e) {
            System.out.println("Error al obtener cursos: " + e.getMessage());
        }
        
        return cursos;
    }
    
    private double obtenerCreditosPorCodigo(String codigoCurso) {
        if (codigoCurso == null || codigoCurso.trim().isEmpty()) {
            System.out.println("Código de curso vacío, asignando 0 créditos por defecto");
            return 0.0;
        }
        
        Optional<Plan2018.Curso> cursoOpt = Plan2018.Curso.buscarPorCodigo(codigoCurso);
        
        if (cursoOpt.isPresent()) {
            return cursoOpt.get().creditos;
        } else {
            System.out.println("Curso " + codigoCurso + " no encontrado en Plan2018, asignando 0 créditos");
            return 0.0;
        }
    }
    
    private boolean registrarMatricula(int idEstudiante, List<CursoDisponible> cursos) {
        String sql = "INSERT INTO Matricula (ID_Estudiante, ID_Curso) VALUES (?, ?)";
        
        try (Connection conn = SQLiteManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            
            for (CursoDisponible curso : cursos) {
                pstmt.setInt(1, idEstudiante);
                pstmt.setInt(2, curso.idCurso);
                pstmt.executeUpdate();
            }
            
            conn.commit();
            conn.setAutoCommit(true);
            return true;
            
        } catch (SQLException e) {
            System.out.println("Error al registrar matrícula: " + e.getMessage());
            return false;
        }
    }
    
    // ========== MÉTODOS DE LÓGICA DE NEGOCIO ==========
    
    private void mostrarCursosDisponibles(List<CursoDisponible> cursos) {
        System.out.println("\nCURSOS DISPONIBLES:");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        for (int i = 0; i < cursos.size(); i++) {
            CursoDisponible c = cursos.get(i);
            System.out.println((i + 1) + ". " + c.nombreCurso + " (" + c.codigoCurso + ")");
            System.out.println("   Ciclo: " + c.ciclo + " | Créditos: " + c.creditos);
            System.out.println("   Horario:");
            System.out.println("     - " + c.dia1 + " " + c.horaInicio1 + " - " + c.horaFin1);
            System.out.println("     - " + c.dia2 + " " + c.horaInicio2 + " - " + c.horaFin2);
            System.out.println();
        }
        
        System.out.println("═══════════════════════════════════════════════════════════");
    }
    
    private List<CursoDisponible> seleccionarCursos(List<CursoDisponible> disponibles, 
                                                     int maxCursos) {
        List<CursoDisponible> seleccionados = new ArrayList<>();
        
        System.out.println("\nSELECCIÓN DE CURSOS");
        System.out.println("Puede seleccionar hasta " + maxCursos + " cursos (máximo 11 créditos)");
        System.out.println("Ingrese 0 para finalizar la selección");
        
        while (true) {
            System.out.print("\nIngrese el número del curso (0 para terminar): ");
            int opcion = scanner.nextInt();
            scanner.nextLine(); // Consumir salto de línea
            
            if (opcion == 0) {
                break;
            }
            
            if (opcion < 1 || opcion > disponibles.size()) {
                System.out.println("Opción inválida");
                continue;
            }
            
            CursoDisponible curso = disponibles.get(opcion - 1);
            
            // Verificar si ya fue seleccionado
            if (seleccionados.stream().anyMatch(c -> c.idCurso == curso.idCurso)) {
                System.out.println("Este curso ya fue seleccionado");
                continue;
            }
            
            // Verificar límite de cursos
            if (seleccionados.size() >= maxCursos) {
                System.out.println("Ya alcanzó el límite de cursos habilitados (" + maxCursos + ")");
                continue;
            }
            
            // Verificar límite de créditos
            double creditosActuales = calcularTotalCreditos(seleccionados);
            if (creditosActuales + curso.creditos > 11) {
                System.out.println("Excedería el límite de 11 créditos");
                System.out.println("Créditos actuales: " + creditosActuales);
                System.out.println("Créditos del curso: " + curso.creditos);
                continue;
            }
            
            seleccionados.add(curso);
            System.out.println("Curso agregado: " + curso.nombreCurso + 
                             " (" + curso.creditos + " créditos)");
            System.out.println("   Total: " + seleccionados.size() + " cursos, " + 
                             calcularTotalCreditos(seleccionados) + " créditos");
        }
        
        return seleccionados;
    }
    
    private double calcularTotalCreditos(List<CursoDisponible> cursos) {
        return cursos.stream().mapToDouble(c -> c.creditos).sum();
    }
    
    private boolean tieneCruceHorarios(List<CursoDisponible> cursos) {
        for (int i = 0; i < cursos.size(); i++) {
            for (int j = i + 1; j < cursos.size(); j++) {
                if (seCruzanHorarios(cursos.get(i), cursos.get(j))) {
                    System.out.println("Cruce detectado entre:");
                    System.out.println("   - " + cursos.get(i).nombreCurso);
                    System.out.println("   - " + cursos.get(j).nombreCurso);
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean seCruzanHorarios(CursoDisponible c1, CursoDisponible c2) {
        // Verificar cruce en día 1 de c1 con ambos días de c2
        if (seCruzanEnDia(c1.dia1, c1.horaInicio1, c1.horaFin1, 
                          c2.dia1, c2.horaInicio1, c2.horaFin1)) {
            return true;
        }
        if (seCruzanEnDia(c1.dia1, c1.horaInicio1, c1.horaFin1, 
                          c2.dia2, c2.horaInicio2, c2.horaFin2)) {
            return true;
        }
        
        // Verificar cruce en día 2 de c1 con ambos días de c2
        if (seCruzanEnDia(c1.dia2, c1.horaInicio2, c1.horaFin2, 
                          c2.dia1, c2.horaInicio1, c2.horaFin1)) {
            return true;
        }
        if (seCruzanEnDia(c1.dia2, c1.horaInicio2, c1.horaFin2, 
                          c2.dia2, c2.horaInicio2, c2.horaFin2)) {
            return true;
        }
        
        return false;
    }
    
    private boolean seCruzanEnDia(String dia1, String inicio1, String fin1,
                                  String dia2, String inicio2, String fin2) {
        // Si no son el mismo día, no hay cruce
        if (!dia1.equalsIgnoreCase(dia2)) {
            return false;
        }
        
        // Convertir a minutos para facilitar comparación
        int minInicio1 = convertirAMinutos(inicio1);
        int minFin1 = convertirAMinutos(fin1);
        int minInicio2 = convertirAMinutos(inicio2);
        int minFin2 = convertirAMinutos(fin2);
        
        // Verificar si hay cruce de horarios
        return (minInicio1 < minFin2 && minFin1 > minInicio2);
    }
    
    private int convertirAMinutos(String hora) {
        String[] partes = hora.split(":");
        int horas = Integer.parseInt(partes[0]);
        int minutos = Integer.parseInt(partes[1]);
        return horas * 60 + minutos;
    }
    
    private void enviarRespuesta(ACLMessage msgOriginal, String contenido) {
        ACLMessage reply = msgOriginal.createReply();
        reply.setContent(contenido);
        agent.send(reply);
    }
    public void consultarHorario(ACLMessage msg) {
        NotificationLogger.mensaje("Solicitante", "UnidadMatricula", 
            "Solicitud de HORARIO recibida");

        try {
            System.out.print("\nIngrese su código de estudiante: ");
            int codigo = scanner.nextInt();
            scanner.nextLine();

            // 1. Verificar que el estudiante existe
            Estudiante estudiante = obtenerEstudiante(codigo);

            if (estudiante == null) {
                NotificationLogger.error("UnidadMatricula", 
                    "Estudiante no encontrado con código " + codigo);
                enviarRespuesta(msg, "Estudiante no encontrado con código " + codigo);
                return;
            }

            // 2. Buscar cursos matriculados con horario
            List<CursoDisponible> cursosMatriculados = obtenerCursosMatriculados(estudiante.id);

            if (cursosMatriculados.isEmpty()) {
                NotificationLogger.error("UnidadMatricula", 
                    "Estudiante " + codigo + " no tiene cursos matriculados");
                enviarRespuesta(msg, "Estudiante " + codigo + " no tiene cursos matriculados");
                return;
            }

            // 3. Mostrar horario en consola del agente
            mostrarHorarioMatriculado(codigo, cursosMatriculados);

            // 4. Construir respuesta para Solicitante
            String respuesta = construirRespuestaHorario(codigo, cursosMatriculados);
            enviarRespuesta(msg, respuesta);

        } catch (Exception e) {
            NotificationLogger.error("UnidadMatricula", 
                "Error al consultar horario: " + e.getMessage());
            e.printStackTrace();
            enviarRespuesta(msg, "Error al consultar horario");
        }
    }

    private List<CursoDisponible> obtenerCursosMatriculados(int idEstudiante) {
        List<CursoDisponible> cursos = new ArrayList<>();

        String sql =
            "SELECT c.ID_Curso, c.Codigo_Curso, c.Nombre_Curso, c.Ciclo, " +
            "       h.Dia_Semana_1, h.Hora_Inicio_1, h.Hora_Fin_1, " +
            "       h.Dia_Semana_2, h.Hora_Inicio_2, h.Hora_Fin_2 " +
            "FROM Matricula m " +
            "INNER JOIN Curso c         ON m.ID_Curso          = c.ID_Curso " +
            "INNER JOIN Horario_Curso h ON c.ID_Horario_Curso  = h.ID_Horario_Curso " +
            "WHERE m.ID_Estudiante = ?";

        try (Connection conn = SQLiteManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEstudiante);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                CursoDisponible curso = new CursoDisponible();
                curso.idCurso       = rs.getInt("ID_Curso");
                curso.codigoCurso   = rs.getString("Codigo_Curso");
                curso.nombreCurso   = rs.getString("Nombre_Curso");
                curso.ciclo         = rs.getString("Ciclo");
                curso.creditos      = obtenerCreditosPorCodigo(curso.codigoCurso);
                curso.dia1          = rs.getString("Dia_Semana_1");
                curso.horaInicio1   = rs.getString("Hora_Inicio_1");
                curso.horaFin1      = rs.getString("Hora_Fin_1");
                curso.dia2          = rs.getString("Dia_Semana_2");
                curso.horaInicio2   = rs.getString("Hora_Inicio_2");
                curso.horaFin2      = rs.getString("Hora_Fin_2");
                cursos.add(curso);
            }

        } catch (SQLException e) {
            System.out.println("Error al consultar cursos matriculados: " + e.getMessage());
        }

        return cursos;
    }

    private void mostrarHorarioMatriculado(int codigo, List<CursoDisponible> cursos) {
        double totalCreditos = calcularTotalCreditos(cursos);
        NotificationLogger.listo("UnidadMatricula", 
            "Horario consultado - Estudiante: " + codigo + 
            " | Cursos: " + cursos.size() + " | Créditos: " + totalCreditos);
    }

    private String construirRespuestaHorario(int codigo, List<CursoDisponible> cursos) {
        double totalCreditos = calcularTotalCreditos(cursos);
        StringBuilder sb = new StringBuilder();

        sb.append("HORARIO MATRICULADO - Estudiante ").append(codigo).append("\n");
        sb.append("Cursos: ").append(cursos.size())
          .append(" | Créditos: ").append(totalCreditos).append("\n");
        sb.append("───────────────────────────────────────\n");

        for (int i = 0; i < cursos.size(); i++) {
            CursoDisponible c = cursos.get(i);
            sb.append(i + 1).append(". ").append(c.nombreCurso)
              .append(" (").append(c.codigoCurso).append(")\n");
            sb.append("   Créditos: ").append(c.creditos).append("\n");
            sb.append("   ").append(c.dia1).append(" ").append(c.horaInicio1)
              .append(" - ").append(c.horaFin1).append("\n");
            sb.append("   ").append(c.dia2).append(" ").append(c.horaInicio2)
              .append(" - ").append(c.horaFin2).append("\n");
        }

        return sb.toString();
    }

}
