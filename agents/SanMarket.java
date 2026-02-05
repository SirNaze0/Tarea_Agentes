import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import java.sql.*;
import java.util.Scanner;
import db.SQLiteManager;

public class SanMarket extends Agent {
    
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado - Sistema de Pagos");
        
        // Descripción del servicio
        ServiceDescription servicio = new ServiceDescription();
        servicio.setType("pagar curso");
        servicio.setName(this.getLocalName());
        
        registrarServicio(servicio);
        
        // Comportamiento principal para procesar pagos
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    if (msg.getContent().equalsIgnoreCase("pagar")) {
                        NotificationLogger.mensaje(msg.getSender().getLocalName(), "SanMarket", 
                            "Solicitud de pago recibida");
                        procesarPago();
                        
                        // Responder al solicitante
                        ACLMessage reply = msg.createReply();
                        reply.setContent("Pago procesado correctamente");
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
    
    // Método principal para procesar el pago
    private void procesarPago() {
        Scanner scanner = new Scanner(System.in);
        
        try {
            // 1. Solicitar datos del estudiante
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("         SISTEMA DE PAGOS");
            System.out.println("═══════════════════════════════════════");
            
            System.out.print("Ingrese su código de estudiante: ");
            int codigo = scanner.nextInt();
            
            // 2. Verificar si el estudiante existe
            Integer idEstudiante = obtenerIdEstudiante(codigo);
            
            if (idEstudiante == null) {
                NotificationLogger.error("SanMarket", 
                    "Estudiante no encontrado con código " + codigo);
                return;
            }
            
            // 3. Solicitar cantidad de cursos
            System.out.println("\n¿Cuántos cursos desea pagar? (máximo 4)");
            System.out.print("Cantidad: ");
            int cantidadCursos = scanner.nextInt();
            
            if (cantidadCursos < 1 || cantidadCursos > 4) {
                NotificationLogger.error("SanMarket", 
                    "Cantidad de cursos inválida: " + cantidadCursos);
                return;
            }
            
            // 4. Pantalla de carga
            System.out.println("\n⏳ Procesando pago...");
            mostrarBarraCarga(2000); // 2 segundos
            
            // 5. Crear los pagos en la BD
            int pagosCreados = crearPagos(idEstudiante, cantidadCursos);
            
            // 6. Mensaje de confirmación
            if (pagosCreados > 0) {
                NotificationLogger.listo("SanMarket", 
                    "Pago procesado - Estudiante: " + codigo + 
                    " | Cursos: " + pagosCreados);
            } else {
                NotificationLogger.error("SanMarket", 
                    "No se pudieron crear los pagos para estudiante " + codigo);
            }
            
        } catch (Exception e) {
            NotificationLogger.error("SanMarket", 
                "Error al procesar pago: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Obtener ID del estudiante desde la BD
    private Integer obtenerIdEstudiante(int codigo) {
        return EstudianteDBUtils.obtenerIdEstudiante(codigo);
    }
    
    // Crear los pagos en la BD
    private int crearPagos(int idEstudiante, int cantidadCursos) {
        String sql = "INSERT INTO Pago (ID_Estudiante) VALUES (?)";
        int pagosCreados = 0;
        
        try (Connection conn = SQLiteManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Crear múltiples pagos
            for (int i = 0; i < cantidadCursos; i++) {
                pstmt.setInt(1, idEstudiante);
                pstmt.executeUpdate();
                pagosCreados++;
            }
            
        } catch (SQLException e) {
            System.out.println("Error al crear pagos: " + e.getMessage());
        }
        
        return pagosCreados;
    }
    
    // Mostrar barra de carga animada
    private void mostrarBarraCarga(int duracionMs) {
        try {
            int pasos = 20;
            int tiempoPorPaso = duracionMs / pasos;
            
            System.out.print("[");
            for (int i = 0; i < pasos; i++) {
                Thread.sleep(tiempoPorPaso);
                System.out.print("█");
            }
            System.out.println("] 100%");
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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