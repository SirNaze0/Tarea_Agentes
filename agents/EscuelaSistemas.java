import static java.lang.Thread.sleep;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.net.URL;
import jade.core.AID;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;



public class EscuelaSistemas extends Agent {
    
    protected void setup() {
        // Descripción del servicio
        ServiceDescription servicio = new ServiceDescription();
        // Su servicio es solicitar curso
        servicio.setType("solicitar curso");
        servicio.setName(this.getLocalName());
        
        registrarServicio(servicio);
        recibirMensajes("solicitar");
    }

    // Método para registrar un servicio
    protected void registrarServicio(ServiceDescription sd) {
        DFServiceUtils.registrarServicio(this, sd);
    }
    //Método para procesar la Solicitud de apertura del curso
    private String procesarSolicitud(String pdfUrl) {
        try {
            String rutaPdf = PDFUtils.descargarPdf(pdfUrl);
            String textoCompleto = PDFUtils.extraerTextoCompleto(rutaPdf);
            String textoFirmas = PDFUtils.extraerTextoDesdePagina(rutaPdf, 2);
            int alumnosRegex = PDFUtils.contarFirmantesPorCodigo(textoFirmas);

            String textoParaIA = prepararTextoParaIA(textoCompleto);
            String textoFirmasPreparado = prepararTextoFirmas(textoFirmas);
            String prompt = construirPrompt(textoParaIA + "\n\n" + textoFirmasPreparado);

            GeminiClient gemini = new GeminiClient();
            String respuestaIA = gemini.llamarIA(prompt);
            if (respuestaIA == null) return "Error al procesar la solicitud";

            String jsonExtraido = gemini.extraerJsonDeRespuesta(respuestaIA);
            SolicitudCursoDTO solicitud = parsearSolicitud(jsonExtraido);

            int alumnosIA = Math.max(0, solicitud.numero_alumnos_detectados);
            solicitud.numero_alumnos_detectados = SolicitudValidator.aplicarEstrategiaConservadora(alumnosRegex, alumnosIA);

            if (Math.abs(alumnosRegex - alumnosIA) >= 5) {
                System.out.println("ADVERTENCIA: Discrepancia IA vs Regex | IA=" + alumnosIA + " Regex=" + alumnosRegex);
            }

            //System.out.println("Alumnos (regex): " + alumnosRegex);
            //System.out.println("Alumnos (IA): " + alumnosIA);
            //System.out.println("Alumnos (final): " + solicitud.numero_alumnos_detectados);

            String resultado = SolicitudValidator.validarSolicitud(solicitud);

            if ("OK".equals(resultado)) {
                NotificationLogger.listo("EscuelaSistemas", 
                    "SOLICITUD APROBADA - Alumnos: " + solicitud.numero_alumnos_detectados);
                enviarAMatricula(solicitud);
                return "\nAlumnos (final): " + solicitud.numero_alumnos_detectados +
                       "\nSOLICITUD APROBADA";
            } else {
                NotificationLogger.error("EscuelaSistemas", 
                    "SOLICITUD OBSERVADA - " + resultado);
                return "\nAlumnos (final): " + solicitud.numero_alumnos_detectados +
                       "\nSOLICITUD OBSERVADA\n" +
                       resultado;
            }


        } catch (Exception e) {
            e.printStackTrace();
            return "Error al procesar la solicitud";
        }
    }


    private String prepararTextoParaIA(String textoCompleto) {
        // Limitar tamaño
        String texto = textoCompleto.replaceAll("\\s+", " ").trim();
        int maxChars = 8000;
        if (texto.length() > maxChars) {
            texto = texto.substring(0, maxChars);
        }

        // Contexto explícito (SIN tildes)
        StringBuilder sb = new StringBuilder();
        sb.append("DOCUMENTO: Solicitud de Apertura de Curso de Verano\n");
        sb.append("ESCUELA: Ingenieria de Sistemas\n\n");
        sb.append("CONTENIDO:\n");
        sb.append(texto);

        return sb.toString();
    }
    private String prepararTextoFirmas(String textoFirmas) {
        if (textoFirmas == null) return "";

        String texto = textoFirmas.replaceAll("\\s+", " ").trim();

        return "SECCIÓN FIRMAS (desde página 2):\n" + texto;
    }

