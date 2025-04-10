package Model;

public class Usuario {
    
    private int id;
    private String nombre;
    private String email;
    private String password;
    private String genero;

    public Usuario(String nombre, String email, String genero, String password) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.genero = genero;
    }
    
    public Usuario(int id, String nombre, String email, String genero, String password) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.genero = genero;
        this.password = password;
    }

    // Getters y setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

	public String getGenero() {
		return genero;
	}

	public void setGenero(String genero) {
		this.genero = genero;
	}

	@Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}

