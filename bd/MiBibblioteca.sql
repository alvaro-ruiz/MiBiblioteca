Drop schema if exists bd_MiBiblioteca;

Create Schema if not exists bd_MiBiblioteca;
use bd_MiBiblioteca;

-- Tabla de Usuarios
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    genero VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de Géneros Favoritos por Usuario
CREATE TABLE generos_favoritos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    genero VARCHAR(50) NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Tabla de Libros
CREATE TABLE libros (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    autor VARCHAR(255) NOT NULL,
    editorial VARCHAR(255),
    isbn VARCHAR(20) UNIQUE,
    fecha_publicacion DATE,
    agregado_por_usuario BOOLEAN DEFAULT FALSE
);

-- Relación entre Usuarios y sus Libros
CREATE TABLE usuarios_libros (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    libro_id INT,
    estado ENUM('pendiente', 'leyendo', 'leído', 'prestado', 'deseado') NOT NULL,
    fecha_lectura DATE NULL,
    nota DECIMAL(3,1) NULL CHECK (nota BETWEEN 0 AND 10),
    comentario TEXT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (libro_id) REFERENCES libros(id) ON DELETE CASCADE
);

-- Tabla de Préstamos de Libros
CREATE TABLE prestamos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    libro_id INT,
    prestado_a VARCHAR(100) NOT NULL,
    fecha_prestamo DATE NOT NULL,
    fecha_devolucion DATE NULL,
    devuelto BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (libro_id) REFERENCES libros(id) ON DELETE CASCADE
);

-- Tabla de Opiniones de Libros
CREATE TABLE opiniones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    libro_id INT,
    nota DECIMAL(3,1) NOT NULL CHECK (nota BETWEEN 0 AND 10),
    comentario TEXT NOT NULL,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (libro_id) REFERENCES libros(id) ON DELETE CASCADE
);

-- Índices para mejorar la búsqueda
CREATE INDEX idx_libro_titulo ON libros (titulo);
CREATE INDEX idx_libro_autor ON libros (autor);
CREATE INDEX idx_libro_isbn ON libros (isbn);
