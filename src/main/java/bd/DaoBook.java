package bd;

import Model.Book;
import Model.BookCollection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.sql.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DaoBook {
    private static final Logger LOGGER = Logger.getLogger(DaoBook.class.getName());

    /**
     * Busca libros en la base de datos local según un término de búsqueda.
     * Busca coincidencias en título, autor y categorías.
     * 
     * @param query Término de búsqueda
     * @return Lista de libros que coinciden con la búsqueda
     */
    public static List<Book> buscarLibrosLocales(String query) {
        List<Book> resultados = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return resultados;
        }
        
        // Normalizar la consulta
        String searchTerm = "%" + query.trim() + "%";
        
        Connection conn = null;
        
        try {
            conn = Conexion.conectar();
            if (conn == null) {
                System.err.println("No se pudo establecer conexión con la base de datos");
                return resultados;
            }
            
            // Buscar libros por título, autor o ID
            String sql = "SELECT DISTINCT l.id, l.titulo, l.autor, l.id_api, l.isbn, l.editorial, " +
                 "l.fecha_publicacion, l.agregado_por_usuario FROM libros l " +
                 "WHERE l.titulo LIKE ? OR l.autor LIKE ? OR l.id_api = ? " +
                 "ORDER BY l.agregado_por_usuario DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, searchTerm);
                stmt.setString(2, searchTerm);
                stmt.setString(3, query.trim()); // Búsqueda exacta por ID
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Book libro = mapResultSetToBook(rs);
                        if (libro != null) {
                            resultados.add(libro);
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar libros locales", e);
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error al cerrar la conexión", e);
                }
            }
        }
        
        return resultados;
    }
    
    /**
     * Convierte un ResultSet en un objeto Book.
     * 
     * @param rs ResultSet con los datos del libro
     * @return Objeto Book o null si ocurre un error
     */
    private static Book mapResultSetToBook(ResultSet rs) throws SQLException {
        try {
            String id = rs.getString("id_api");
            String isbn = rs.getString("isbn");
            String titulo = rs.getString("titulo");
            String autor = rs.getString("autor");
            
            List<String> autores = new ArrayList<>();
            if (autor != null && !autor.isEmpty()) {
                // Dividir autores si están separados por comas
                String[] autoresArray = autor.split(",");
                for (String a : autoresArray) {
                    autores.add(a.trim());
                }
            } else {
                autores.add("Desconocido");
            }
            
            String descripcion = ""; // Valor por defecto
            
            Book book = new Book(id, isbn, titulo, autores, descripcion);
            
            // Establecer datos adicionales si están disponibles
            try {
                book.setPublisher(rs.getString("editorial"));
            } catch (SQLException e) {
                // Ignorar si la columna no existe
            }
            
            try {
                book.setPublishedDate(rs.getString("fecha_publicacion"));
            } catch (SQLException e) {
                // Ignorar si la columna no existe
            }
            
            return book;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al mapear ResultSet a Book", e);
            return null;
        }
    }

    // Método para obtener todos los IDs de los libros favoritos de un usuario
    public static List<String> getLibroIDsFavoritosPorUsuario(int usuarioId) {
        List<String> favoritos = new ArrayList<>();

        // SQL que selecciona los id_api de los libros con estado "favorito"
        String sql = "SELECT l.id_api FROM usuarios_libros ul JOIN libros l ON ul.libro_id = l.id WHERE ul.usuario_id = ? AND ul.estado = 'favorito'";

        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                // Añadir los IDs a la lista
                while (rs.next()) {
                    String idApi = rs.getString("id_api");
                    if (idApi != null && !idApi.isEmpty()) {
                        favoritos.add(idApi);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener libros favoritos", e);
        }

        return favoritos;
    }

    // Función para guardar un libro como favorito
    public static boolean guardarFavorito(int usuarioId, Book libro) {
        try {
            // Primero, insertar o actualizar el libro en la tabla libros
            int libroId = obtenerOCrearLibro(libro);
            
            if (libroId != -1) {
                // Ahora, insertar la relación en usuarios_libros
                String insertRelacion = "INSERT INTO usuarios_libros (usuario_id, libro_id, estado) VALUES (?, ?, 'favorito') ON DUPLICATE KEY UPDATE estado = 'favorito'";
                try (Connection conn = Conexion.conectar();
                     PreparedStatement ps = conn.prepareStatement(insertRelacion)) {
                    ps.setInt(1, usuarioId);
                    ps.setInt(2, libroId);
                    ps.executeUpdate();
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar favorito", e);
            return false;
        }
    }

    // Eliminar un libro de los favoritos
    public static boolean removeBookFromFavorites(int userId, String bookApiId) {
        try {
            // Primero, obtener el ID del libro en la base de datos
            int libroId = -1;
            try (Connection conn = Conexion.conectar();
                 PreparedStatement ps = conn.prepareStatement("SELECT id FROM libros WHERE id_api = ?")) {
                ps.setString(1, bookApiId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        libroId = rs.getInt("id");
                    }
                }
            }

            if (libroId != -1) {
                // Ahora, eliminar la relación de usuarios_libros
                String sql = "DELETE FROM usuarios_libros WHERE usuario_id = ? AND libro_id = ? AND estado = 'favorito'";
                try (Connection conn = Conexion.conectar();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, libroId);
                    int rowsAffected = stmt.executeUpdate();
                    return rowsAffected > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar favorito", e);
            return false;
        }
    }

    // Verificar si un libro es favorito
    public static boolean isFavorite(int userId, String bookApiId) {
        String sql = "SELECT 1 FROM usuarios_libros ul JOIN libros l ON ul.libro_id = l.id WHERE ul.usuario_id = ? AND l.id_api = ? AND ul.estado = 'favorito'";
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, bookApiId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al verificar favorito", e);
            return false;
        }
    }
    
    // Obtener el estado actual de un libro
    public static String getEstadoLibro(int userId, String bookApiId) {
        String sql = "SELECT ul.estado FROM usuarios_libros ul JOIN libros l ON ul.libro_id = l.id WHERE ul.usuario_id = ? AND l.id_api = ? AND ul.estado != 'favorito'";
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, bookApiId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("estado");
                }
            }
            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener estado del libro", e);
            return null;
        }
    }
    
    // Guardar el estado de un libro
    public static boolean guardarEstadoLibro(int userId, Book libro, String estado) {
        try {
            // Primero, insertar o actualizar el libro en la tabla libros
            int libroId = obtenerOCrearLibro(libro);
            
            if (libroId != -1) {
                // Verificar si ya existe una entrada para este libro y usuario
                String checkSql = "SELECT id FROM usuarios_libros WHERE usuario_id = ? AND libro_id = ? AND estado != 'favorito'";
                boolean existeEntrada = false;
                int entradaId = -1;
                
                try (Connection conn = Conexion.conectar();
                     PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, libroId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existeEntrada = true;
                            entradaId = rs.getInt("id");
                        }
                    }
                }
                
                // Actualizar o insertar según corresponda
                String sql;
                if (existeEntrada) {
                    sql = "UPDATE usuarios_libros SET estado = ? WHERE id = ?";
                    try (Connection conn = Conexion.conectar();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, estado);
                        ps.setInt(2, entradaId);
                        return ps.executeUpdate() > 0;
                    }
                } else {
                    sql = "INSERT INTO usuarios_libros (usuario_id, libro_id, estado) VALUES (?, ?, ?)";
                    try (Connection conn = Conexion.conectar();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, userId);
                        ps.setInt(2, libroId);
                        ps.setString(3, estado);
                        return ps.executeUpdate() > 0;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar estado del libro", e);
            return false;
        }
    }
    
    // Obtener información de lectura de un libro
    public static Object[] getInformacionLectura(int userId, String bookApiId) {
        String sql = "SELECT ul.fecha_lectura, ul.nota, ul.comentario FROM usuarios_libros ul JOIN libros l ON ul.libro_id = l.id WHERE ul.usuario_id = ? AND l.id_api = ? AND ul.estado = 'leído'";
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, bookApiId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Date fechaLectura = rs.getDate("fecha_lectura");
                    Double nota = rs.getDouble("nota");
                    String comentario = rs.getString("comentario");
                    
                    LocalDate localFechaLectura = null;
                    if (fechaLectura != null) {
                        localFechaLectura = fechaLectura.toLocalDate();
                    }
                    
                    return new Object[] { localFechaLectura, nota, comentario };
                }
            }
            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener información de lectura", e);
            return null;
        }
    }
    
    // Guardar información de lectura de un libro
    public static boolean guardarInformacionLectura(int userId, Book libro, LocalDate fechaLectura, double nota, String comentario) {
        try {
            // Primero, obtener el ID del libro
            int libroId = obtenerOCrearLibro(libro);
            
            if (libroId != -1) {
                // Verificar si ya existe una entrada para este libro y usuario
                String checkSql = "SELECT id FROM usuarios_libros WHERE usuario_id = ? AND libro_id = ? AND estado = 'leído'";
                boolean existeEntrada = false;
                int entradaId = -1;
                
                try (Connection conn = Conexion.conectar();
                     PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, libroId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existeEntrada = true;
                            entradaId = rs.getInt("id");
                        }
                    }
                }
                
                // Actualizar o insertar según corresponda
                String sql;
                if (existeEntrada) {
                    sql = "UPDATE usuarios_libros SET fecha_lectura = ?, nota = ?, comentario = ? WHERE id = ?";
                    try (Connection conn = Conexion.conectar();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setDate(1, fechaLectura != null ? Date.valueOf(fechaLectura) : null);
                        ps.setDouble(2, nota);
                        ps.setString(3, comentario);
                        ps.setInt(4, entradaId);
                        return ps.executeUpdate() > 0;
                    }
                } else {
                    sql = "INSERT INTO usuarios_libros (usuario_id, libro_id, estado, fecha_lectura, nota, comentario) VALUES (?, ?, 'leído', ?, ?, ?)";
                    try (Connection conn = Conexion.conectar();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, userId);
                        ps.setInt(2, libroId);
                        ps.setDate(3, fechaLectura != null ? Date.valueOf(fechaLectura) : null);
                        ps.setDouble(4, nota);
                        ps.setString(5, comentario);
                        return ps.executeUpdate() > 0;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar información de lectura", e);
            return false;
        }
    }
    
    public static Object[] getInformacionPrestamo(int userId, String bookApiId) {
        String sql = "SELECT ul.prestado_a, ul.fecha_prestamo, ul.devuelto FROM usuarios_libros ul JOIN libros l ON ul.libro_id = l.id WHERE ul.usuario_id = ? AND l.id_api = ? AND ul.estado = 'prestado'";
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, bookApiId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String prestadoA = rs.getString("prestado_a");
                    Date fechaPrestamo = rs.getDate("fecha_prestamo");
                    Boolean devuelto = rs.getBoolean("devuelto");
                    
                    LocalDate localFechaPrestamo = null;
                    if (fechaPrestamo != null) {
                        localFechaPrestamo = fechaPrestamo.toLocalDate();
                    }
                    
                    return new Object[] { prestadoA, localFechaPrestamo, devuelto };
                }
            }
            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener información de préstamo", e);
            return null;
        }
    }
    
    public static boolean guardarInformacionPrestamo(int userId, Book libro, String prestadoA, LocalDate fechaPrestamo, boolean devuelto) {
        try {
            int libroId = obtenerOCrearLibro(libro);
            
            if (libroId != -1) {
                String checkSql = "SELECT id FROM usuarios_libros WHERE usuario_id = ? AND libro_id = ? AND estado = 'prestado'";
                boolean existeEntrada = false;
                int entradaId = -1;
                
                try (Connection conn = Conexion.conectar();
                     PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, libroId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existeEntrada = true;
                            entradaId = rs.getInt("id");
                        }
                    }
                }
                
                String sql;
                if (existeEntrada) {
                    sql = "UPDATE usuarios_libros SET prestado_a = ?, fecha_prestamo = ?, devuelto = ? WHERE id = ?";
                    try (Connection conn = Conexion.conectar();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, prestadoA);
                        ps.setDate(2, fechaPrestamo != null ? Date.valueOf(fechaPrestamo) : null);
                        ps.setBoolean(3, devuelto);
                        ps.setInt(4, entradaId);
                        return ps.executeUpdate() > 0;
                    }
                } else {
                    sql = "INSERT INTO usuarios_libros (usuario_id, libro_id, estado, prestado_a, fecha_prestamo, devuelto) VALUES (?, ?, 'prestado', ?, ?, ?)";
                    try (Connection conn = Conexion.conectar();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, userId);
                        ps.setInt(2, libroId);
                        ps.setString(3, prestadoA);
                        ps.setDate(4, fechaPrestamo != null ? Date.valueOf(fechaPrestamo) : null);
                        ps.setBoolean(5, devuelto);
                        return ps.executeUpdate() > 0;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar información de préstamo", e);
            return false;
        }
    }
    
    public static List<BookCollection> getLibrosPorEstado(int userId, String estado) {
        List<BookCollection> libros = new ArrayList<>();
        
        String sql = "SELECT l.id, l.titulo, l.autor, l.id_api, l.isbn, ul.fecha_lectura, ul.nota, ul.comentario, ul.prestado_a, ul.fecha_prestamo, ul.devuelto " +
                     "FROM usuarios_libros ul JOIN libros l ON ul.libro_id = l.id " +
                     "WHERE ul.usuario_id = ? AND ul.estado = ?";
        
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, estado);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id_api");
                    String isbn = rs.getString("isbn");
                    String titulo = rs.getString("titulo");
                    String autor = rs.getString("autor");
                    
                    List<String> autores = new ArrayList<>();
                    autores.add(autor);
                    
                    Book book = new Book(id, isbn, titulo, autores, "");
                    
                    BookCollection bookCollection;
                    
                    if ("leído".equals(estado)) {
                        Date fechaLectura = rs.getDate("fecha_lectura");
                        double nota = rs.getDouble("nota");
                        String comentario = rs.getString("comentario");
                        
                        LocalDate localFechaLectura = null;
                        if (fechaLectura != null) {
                            localFechaLectura = fechaLectura.toLocalDate();
                        }
                        
                        bookCollection = new BookCollection(book, estado, localFechaLectura, nota, comentario);
                    } else if ("prestado".equals(estado)) {
                        String prestadoA = rs.getString("prestado_a");
                        Date fechaPrestamo = rs.getDate("fecha_prestamo");
                        boolean devuelto = rs.getBoolean("devuelto");
                        
                        LocalDate localFechaPrestamo = null;
                        if (fechaPrestamo != null) {
                            localFechaPrestamo = fechaPrestamo.toLocalDate();
                        }
                        
                        bookCollection = new BookCollection(book, estado, prestadoA, localFechaPrestamo, devuelto);
                    } else {
                        bookCollection = new BookCollection(book, estado);
                    }
                    
                    libros.add(bookCollection);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener libros por estado", e);
        }
        
        return libros;
    }
    
    private static int obtenerOCrearLibro(Book libro) throws SQLException {
        String findSql = "SELECT id FROM libros WHERE id_api = ?";
        try (Connection conn = Conexion.conectar();
             PreparedStatement ps = conn.prepareStatement(findSql)) {
            ps.setString(1, libro.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        
        String insertSql = "INSERT INTO libros (titulo, autor, id_api, isbn, agregado_por_usuario) VALUES (?, ?, ?, ?, false)";
        try (Connection conn = Conexion.conectar();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, libro.getTitle());
            ps.setString(2, libro.getAuthors() != null && !libro.getAuthors().isEmpty() ? String.join(", ", libro.getAuthors()) : "Desconocido");
            ps.setString(3, libro.getId());
            ps.setString(4, libro.getIsbn());
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return -1;
    }
    
    public static boolean guardarLibroManual(int userId, Book libro, String estado) {
        Connection conn = null;
        try {
            conn = Conexion.conectar();
            if (conn == null) {
                System.err.println("Error: No se pudo establecer conexión con la base de datos");
                return false;
            }
            
            // Primero verificar si el libro ya existe en la base de datos
            String checkSql = "SELECT id FROM libros WHERE id_api = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, libro.getId());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    int libroId;
                    
                    if (rs.next()) {
                        // El libro ya existe, obtener su ID
                        libroId = rs.getInt("id");
                        System.out.println("Libro encontrado en la base de datos, ID: " + libroId);
                    } else {
                        // El libro no existe, insertarlo
                        System.out.println("Libro no encontrado, insertando nuevo libro");
                        
                        String insertLibroSql = "INSERT INTO libros (id_api, titulo, autor, editorial, fecha_publicacion, isbn, agregado_por_usuario) VALUES (?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement insertLibroStmt = conn.prepareStatement(insertLibroSql, Statement.RETURN_GENERATED_KEYS)) {
                            
                            insertLibroStmt.setString(1, libro.getId());
                            insertLibroStmt.setString(2, libro.getTitle());
                            insertLibroStmt.setString(3, libro.getAuthors() != null ? String.join(", ", libro.getAuthors()) : "");
                            insertLibroStmt.setString(4, libro.getPublisher());
                            insertLibroStmt.setString(5, libro.getPublishedDate());
                            insertLibroStmt.setString(6, libro.getIsbn());
                            insertLibroStmt.setBoolean(7, true);
                            
                            int rowsAffected = insertLibroStmt.executeUpdate();
                            System.out.println("Filas afectadas al insertar libro: " + rowsAffected);
                            
                            // Obtener el ID generado
                            try (ResultSet generatedKeys = insertLibroStmt.getGeneratedKeys()) {
                                if (generatedKeys.next()) {
                                    libroId = generatedKeys.getInt(1);
                                    System.out.println("ID generado para el nuevo libro: " + libroId);
                                } else {
                                    throw new SQLException("No se pudo obtener el ID del libro insertado");
                                }
                            }
                        }
                    }
                    
                    // Ahora insertar en la tabla de usuarios_libros
                    System.out.println("Insertando relación usuario-libro. Usuario ID: " + userId + ", Libro ID: " + libroId + ", Estado: " + estado);
                    String insertRelacionSql = "INSERT INTO usuarios_libros (usuario_id, libro_id, estado) VALUES (?, ?, ?) " +
                                              "ON DUPLICATE KEY UPDATE estado = ?";
                    try (PreparedStatement insertRelacionStmt = conn.prepareStatement(insertRelacionSql)) {
                        
                        insertRelacionStmt.setInt(1, userId);
                        insertRelacionStmt.setInt(2, libroId);
                        insertRelacionStmt.setString(3, estado);
                        insertRelacionStmt.setString(4, estado);
                        
                        int rowsAffected = insertRelacionStmt.executeUpdate();
                        System.out.println("Filas afectadas al insertar relación: " + rowsAffected);
                        
                        return rowsAffected > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error SQL al guardar libro manual: " + e.getMessage());
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Error al guardar libro manual", e);
            return false;
        } finally {
            // Asegurarse de cerrar la conexión
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error al cerrar la conexión: " + e.getMessage());
                }
            }
        }
    }
}
