package Model;

import java.time.LocalDate;

public class BookCollection {
    private Book book;
    private String estado;
    private LocalDate fechaLectura;
    private double nota;
    private String comentario;
    private String prestadoA;
    private LocalDate fechaPrestamo;
    private boolean devuelto;
    
    public BookCollection(Book book, String estado) {
        this.book = book;
        this.estado = estado;
    }
    
    public BookCollection(Book book, String estado, LocalDate fechaLectura, double nota, String comentario) {
        this.book = book;
        this.estado = estado;
        this.fechaLectura = fechaLectura;
        this.nota = nota;
        this.comentario = comentario;
    }
    
    public BookCollection(Book book, String estado, String prestadoA, LocalDate fechaPrestamo, boolean devuelto) {
        this.book = book;
        this.estado = estado;
        this.prestadoA = prestadoA;
        this.fechaPrestamo = fechaPrestamo;
        this.devuelto = devuelto;
    }
    
    // Getters y setters
    public Book getBook() {
        return book;
    }
    
    public void setBook(Book book) {
        this.book = book;
    }
    
    public String getEstado() {
        return estado;
    }
    
    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    public LocalDate getFechaLectura() {
        return fechaLectura;
    }
    
    public void setFechaLectura(LocalDate fechaLectura) {
        this.fechaLectura = fechaLectura;
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
    
    public String getPrestadoA() {
        return prestadoA;
    }
    
    public void setPrestadoA(String prestadoA) {
        this.prestadoA = prestadoA;
    }
    
    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }
    
    public void setFechaPrestamo(LocalDate fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }
    
    public boolean isDevuelto() {
        return devuelto;
    }
    
    public void setDevuelto(boolean devuelto) {
        this.devuelto = devuelto;
    }
}
