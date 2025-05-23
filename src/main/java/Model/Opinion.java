package Model;

import java.time.LocalDate;

public class Opinion {
    private int id;
    private int usuarioId;
    private String nombreUsuario;
    private String libroId;
    private double nota;
    private String comentario;
    private LocalDate fecha;
    
    public Opinion(int id, int usuarioId, String nombreUsuario, String libroId, double nota, String comentario, LocalDate fecha) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
        this.libroId = libroId;
        this.nota = nota;
        this.comentario = comentario;
        this.fecha = fecha;
    }
    
    // Getters y setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getUsuarioId() {
        return usuarioId;
    }
    
    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }
    
    public String getNombreUsuario() {
        return nombreUsuario;
    }
    
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
    
    public String getLibroId() {
        return libroId;
    }
    
    public void setLibroId(String libroId) {
        this.libroId = libroId;
    }
    
    public double getNota() {
        return nota;
    }
    
    public void setNota(double nota) {
        this.nota = nota;
    }
    
    public String getComentario() {
        return comentario;
    }
    
    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
    
    public LocalDate getFecha() {
        return fecha;
    }
    
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}
