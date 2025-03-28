package bd;

import java.sql.DriverManager;
import java.sql.SQLException;

public class Connection {

	private static final String URL = "jdbc:mysql://localhost:3306/";
    private static final String BBDD = "bd_MiBiblioteca";
    private static final String PARAMETROS = "?serverTimezone=UTC";
    private static final String USUARIO = "root";
    private static final String CLAVE = "26838279q";

    public static Connection conectar() {
        Connection conexion = null;

        try {
            conexion = (Connection) DriverManager.getConnection(URL + BBDD + PARAMETROS, USUARIO, CLAVE);
            System.out.println("Conexion OK");
        } catch (SQLException e) {
            System.out.println("Error en la conexion");
            e.printStackTrace();
        }

        return conexion;
    }
}
