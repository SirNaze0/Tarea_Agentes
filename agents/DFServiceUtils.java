import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.core.AID;

/**
 * Utilidades para operaciones con el Directory Facilitator (DF)
 */
public class DFServiceUtils {
    
    /**
     * Registra un servicio en el DF
     */
    public static void registrarServicio(Agent agent, ServiceDescription servicio) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(agent.getAID());
        dfd.addServices(servicio);
        
        try {
            DFService.register(agent, dfd);
            System.out.println("Servicio '" + servicio.getType() + "' registrado");
        } catch (FIPAException e) {
            System.err.println("Error al registrar servicio:");
            e.printStackTrace();
        }
    }
    
    /**
     * Busca un servicio en el DF y devuelve el primer agente encontrado
     */
    public static AID buscarServicio(Agent agent, String tipoServicio) throws FIPAException {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(tipoServicio);
        template.addServices(sd);
        
        DFAgentDescription[] result = DFService.search(agent, template);
        
        if (result.length == 0) {
            return null;
        }
        
        return result[0].getName();
    }
    
    /**
     * Busca un servicio y envía un mensaje al primer agente encontrado
     */
    public static boolean buscarYEnviar(Agent agent, String tipoServicio, 
                                         jade.lang.acl.ACLMessage mensaje) {
        try {
            AID receptorAID = buscarServicio(agent, tipoServicio);
            
            if (receptorAID == null) {
                System.err.println("No se encontró el servicio '" + tipoServicio + "'");
                return false;
            }
            
            mensaje.addReceiver(receptorAID);
            agent.send(mensaje);
            System.out.println("Mensaje enviado a " + receptorAID.getLocalName());
            return true;
            
        } catch (FIPAException e) {
            System.err.println("Error al buscar servicio '" + tipoServicio + "':");
            e.printStackTrace();
            return false;
        }
    }
}
