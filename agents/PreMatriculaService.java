import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.core.AID;
import com.google.gson.Gson;
import java.sql.*;
import java.util.Optional;

public class PreMatriculaService {
    
    private Agent agent;
    private Gson gson;
    private DatabaseHelper dbHelper;
    
    public PreMatriculaService(Agent agent) {
        this.agent = agent;
        this.gson = new Gson();
        this.dbHelper = new DatabaseHelper();
    }
    
    public void procesar(ACLMessage msg) {
        try {
            SolicitudCursoDTO solicitud = gson.fromJson(msg.getContent(), SolicitudCursoDTO.class);
            
            // 1. VALIDAR CURSO EN PLAN 2018
            Optional<Plan2018.Curso> cursoOpt = Plan2018.Curso.buscarPorCodigo(solicitud.curso_codigo);
            
            if (!cursoOpt.isPresent()) {
                NotificationLogger.error("UnidadMatricula", 
                    "Curso no encontrado en Plan 2018: " + solicitud.curso_codigo);
                notificarASolicitante(solicitud.delegado.codigo, 
                    "PREMATRICULA RECHAZADA: Curso no encontrado en Plan 2018");
                return;
            }
            
            Plan2018.Curso curso = cursoOpt.get();
            
            // 2. PARSEAR HORARIO
            HorarioParser.HorarioDual horario = HorarioParser.parsear(solicitud.horario_propuesto);
            
            if (horario == null) {
                NotificationLogger.error("UnidadMatricula", 
                    "Formato de horario inválido: " + solicitud.horario_propuesto);
                notificarASolicitante(solicitud.delegado.codigo,
                    "PREMATRICULA RECHAZADA: Formato de horario inválido\n" +
                    "Horario recibido: " + solicitud.horario_propuesto);
                return;
            }
            
            // 3. REGISTRAR EN BASE DE DATOS
            int resultado = dbHelper.registrarCursoCompleto(solicitud, curso, horario);
            
            // 4. PROCESAR RESULTADO
            procesarResultado(resultado, solicitud, curso, horario);
            
        } catch (Exception e) {
            NotificationLogger.error("UnidadMatricula", 
                "Error al procesar prematricula: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void procesarResultado(int resultado, SolicitudCursoDTO solicitud, 
                                   Plan2018.Curso curso, HorarioParser.HorarioDual horario) {
        
        if (resultado > 0) {
            // ÉXITO
            NotificationLogger.listo("UnidadMatricula", 
                "PREMATRICULA registrada - Curso: " + curso.codigo + 
                " | ID: " + resultado);
            
            notificarASolicitante(solicitud.delegado.codigo,
                "PREMATRICULA REGISTRADA EXITOSAMENTE\n\n" +
                "Curso: " + curso.nombre + " (" + curso.codigo + ")\n" +
                "Ciclo: " + curso.ciclo + " | Creditos: " + curso.creditos + "\n" +
                "Docente: " + solicitud.docente.nombre + "\n" +
                "Horario:\n" +
                "  " + horario.diaSemana1 + " " + horario.horaInicio1 + "-" + horario.horaFin1 + "\n" +
                "  " + horario.diaSemana2 + " " + horario.horaInicio2 + "-" + horario.horaFin2 + "\n" +
                "Alumnos: " + solicitud.numero_alumnos_detectados);
                
        } else if (resultado == -2) {
            // LÍMITE DE CURSOS DEL PROFESOR
            NotificationLogger.error("UnidadMatricula", 
                "Profesor con límite de cursos alcanzado: " + solicitud.docente.nombre);
            notificarASolicitante(solicitud.delegado.codigo,
                "PREMATRICULA RECHAZADA\n\n" +
                "Razón: El profesor ya tiene asignados 2 cursos (límite máximo).\n\n" +
                "Docente: " + solicitud.docente.nombre);
                
        } else if (resultado == -3) {
            // LÍMITE DE SECCIONES DEL CURSO
            NotificationLogger.error("UnidadMatricula", 
                "Curso con límite de secciones alcanzado: " + curso.nombre);
            notificarASolicitante(solicitud.delegado.codigo,
                "PREMATRICULA RECHAZADA\n\n" +
                "Razón: El curso '" + curso.nombre + "' ya tiene 2 secciones registradas (límite máximo).\n\n" +
                "Curso: " + curso.nombre + " (" + curso.codigo + ")\n" +
                "Ciclo: " + curso.ciclo);
                
        } else {
            // PROFESOR NO REGISTRADO
            NotificationLogger.error("UnidadMatricula", 
                "Docente no registrado: " + solicitud.docente.nombre);
            notificarASolicitante(solicitud.delegado.codigo,
                "PREMATRICULA RECHAZADA\n\n" +
                "Razón: El docente '" + solicitud.docente.nombre + 
                "' no está registrado como Profesor Nombrado.\n\n" +
                "El docente debe estar en la base de datos de profesores nombrados.");
        }
    }
    
    private void notificarASolicitante(String codigoSolicitante, String mensaje) {
        ACLMessage notificacion = new ACLMessage(ACLMessage.INFORM);
        notificacion.setContent(mensaje);
        notificacion.setConversationId("notificacion-prematricula");
        
        if (DFServiceUtils.buscarYEnviar(agent, "estudiante", notificacion)) {
            NotificationLogger.mensaje(agent.getLocalName(), "Solicitante", 
                "Notificación enviada a estudiante " + codigoSolicitante);
        } else {
            NotificationLogger.error("UnidadMatricula", 
                "No se encontró al agente Solicitante");
        }
    }
}