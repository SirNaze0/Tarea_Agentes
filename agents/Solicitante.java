import jade.core.Agent;
import jade.core.behaviours.*;
import java.util.Scanner;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
/*
  Ingresar api key de gemini en GeminiClient.java
  BD: 
  rm data/escuela.db
  $ java -cp ".;sqlite-jdbc-3.51.1.0.jar" db.InitDatabase
  $ java -cp "out;sqlite-jdbc-3.51.1.0.jar" db.ConsultarHorarios --estudiantes
  $ java -cp "out;sqlite-jdbc-3.51.1.0.jar" db.ConsultarHorarios
  $ java -cp "out;sqlite-jdbc-3.51.1.0.jar" db.ConsultarHorarios --todas
  Compilar: javac -encoding UTF-8 -d out -cp ".;jade.jar;gson-2.10.1.jar;pdfbox-app-2.0.35.jar;sqlite-jdbc-3.51.1.0.jar" agents/*.java db/*.java
  Ejecutar: java -cp ".;out;jade.jar;gson-2.10.1.jar;pdfbox-app-2.0.35.jar;sqlite-jdbc-3.51.1.0.jar" jade.Boot -gui -agents "escuela:EscuelaSistemas;solicitante:Solicitante;matricula:UnidadMatricula;sanmarket:SanMarket;economia:UnidadEconomia"
  
*/
public class Solicitante extends Agent {

    private volatile String ultimoMensaje = "";
    private volatile boolean mensajeRecibido = false;
    private volatile boolean primeraVez = true;
    private java.util.List<String> notificacionesLocales = new java.util.ArrayList<>();

