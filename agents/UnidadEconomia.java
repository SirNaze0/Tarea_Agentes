import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

import java.sql.*;
import java.util.Scanner;
import db.SQLiteManager;

public class UnidadEconomia extends Agent {
    
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado - Unidad Económica");
        
        // Descripción del servicio
        ServiceDescription servicio = new ServiceDescription();
        servicio.setType("habilitar matricula");
        servicio.setName(this.getLocalName());
        
        registrarServicio(servicio);
        
        // Comportamiento principal para habilitar matrícula
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    if (msg.getContent().equalsIgnoreCase("habilitar")) {
                        NotificationLogger.mensaje(msg.getSender().getLocalName(), "UnidadEconomia", 
                            "Solicitud de habilitación recibida");
                        procesarHabilitacion();
                        
                        // Responder al solicitante
                        ACLMessage reply = msg.createReply();
                        reply.setContent("Proceso de habilitación completado");
                        myAgent.send(reply);
                    }
                } else {
                    block();
                }
            }
        });
    }
    
    // Método para registrar un servicio
    protected void registrarServicio(ServiceDescription sd) {
        DFServiceUtils.registrarServicio(this, sd);
    }
    
    // Método principal para procesar la habilitación
    private void procesarHabilitacion() {
        Scanner scanner = new Scanner(System.in);
        
        try {
            // 1. Solicitar código del estudiante
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("      HABILITACIÓN DE MATRÍCULA");
            System.out.println("═══════════════════════════════════════");
            
            System.out.print("Ingrese su código de estudiante: ");
            int codigo = scanner.nextInt();
            
            // 2. Obtener ID del estudiante
            Integer idEstudiante = obtenerIdEstudiante(codigo);
            
            if (idEstudiante == null) {
                NotificationLogger.error("UnidadEconomia", 
                    "Estudiante no encontrado con código " + codigo);
                return;
            }
            
            // 3. Contar pagos del estudiante
            int cantidadPagos = contarPagos(idEstudiante);
            
            // 4. Verificar que tenga al menos 1 pago y máximo 4
            if (cantidadPagos < 1) {
                NotificationLogger.error("UnidadEconomia", 
                    "Estudiante " + codigo + " necesita al menos 1 pago (tiene " + cantidadPagos + ")");
                return;
            }
            
            if (cantidadPagos > 4) {
                NotificationLogger.mensaje("UnidadEconomia", "Sistema", 
                    "Estudiante " + codigo + " tiene " + cantidadPagos + 
                    " pagos, se habilitarán solo 4");
                cantidadPagos = 4;
            }
            
            // 5. Actualizar Cantidad_Habilitada
            boolean actualizado = actualizarCantidadHabilitada(idEstudiante, cantidadPagos);
            
            // 6. Mensaje de confirmación
            if (actualizado) {
                NotificationLogger.listo("UnidadEconomia", 
                    "Matrícula habilitada - Estudiante: " + codigo + 
                    " | Cursos habilitados: " + cantidadPagos);
            } else {
                NotificationLogger.error("UnidadEconomia", 
                    "No se pudo actualizar la habilitación del estudiante " + codigo);
            }
            
        } catch (Exception e) {
            NotificationLogger.error("UnidadEconomia", 
                "Error al procesar habilitación: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Obtener ID del estudiante desde la BD
    private Integer obtenerIdEstudiante(int codigo) {
        return EstudianteDBUtils.obtenerIdEstudiante(codigo);
    }
    
    // Contar pagos del estudiante
    private int contarPagos(int idEstudiante) {
        return EstudianteDBUtils.contarPagos(idEstudiante);
    }
    
    // Actualizar Cantidad_Habilitada del estudiante
    private boolean actualizarCantidadHabilitada(int idEstudiante, int cantidadPagos) {
        return EstudianteDBUtils.actualizarCantidadHabilitada(idEstudiante, cantidadPagos);
    }
    
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("Agente " + getLocalName() + " finalizado");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}