    private SolicitudCursoDTO parsearSolicitud(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, SolicitudCursoDTO.class);
    }

    private String construirPrompt(String textoParaIA) {
        return
            "Eres un sistema de extraccion de informacion academica.\n"
          + "Tu tarea es analizar una SOLICITUD DE APERTURA DE CURSO DE VERANO.\n\n"
          + "INSTRUCCIONES:\n"
          + "- NO interpretes ni evalues.\n"
          + "- NO inventes informacion.\n"
          + "- Si un dato no aparece, usa null.\n"
          + "- Devuelve SOLO JSON valido.\n"
          + "- El texto NO tiene tildes (a, e, i, o, u en lugar de á, é, í, ó, ú).\n\n"
          + "FORMATO JSON:\n"
          + "{\n"
          + "  \"curso_nombre\": \"\",\n"
          + "  \"curso_codigo\": \"\",\n"
          + "  \"plan_estudios\": \"\",\n"
          + "  \"escuela_profesional\": \"\",\n"
          + "  \"tipo_curso\": \"\",\n"
          + "  \"anio_curso\": \"\",\n"
          + "  \"horario_propuesto\": \"\",\n"
          + "  \"docente\": {\n"
          + "    \"nombre\": \"\",\n"
          + "    \"correo\": \"\",\n"
          + "    \"celular\": \"\"\n"
          + "  },\n"
          + "  \"delegado\": {\n"
          + "    \"nombre\": \"\",\n"
          + "    \"codigo\": \"\",\n"
          + "    \"correo\": \"\",\n"
          + "    \"celular\": \"\"\n"
          + "  },\n"
          + "  \"fecha_documento\": \"\",\n"
          + "  \"numero_alumnos_detectados\": 0,\n"
          + "  \"lista_alumnos_detectados\": []\n"
          + "}\n\n"
          + "TEXTO DEL DOCUMENTO:\n"
          + textoParaIA;
    }


    // Método para añadir un comportamiento que recibe mensajes
    protected void recibirMensajes(final String comando) {
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent();

                    if (content.startsWith(comando + "|")) {
                        String link = content.split("\\|")[1];

                        System.out.println("Solicitud recibida");
                        System.out.println("Link: " + link);
                        Thread hiloCafe = new Thread(() -> {
                            try {
                                String[] mensajes = {
                                    "Preparando.",
                                    "Preparando..",
                                    "Preparando...",
                                    "Preparando....",
                                    "Preparando....."
                                };

                                int mensajeIndex = 0;

                                while (!Thread.currentThread().isInterrupted()) {
                                    for (int i = 0; i < getNumeroFrames(); i++) {
                                        String frame = getFrame(i);  // Obtenemos el frame
                                        mostrarCafe(frame, mensajes[mensajeIndex % mensajes.length]);
                                        mensajeIndex++;
                                        Thread.sleep(700); // velocidad de animación
                                    }
                                }

                            } catch (InterruptedException e) {
                                // Se interrumpe cuando termina la solicitud
                            }
                        });

                        hiloCafe.start();
                        String resultado = procesarSolicitud(link);
                        // Detener el hilo de “espera”
                        hiloCafe.interrupt();
                        //AQUI
                        try {
                            // Limpiar consola (funciona en la mayoría de terminales que soportan ANSI)
                            System.out.print("\033[H\033[2J");
                            System.out.flush();

                            // Imprimir el frame 0 del café
                            System.out.println(getFrame(0));

                            // Mensaje final
                            System.out.println("\nListo!");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println("\rProcesamiento terminado!            ");
                        ACLMessage reply = msg.createReply();
                        reply.setContent(resultado);
                        myAgent.send(reply);
                    }
                } else {
                    block();
                }
            }
        });
    }
    private void enviarAMatricula(SolicitudCursoDTO solicitud) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        Gson gson = new Gson();
        String jsonSolicitud = gson.toJson(solicitud);
        
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setContent(jsonSolicitud);
        msg.setConversationId("prematricula-" + System.currentTimeMillis());
        
        if (DFServiceUtils.buscarYEnviar(this, "prematricula", msg)) {
            NotificationLogger.mensaje(this.getLocalName(), "UnidadMatricula", 
                "PREMATRICULA enviada - Curso: " + solicitud.curso_codigo + 
                " | Horario: " + solicitud.horario_propuesto);
        } else {
            NotificationLogger.error(this.getLocalName(), 
                "No se pudo enviar PREMATRICULA a UnidadMatricula");
        }
    }


    //Animación
     // Método que imprime un frame del café y un mensaje debajo
    public static void mostrarCafe(String frame, String mensaje) {
        // Limpiar consola
        System.out.print("\033[H\033[2J");
        System.out.flush();

        // Imprimir el café
        System.out.println(frame);

        // Imprimir mensaje debajo
        System.out.println(mensaje);
    }
    public static int getNumeroFrames() {
        return 3; // Tenemos 3 frames
    }
    // Método que devuelve un frame según su índice
    public static String getFrame(int index) {
        switch (index) {
            case 0:
                return "    (  )   (   )  )\n" +
                       "     ) (   )  (  (\n" +
                       "     ( )  (    ) )\n" +
                       "     _____________\n" +
                       "    <_____________> ___\n" +
                       "    |             |/ _ \\\n" +
                       "    |               | | |\n" +
                       "    |               |_| |\n" +
                       " ___|             |\\___/\n" +
                       "/    \\___________/    \\\n" +
                       "\\_____________________/";
            case 1:
                return "    (  )   (    )  )\n" +
                       "      ) (   )  (  (\n" +
                       "     ( )  (     ) )\n" +
                       "     _____________\n" +
                       "    <_____________> ___\n" +
                       "    |             |/ _ \\\n" +
                       "    |               | | |\n" +
                       "    |               |_| |\n" +
                       " ___|             |\\___/\n" +
                       "/    \\___________/    \\\n" +
                       "\\_____________________/";
            case 2:
                return "       (  )  (   )  )\n" +
                       "        ) (   )  (  (\n" +
                       "       ( )  (    ) )\n" +
                       "     _____________\n" +
                       "    <_____________> ___\n" +
                       "    |             |/ _ \\\n" +
                       "    |               | | |\n" +
                       "    |               |_| |\n" +
                       " ___|             |\\___/\n" +
                       "/    \\___________/    \\\n" +
                       "\\_____________________/";
            default:
                return "";
        }
    }

}
