import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

public class UnidadMatricula extends Agent {
    
    private PreMatriculaService preMatriculaService;
    private MatriculaService matriculaService;
    
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado - Unidad de Matrícula");
        
        // Inicializar servicios
        preMatriculaService = new PreMatriculaService(this);
        matriculaService = new MatriculaService(this);
        
        // Registrar servicios en DF
        registrarServicios();
        
        // Iniciar comportamiento de escucha
        recibirMensajes();
    }
    
    private void registrarServicios() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        
        // Servicio 1: prematricula (lo invoca Escuela)
        ServiceDescription pre = new ServiceDescription();
        pre.setType("prematricula");
        pre.setName(this.getLocalName() + "-prematricula");
        dfd.addServices(pre);
        
        // Servicio 2: matricula (lo invoca Solicitante)
        ServiceDescription mat = new ServiceDescription();
        mat.setType("matricula");
        mat.setName(this.getLocalName() + "-matricula");
        dfd.addServices(mat);
        
        try {
            DFService.register(this, dfd);
            System.out.println("Servicios 'prematricula' y 'matricula' registrados");
        } catch (FIPAException e) {
            System.err.println("Error al registrar servicios:");
            e.printStackTrace();
        }
    }
    
    protected void recibirMensajes() {
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String contenido = msg.getContent();
                    String agenteOrigen = msg.getSender().getLocalName();
                    
                    // Determinar tipo de mensaje
                    if (contenido != null && contenido.startsWith("HORARIO|")) {
                        NotificationLogger.mensaje(agenteOrigen, getLocalName(), 
                            "CONSULTA_HORARIO recibida");
                        matriculaService.consultarHorario(msg);
                    } else if (contenido != null && contenido.startsWith("MATRICULA|")) {
                        NotificationLogger.mensaje(agenteOrigen, getLocalName(), 
                            "Solicitud de MATRICULA recibida");
                        matriculaService.procesar(msg);
                    } else if (msg.getPerformative() == ACLMessage.REQUEST) {
                        NotificationLogger.mensaje(agenteOrigen, getLocalName(), 
                            "PREMATRICULA recibida");
                        preMatriculaService.procesar(msg);
                    } else {
                        NotificationLogger.error("UnidadMatricula", 
                            "Tipo de mensaje desconocido de " + agenteOrigen);
                    }

                } else {
                    block();
                }
            }
        });
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
