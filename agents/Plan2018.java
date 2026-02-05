import java.util.*;
import java.util.Optional;


public class Plan2018 {

  public static final class Curso {
    public final String codigo;
    public final int ciclo;
    public final String nombre;
    public final double creditos;

    public Curso(String codigo, int ciclo, String nombre, double creditos) {
      this.codigo = codigo;
      this.ciclo = ciclo;
      this.nombre = nombre;
      this.creditos = creditos;
    }
    public static Optional<Curso> buscarPorCodigo(String codigo) {
        return CURSOS.stream()
                .filter(c -> c.codigo.equalsIgnoreCase(codigo))
                .findFirst();
    }
  }

  public static final List<Curso> CURSOS =
    Collections.unmodifiableList(Arrays.asList(

    // ================= CICLO 1 =================
    new Curso("INE002", 1, "PROGRAMACION Y COMPUTACION", 2),
    new Curso("INE013", 1, "EMPRENDIMIENTO E INNOVACION", 2),
    new Curso("INO101", 1, "REDACCION Y TÉCNICAS DE COMUNICACION EFECTIVA I", 3),
    new Curso("INO102", 1, "METODOS DE ESTUDIO UNIVERSITARIO", 2),
    new Curso("INO103", 1, "DESARROLLO PERSONAL Y LIDERAZGO", 2),
    new Curso("INO104", 1, "CALCULO I", 4),
    new Curso("INO105", 1, "BIOLOGIA PARA CIENCIAS E INGENIERIA", 4),
    new Curso("INO106", 1, "ALGEBRA Y GEOMETRIA ANALITICA", 4),
    new Curso("INO107", 1, "MEDIO AMBIENTE Y DESARROLLO SOSTENIBLE", 3),

    // ================= CICLO 2 =================
    new Curso("INO201", 2, "REDACCION Y TECNICAS DE COMUNICACION EFECTIVA II", 3),
    new Curso("INO202", 2, "INVESTIGACION FORMATIVA", 3),
    new Curso("INO203", 2, "REALIDAD NACIONAL Y MUNDIAL", 2),
    new Curso("INO204", 2, "CALCULO II", 4),
    new Curso("INO205", 2, "FISICA I", 4),
    new Curso("INO206", 2, "QUIMICA GENERAL", 4),
    new Curso("INO207", 2, "INTRODUCCION A LAS CIENCIAS E INGENIERIA", 2),

    // ================= CICLO 3 =================
    new Curso("20118031", 3, "PROGRAMACION Y FUNDAMENTOS DE ALGORITMICA", 4),
    new Curso("20118032", 3, "TEORIA GENERAL DE SISTEMAS", 3),
    new Curso("20118033", 3, "ORGANIZACION Y ADMINISTRACION", 3),
    new Curso("20118034", 3, "INGENIERIA ECONOMICA", 3),
    new Curso("20118035", 3, "ESTADISTICA", 3),
    new Curso("20118036", 3, "SERIES Y ECUACIONES DIFERENCIALES", 3),
    new Curso("20118037", 3, "MATEMATICAS DISCRETAS", 3),

    // ================= CICLO 4 =================
    new Curso("20118041", 4, "ALGORITMICA Y PROGRAMACION ORIENTADA A OBJETOS", 4),
    new Curso("20118042", 4, "MARKETING", 2),
    new Curso("20118043", 4, "CONTABILIDAD GENERAL", 3),
    new Curso("20118044", 4, "PROCESOS DE NEGOCIOS", 3),
    new Curso("20118045", 4, "METODOS NUMERICOS", 3),
    new Curso("20118046", 4, "PROBABILIDADES Y MUESTREO", 3),
    new Curso("20118047", 4, "FISICA ELECTRÓNICA Y SISTEMAS DIGITALES", 4),

    // ================= CICLO 5 =================
    new Curso("20118051", 5, "BASE DE DATOS", 4),
    new Curso("20118052", 5, "DISEÑO Y ANÁLISIS DE ALGORITMOS", 3),
    new Curso("20118053", 5, "ESTRUCTURA DE DATOS", 3),
    new Curso("20118054", 5, "ANALISIS DE SISTEMAS DE INFORMACION", 3),
    new Curso("20118055", 5, "MODELOS Y SIMULACION", 3),
    new Curso("20118056", 5, "ARQUITECTURA DE COMPUTADORAS", 3),
    new Curso("20118057", 5, "LENGUAJES Y COMPILADORES", 3),

    // ================= CICLO 6 =================
    new Curso("20118061", 6, "BIG DATA", 3),
    new Curso("20118062", 6, "COMPUTACION VISUAL", 3),
    new Curso("20118063", 6, "FINANZAS PARA LA GESTION", 3),
    new Curso("20118064", 6, "DISEÑO DE SISTEMAS DE INFORMACION", 3),
    new Curso("20118065", 6, "INVESTIGACION OPERATIVA", 3),
    new Curso("20118066", 6, "REDES, TRANSMISION Y AUTOMATIZACION Y CONTROL", 4),
    new Curso("20118067", 6, "SISTEMAS OPERATIVOS", 3),

    // ================= CICLO 7 =================
    new Curso("20118071", 7, "INTERACCION HOMBRE COMPUTADOR", 3),
    new Curso("20118072", 7, "INTELIGENCIA DE NEGOCIOS", 3),
    new Curso("20118073", 7, "DESARROLLO DE SISTEMAS WEB", 3),
    new Curso("20118074", 7, "FORMULACION Y EVALUACION DE PROYECTOS", 3),
    new Curso("20118075", 7, "INTELIGENCIA ARTIFICIAL", 3),
    new Curso("20118076", 7, "INTERNET DE LAS COSAS", 3),
    new Curso("20118077", 7, "PROGRAMACION PARALELA", 3),

    // ================= CICLO 8 =================
    new Curso("20118081", 8, "METODOLOGIA DE LA ELABORACION DE TESIS", 3),
    new Curso("20118082", 8, "SISTEMAS DISTRIBUIDOS", 3),
    new Curso("20118083", 8, "DESARROLLO DE SISTEMAS MOVILES", 3),
    new Curso("20118084", 8, "INGENIERIA DE LA INFORMACION", 4),
    new Curso("20118085", 8, "GESTION DE PROYECTOS DE TI", 3),
    new Curso("20118086", 8, "SISTEMAS INTELIGENTES", 3),
    new Curso("20118087", 8, "AUDITORIA Y SEGURIDAD DE TI", 3),

    // ================= CICLO 9 =================
    new Curso("20118091", 9, "DESARROLLO DE PROYECTOS DE TESIS I", 2),
    new Curso("20118092", 9, "MINERIA DE DATOS", 3),
    new Curso("20118093", 9, "TALLER DE APLICACIONES DISTRIBUIDAS", 3),
    new Curso("20118094", 9, "INNOVACION, CAMBIO ORGANIZACIONAL Y EMPRENDIMIENTO", 4),
    new Curso("20118095", 9, "ARQUITECTURA EMPRESARIAL", 3),
    new Curso("20118096", 9, "ETICA Y DERECHO INFORMATICO", 2),
    new Curso("20118097", 9, "TENDENCIAS EN SISTEMAS DE INFORMACION", 3),

    // ================= CICLO 10 =================
    new Curso("20118101",10, "DESARROLLO DE PROYECTOS DE TESIS II", 2),
    new Curso("20118102",10, "PRACTICA PRE PROFESIONAL", 4),
    new Curso("20118103",10, "GESTION DEL CONOCIMIENTO", 3),
    new Curso("20118104",10, "PLANEAMIENTO DE RECURSOS EMPRESARIALES", 3),
    new Curso("20118105",10, "GERENCIA INFORMATICA", 2)

  ));
}
