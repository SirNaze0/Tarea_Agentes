package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class InitDatabase {

    private static final String URL = "jdbc:sqlite:data/escuela.db";

    public static void main(String[] args) {
        crearTablas();
        insertarDatosIniciales();
        mostrarContenidoTablas();
    }

    private static void crearTablas() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Profesor_Nombrado (" +
                "ID_Profesor_Nombrado INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Nombres TEXT NOT NULL," +
                "Apellidos TEXT NOT NULL," +
                "DNI TEXT UNIQUE NOT NULL" +
                ");"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Horario_Curso (" +
                "ID_Horario_Curso INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Dia_Semana_1 TEXT NOT NULL," +
                "Hora_Inicio_1 TEXT NOT NULL," +
                "Hora_Fin_1 TEXT NOT NULL," +
                "Dia_Semana_2 TEXT NOT NULL," +
                "Hora_Inicio_2 TEXT NOT NULL," +
                "Hora_Fin_2 TEXT NOT NULL" +
            ");"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Curso (" +
                "ID_Curso INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ID_Profesor_Nombrado INTEGER," +
                "ID_Horario_Curso INTEGER," +
                "Codigo_Curso TEXT," +
                "Nombre_Curso TEXT," +
                "Ciclo TEXT," +
                "FOREIGN KEY (ID_Profesor_Nombrado) REFERENCES Profesor_Nombrado(ID_Profesor_Nombrado)," +
                "FOREIGN KEY (ID_Horario_Curso) REFERENCES Horario_Curso(ID_Horario_Curso)" +
                ");"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Estudiante (" +
                "ID_Estudiante INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Codigo INTEGER UNIQUE," +
                "Cantidad_Habilitada INTEGER," +
                "Ciclo INTEGER" +
                ");"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Matricula (" +
                "ID_Curso_Estudiante INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ID_Estudiante INTEGER," +
                "ID_Curso INTEGER," +
                "FOREIGN KEY (ID_Estudiante) REFERENCES Estudiante(ID_Estudiante)," +
                "FOREIGN KEY (ID_Curso) REFERENCES Curso(ID_Curso)" +
                ");"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Pago (" +
                "ID_Pago INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ID_Estudiante INTEGER," +
                "FOREIGN KEY (ID_Estudiante) REFERENCES Estudiante(ID_Estudiante)" +
                ");"
            );

            System.out.println("✅ Tablas creadas correctamente");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertarDatosIniciales() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                "INSERT OR IGNORE INTO Profesor_Nombrado (Nombres, Apellidos, DNI) VALUES " +
                "('GELBER CHRISTIAN', 'USCUCHAGUA FLORES', '45678912')," +
                "('JORGE SANTIAGO', 'PANTOJA COLLANTES', '48912377')," +
                "('JOSE CESAR', 'PIEDRA ISUSQUI', '47896521');"
            );

            System.out.println("✅ Profesores insertados");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void mostrarContenidoTablas() {
        mostrarTabla("Profesor_Nombrado");
        mostrarTabla("Horario_Curso");
        mostrarTabla("Curso");
        mostrarTabla("Estudiante");
        mostrarTabla("Matricula");
        mostrarTabla("Pago");
    }

    private static void mostrarTabla(String tabla) {
        System.out.println("\n📄 Tabla: " + tabla);

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tabla)) {

            int cols = rs.getMetaData().getColumnCount();

            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    System.out.print(
                        rs.getMetaData().getColumnName(i) +
                        "=" + rs.getString(i) + " | "
                    );
                }
                System.out.println();
            }

        } catch (Exception e) {
            System.out.println("(vacía)");
        }
    }
}

