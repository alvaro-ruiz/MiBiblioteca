package bd;

import Model.Opinion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DaoOpinion {

    // Obtener todas las opiniones de un libro
    public static List<Opinion> getOpinionesPorLibro(String libroId) {
        List<Opinion> opiniones = new ArrayList<>();
        
        String sql = "SELECT o.id, o.usuario_id, u.nombre, o.libro_id, o.nota, o.comentario, o.fecha " +
                     "FROM opiniones o JOIN usuarios u ON o.usuario_id = u.id " +
                     "JOIN libros l ON o.libro_id = l.id " +
                     "WHERE l.id_api = ? " +
                     "ORDER BY o.fecha DESC";
        
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, libroId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                int usuarioId = rs.getInt("usuario_id");
                String nombreUsuario = rs.getString("nombre");
                String idLibro = rs.getString("libro_id");
                double nota = rs.getDouble("nota");
                String comentario = rs.getString("comentario");
                Date fecha = rs.getDate("fecha");
                
                LocalDate localFecha = null;
                if (fecha != null) {
                    localFecha = fecha.toLocalDate();
                } else {
                    localFecha = LocalDate.now();
                }
                
                Opinion opinion = new Opinion(id, usuarioId, nombreUsuario, idLibro, nota, comentario, localFecha);
                opiniones.add(opinion);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return opiniones;
    }
    
    // Agregar una nueva opinión
    public static boolean agregarOpinion(Opinion opinion) {
        // Primero, obtener el ID del libro en la base de datos
        int libroId = obtenerIdLibro(opinion.getLibroId());
        
        if (libroId == -1) {
            return false; // No se encontró el libro
        }
        
        String sql = "INSERT INTO opiniones (usuario_id, libro_id, nota, comentario, fecha) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, opinion.getUsuarioId());
            stmt.setInt(2, libroId);
            stmt.setDouble(3, opinion.getNota());
            stmt.setString(4, opinion.getComentario());
            stmt.setDate(5, Date.valueOf(opinion.getFecha()));
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Eliminar una opinión
    public static boolean eliminarOpinion(int opinionId) {
        String sql = "DELETE FROM opiniones WHERE id = ?";
        
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, opinionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Método auxiliar para obtener el ID del libro en la base de datos
    private static int obtenerIdLibro(String idApi) {
        String sql = "SELECT id FROM libros WHERE id_api = ?";
        
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, idApi);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return -1;
    }
}