    protected void setup() {
        System.out.println("=== Agente Solicitante Iniciado ===");
        
        registrarServicio();
        
        // Comportamiento para recibir mensajes
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String agenteOrigen = msg.getSender().getLocalName();
                    String contenido = msg.getContent();
                    
                    // Distinguir entre respuesta normal y notificación de prematrícula
                    if ("notificacion-prematricula".equals(msg.getConversationId())) {
                        // Notificación asíncrona de UnidadMatricula
                        notificacionesLocales.add("[MSJ] " + agenteOrigen + " → "+ getLocalName() + contenido);
                        NotificationLogger.mensaje(agenteOrigen, "Solicitante", 
                            "Notificación de PREMATRÍCULA recibida");
                    } else {
                        // Respuesta normal de otros agentes
                        ultimoMensaje = "AGENTE: " + agenteOrigen + " → " + contenido;
                        notificacionesLocales.add("[MSJ] " + agenteOrigen + " → "+ getLocalName() + contenido);
                        mensajeRecibido = true;
                    }
                } else {
                    block();
                }
            }
        });
        
        // Hilo principal de la consola
        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (true) {
                if (primeraVez) {
                    primeraVez = false;
                    mostrarMenu(sc);
                    continue;
                }
                
                while (!mensajeRecibido) {
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                }
                
                mensajeRecibido = false;
                System.out.println("\nMensaje recibido:\n" + ultimoMensaje);
                mostrarMenu(sc);
            }
        }).start();
    }
    
    //Registrar servicio "estudiante"
    private void registrarServicio() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        
        ServiceDescription sd = new ServiceDescription();
        sd.setType("estudiante");
        sd.setName(getLocalName() + "-estudiante");
        
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
            System.out.println("Servicio 'estudiante' registrado en DF");
        } catch (FIPAException e) {
            System.err.println("Error al registrar servicio:");
            e.printStackTrace();
        }
    }
    
    // Cleanup al cerrar
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("Agente Solicitante finalizado");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void mostrarMenu(Scanner sc) {
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Limpiar consola
            System.out.print("\033[H\033[2J");
            System.out.flush();
            if (!ultimoMensaje.isEmpty()) {
                System.out.println("Último mensaje recibido: " + ultimoMensaje);
            }
            System.out.println("===== MENU =====");
            System.out.println("1. Solicitar curso");
            System.out.println("2. Pagar matrícula");
            System.out.println("3. Validar pago");
            System.out.println("4. Matricularse ciclo verano");
            System.out.println("5. Consultar horario matriculado");
            System.out.println("6. Ver Notificaciones");
            System.out.println("0. Salir");
            System.out.println("================");
            System.out.print("Seleccione una opción: ");

            String opcion;
            try {
                opcion = sc.nextLine();
            } catch (java.util.NoSuchElementException e) {
                System.out.println("\nInterrupción detectada. Saliendo del menú...");
                doDelete();
                System.exit(0);
                return;
            }
            
            switch (opcion) {
                case "6":
                    // Ver notificaciones
                    mostrarNotificaciones();
                    System.out.print("\nPresione Enter para continuar...");
                    System.out.flush();
                    try {
                        // Esperar Enter - leer la línea
                        sc.nextLine();
                    } catch (Exception e) {
                        // Si hay error, simplemente continuar
                    }
                    // Volver al inicio del while para mostrar el menú de nuevo
                    continue;
                    
                case "5":
                    // Consultar horario matriculado
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("Consultando horario...");

                    ServiceDescription sd5 = new ServiceDescription();
                    sd5.setType("matricula");
                    buscar(sd5, "HORARIO|");

                    while (!mensajeRecibido) {
                        try { Thread.sleep(500); } catch (InterruptedException e) {}
                    }

                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("HORARIO CONSULTADO\n");
                    System.out.println(ultimoMensaje.replace("AGENTE: escuela → ", ""));
                    System.out.println("\nPresione Enter para continuar...");
                    try {
                        sc.nextLine();
                    } catch (Exception ex) {}
                    return;

                case "4":
                    //Llamar a UnidadMatricula para matricularse
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("Procesando matrícula...");
                    
                    ServiceDescription sd4 = new ServiceDescription();
                    sd4.setType("matricula");
                    buscar(sd4, "MATRICULA|");

                    while (!mensajeRecibido) {
                        try { Thread.sleep(500); } catch (InterruptedException e) {}
                    }

                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("RESULTADO DE MATRÍCULA\n");
                    System.out.println(ultimoMensaje.replace("AGENTE: matricula → ", ""));
                    System.out.println("\nPresione Enter para continuar...");
                    try {
                        sc.nextLine();
                    } catch (Exception ex) {}
                    return;
                case "3":
                    // Llamar a UnidadEconomica para validar pago y habilitar matrícula
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("Validando pagos y habilitando matrícula...");
                    
                    ServiceDescription sd3 = new ServiceDescription();
                    sd3.setType("habilitar matricula");
                    buscar(sd3, "habilitar");

                    while (!mensajeRecibido) {
                        try { Thread.sleep(500); } catch (InterruptedException e) {}
                    }

                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("RESULTADO DE VALIDACIÓN\n");
                    System.out.println(ultimoMensaje.replace("AGENTE: economia → ", ""));
                    System.out.println("\nPresione Enter para continuar...");
                    try {
                        sc.nextLine();
                    } catch (Exception ex) {}
                    return;
                case "2":
                    // Llamar a SanMarket para pagar
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("Procesando pago...");
                    
                    ServiceDescription sd2 = new ServiceDescription();
                    sd2.setType("pagar curso");
                    buscar(sd2, "pagar");

                    while (!mensajeRecibido) {
                        try { Thread.sleep(500); } catch (InterruptedException e) {}
                    }

                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("RESULTADO DE PAGO\n");
                    System.out.println(ultimoMensaje.replace("AGENTE: sanmarket → ", ""));
                    System.out.println("\nPresione Enter para continuar...");
                    try {
                        sc.nextLine();
                    } catch (Exception ex) {}
                    return;
                case "1":
                    String downloadLink = solicitar(sc);
                    if (downloadLink == null) break;

                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("Procesando solicitud de curso...");

                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("solicitar curso");
                    buscar(sd, "solicitar|" + downloadLink);

                    while (ultimoMensaje.isEmpty()) {
                        try { Thread.sleep(500); } catch (InterruptedException e) {}
                    }

                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("RESULTADO DE SOLICITUD\n");
                    System.out.println(ultimoMensaje.replace("AGENTE: escuela → ", ""));
                    System.out.println("\nPresione Enter para continuar...");
                    try {
                        sc.nextLine();
                    } catch (Exception ex) {}
                    return;

                case "0":
                    System.out.println("Saliendo...");
                    doDelete();
                    System.exit(0);
                    return;

                default:
                    System.out.println("Opción inválida. Intente de nuevo.");
                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
            }
        }
    }

    private String solicitar(Scanner sc) {
        System.out.print("Ingrese el link de Google Docs: ");
        String link = sc.nextLine();
        if (!link.contains("docs.google.com")) {
            System.out.println("Link inválido. Operación cancelada.");
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            return null;
        }
        return link.replace("/edit?usp=sharing", "/export?format=pdf");
    }

    private void buscarServicio(String tipoServicio, String pedido) {
        ServiceDescription sd = new ServiceDescription();
        sd.setType(tipoServicio);
        buscar(sd, pedido);

        for (int i = 3; i > 0; i--) {
            System.out.print("\rRefrescando en " + i + "...   ");
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
        }
        System.out.println("\nÚltimo mensaje recibido: \n" + ultimoMensaje);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
    }

    private void buscar(final ServiceDescription sd, final String pedido) {
        addBehaviour(new TickerBehaviour(this, 1000) {
            protected void onTick() {
                DFAgentDescription dfd = new DFAgentDescription();
                dfd.addServices(sd);
                try {
                    DFAgentDescription[] resultado = DFService.search(myAgent, dfd);
                    if (resultado.length != 0) {
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(resultado[0].getName());
                        msg.setContent(pedido);
                        myAgent.send(msg);
                        stop();
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void mostrarNotificaciones() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("                    NOTIFICACIONES");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        // Mostrar notificaciones del sistema global
        java.util.List<String> notificacionesGlobales = NotificationLogger.obtenerNotificaciones();
        
        if (notificacionesGlobales.isEmpty() && notificacionesLocales.isEmpty()) {
            System.out.println("\nNo hay notificaciones disponibles.\n");
        } else {
            System.out.println("\n--- Notificaciones del Sistema ---\n");
            for (String notif : notificacionesGlobales) {
                System.out.println(notif);
            }
            
            if (!notificacionesLocales.isEmpty()) {
                System.out.println("\n--- Notificaciones Locales ---\n");
                for (String notif : notificacionesLocales) {
                    System.out.println(notif);
                }
            }
        }
        
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("Total: " + (notificacionesGlobales.size() + notificacionesLocales.size()) + " notificaciones");
    }
}