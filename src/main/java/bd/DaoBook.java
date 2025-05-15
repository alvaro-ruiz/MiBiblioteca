package bd;

import Model.Book;
import Model.BookCollection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.sql.Date;
import java.util.List;
import java.util.ArrayList;

public class DaoBook {

    // Método para obtener todos los IDs de los libros favoritos de un usuario
    public static List<String> getLibroIDsFavoritosPorUsuario(int usuarioId) {
        List<String> favoritos = new ArrayList<>();

        // SQL que selecciona los id_api de los libros con estado "favorito"
        String sql = "SELECT l.id_api FROM usuarios_libros ul JOIN libros l ON ul.libro_id = l.id WHERE ul.usuario_id = ? AND ul.estado = 'favorito'";

        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);
            ResultSet rs = stmt.executeQuery();

            // Añadir los IDs a la lista
            while (rs.next()) {
                String idApi = rs.getString("id_api");
                if (idApi != null && !idApi.isEmpty()) {
                    favoritos.add(idApi);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return favoritos;
    }

    // Función para guardar un libro como favorito
    public static boolean guardarFavorito(int usuarioId, Book libro) {
        try {
            // Primero, insertar o actualizar el libro en la tabla libros
            String insertLibro = "INSERT INTO libros (titulo, autor, id_api, isbn, agregado_por_usuario) VALUES (?, ?, ?, ?, false) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)";
            int libroId = -1;

            try (Connection conn = Conexion.conectar();
                 PreparedStatement ps = conn.prepareStatement(insertLibro, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, libro.getTitle());
                ps.setString(2, libro.getAuthors() != null && !libro.getAuthors().isEmpty() ? libro.getAuthors().get(0) : "Desconocido");
                ps.setString(3, libro.getId());
                ps.setString(4, libro.getIsbn());
                ps.executeUpdate();

                // Obtener el ID del libro insertado o actualizado
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    libroId = rs.getInt(1);
                } else {
                    // Si no se obtuvo un ID, intentar buscarlo por id_api
                    try (PreparedStatement psFind = conn.prepareStatement("SELECT id FROM libros WHERE id_api = ?")) {
                        psFind.setString(1, libro.getId());
                        ResultSet rsFind = psFind.executeQuery();
                        if (rsFind.next()) {
                            libroId = rsFind.getInt("id");
                        }
                    }
                }
            }

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
            e.printStackTrace();
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
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    libroId = rs.getInt("id");
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
            e.printStackTrace();
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
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
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
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("estado");
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
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
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        existeEntrada = true;
                        entradaId = rs.getInt("id");
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
            e.printStackTrace();
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
            ResultSet rs = stmt.executeQuery();
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
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
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
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        existeEntrada = true;
                        entradaId = rs.getInt("id");
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
            e.printStackTrace();
            return false;
        }
    }
    
    // Obtener información de préstamo de un libro
    public static Object[] getInformacionPrestamo(int userId, String bookApiId) {
        String sql = "SELECT ul.prestado_a, ul.fecha_prestamo, ul.devuelto FROM usuarios_libros ul JOIN libros l ON ul.libro_id = l.id WHERE ul.usuario_id = ? AND l.id_api = ? AND ul.estado = 'prestado'";
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, bookApiId);
            ResultSet rs = stmt.executeQuery();
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
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Guardar información de préstamo de un libro
    public static boolean guardarInformacionPrestamo(int userId, Book libro, String prestadoA, LocalDate fechaPrestamo, boolean devuelto) {
        try {
            // Primero, obtener el ID del libro
            int libroId = obtenerOCrearLibro(libro);
            
            if (libroId != -1) {
                // Verificar si ya existe una entrada para este libro y usuario
                String checkSql = "SELECT id FROM usuarios_libros WHERE usuario_id = ? AND libro_id = ? AND estado = 'prestado'";
                boolean existeEntrada = false;
                int entradaId = -1;
                
                try (Connection conn = Conexion.conectar();
                     PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, libroId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        existeEntrada = true;
                        entradaId = rs.getInt("id");
                    }
                }
                
                // Actualizar o insertar según corresponda
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
            e.printStackTrace();
            return false;
        }
    }
    
    // Obtener libros por estado
    public static List<BookCollection> getLibrosPorEstado(int userId, String estado) {
        List<BookCollection> libros = new ArrayList<>();
        
        String sql = "SELECT l.id, l.titulo, l.autor, l.id_api, l.isbn, ul.fecha_lectura, ul.nota, ul.comentario, ul.prestado_a, ul.fecha_prestamo, ul.devuelto " +
                     "FROM usuarios_libros ul JOIN libros l ON ul.libro_id = l.id " +
                     "WHERE ul.usuario_id = ? AND ul.estado = ?";
        
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, estado);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String id = rs.getString("id_api");
                String isbn = rs.getString("isbn");
                String titulo = rs.getString("titulo");
                String autor = rs.getString("autor");
                
                // Crear una lista de autores
                List<String> autores = new ArrayList<>();
                autores.add(autor);
                
                // Crear el objeto Book
                Book book = new Book(id, isbn, titulo, autores, "");
                
                // Crear el objeto BookCollection según el estado
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return libros;
    }
    
    // Método auxiliar para obtener o crear un libro en la base de datos
    private static int obtenerOCrearLibro(Book libro) throws SQLException {
        // Primero, intentar obtener el ID del libro
        String findSql = "SELECT id FROM libros WHERE id_api = ?";
        try (Connection conn = Conexion.conectar();
             PreparedStatement ps = conn.prepareStatement(findSql)) {
            ps.setString(1, libro.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        
        // Si no existe, crear el libro
        String insertSql = "INSERT INTO libros (titulo, autor, id_api, isbn, agregado_por_usuario) VALUES (?, ?, ?, ?, false)";
        try (Connection conn = Conexion.conectar();
             PreparedStatement ps = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, libro.getTitle());
            ps.setString(2, libro.getAuthors() != null && !libro.getAuthors().isEmpty() ? libro.getAuthors().get(0) : "Desconocido");
            ps.setString(3, libro.getId());
            ps.setString(4, libro.getIsbn());
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        return -1;
    }
    
    // Método auxiliar para actualizar el estado de un libro en usuarios_libros
    private static void actualizarEstadoLibro(int userId, int libroId, String estado) throws SQLException {
        // Verificar si ya existe una entrada para este libro y usuario
        String checkSql = "SELECT id FROM usuarios_libros WHERE usuario_id = ? AND libro_id = ? AND estado != 'favorito'";
        boolean existeEntrada = false;
        int entradaId = -1;
        
        try (Connection conn = Conexion.conectar();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, libroId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                existeEntrada = true;
                entradaId = rs.getInt("id");
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
                ps.executeUpdate();
            }
        } else {
            sql = "INSERT INTO usuarios_libros (usuario_id, libro_id, estado) VALUES (?, ?, ?)";
            try (Connection conn = Conexion.conectar();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, libroId);
                ps.setString(3, estado);
                ps.executeUpdate();
            }
        }
    }
}
