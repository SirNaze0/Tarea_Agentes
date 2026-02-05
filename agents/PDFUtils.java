import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PDFUtils {
    
    /**
     * Descarga PDF con manejo de errores mejorado
     */
    public static String descargarPdf(String pdfUrl) throws Exception {
        // Si es una ruta local, devolverla directamente
        File archivoLocal = new File(pdfUrl);
        if (archivoLocal.exists()) {
            System.out.println("Usando archivo local: " + pdfUrl);
            return pdfUrl;
        }
        
        // Si es URL, descargar
        String carpeta = "pdfs/";
        Files.createDirectories(Paths.get(carpeta));
        String nombreArchivo = "solicitud_" + System.currentTimeMillis() + ".pdf";
        String rutaDestino = carpeta + nombreArchivo;
        
        try {
            //System.out.println("Descargando PDF desde: " + pdfUrl);
            
            URL url = new URL(pdfUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, Paths.get(rutaDestino), StandardCopyOption.REPLACE_EXISTING);
            }
            
            //System.out.println("PDF descargado: " + rutaDestino);
            return rutaDestino;
            
        } catch (UnknownHostException e) {
            System.err.println("No se pudo conectar a Internet");
            throw new Exception("Sin conexión a Internet: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error al descargar PDF: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Extrae texto completo del PDF y ELIMINA TODAS LAS TILDES
     */
    public static String extraerTextoCompleto(String rutaPdf) throws IOException {
        try (PDDocument document = PDDocument.load(new File(rutaPdf))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            
            String texto = stripper.getText(document);
            
            // IMPORTANTE: Normalizar y eliminar tildes
            texto = TextUtils.normalizarYLimpiar(texto);
            
            return texto;
        }
    }
    
    /**
     * Extrae texto desde una página específica y ELIMINA TILDES
     */
    public static String extraerTextoDesdePagina(String rutaPdf, int paginaInicio) throws IOException {
        try (PDDocument document = PDDocument.load(new File(rutaPdf))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(paginaInicio);
            stripper.setEndPage(document.getNumberOfPages());
            stripper.setSortByPosition(true);
            
            String texto = stripper.getText(document);
            texto = TextUtils.normalizarYLimpiar(texto);
            
            return texto;
        }
    }
    
    
    /**
     * Cuenta códigos de estudiantes (formato: AAXXXXXX)
     */
    public static int contarFirmantesPorCodigo(String textoFirmas) {
        if (textoFirmas == null || textoFirmas.isEmpty()) return 0;
        
        Pattern pattern = Pattern.compile("\\b\\d{2}20\\d{4}\\b");
        Matcher matcher = pattern.matcher(textoFirmas);
        
        Set<String> codigosUnicos = new HashSet<>();
        while (matcher.find()) {
            codigosUnicos.add(matcher.group());
        }
        
        if (codigosUnicos.size() > 0) {
            //System.out.println("codigos detectados (" + codigosUnicos.size() + "): " + codigosUnicos);
        }
        
        return codigosUnicos.size();
    }
}