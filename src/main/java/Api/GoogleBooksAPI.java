package Api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import Model.Book;

public class GoogleBooksAPI {

    private static final Logger LOGGER = Logger.getLogger(GoogleBooksAPI.class.getName());
    private static final String API_KEY = "AIzaSyBlwd65C03o1_2H9iQZGDv8IL7mE6VJSck";
    private static final String API_URL = "https://www.googleapis.com/books/v1/volumes?q=";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(5);

    /**
     * Busca libros en la API de Google Books.
     * @param query Término de búsqueda
     * @return Lista de libros encontrados
     */
    public static List<Book> searchBooks(String query) {
        List<Book> books = new ArrayList<>();

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String fullUrl = API_URL + encodedQuery + "&maxResults=40&key=" + API_KEY;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.log(Level.WARNING, "Error en la conexión: {0}", response.statusCode());
                return books;
            }

            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray items = jsonResponse.optJSONArray("items");

            if (items == null) return books;

            for (int i = 0; i < items.length(); i++) {
                Book book = parseBookFromJson(items.getJSONObject(i));
                if (book != null) {
                    books.add(book);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar libros", e);
        }

        return books;
    }

    /**
     * Busca libros por género en la API de Google Books.
     * @param genre Género a buscar
     * @return Lista de libros encontrados
     */
    public static List<Book> searchBooksByGenre(String genre) {
        String query = "subject:" + genre;
        return searchBooks(query);
    }

    /**
     * Busca un libro por su ID en la API de Google Books de forma asíncrona.
     * @param volumeId ID del libro
     * @return CompletableFuture con el libro encontrado
     */
    public static CompletableFuture<Book> searchBookByIdAsync(String volumeId) {
        return CompletableFuture.supplyAsync(() -> searchBookById(volumeId), EXECUTOR);
    }

    /**
     * Busca un libro por su ID en la API de Google Books.
     * @param volumeId ID del libro
     * @return Libro encontrado o null si no existe
     */
    public static Book searchBookById(String volumeId) {
        try {
            String urlStr = "https://www.googleapis.com/books/v1/volumes/" + volumeId + "?key=" + API_KEY;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.log(Level.WARNING, "Error en la conexión: {0}", response.statusCode());
                return null;
            }

            JSONObject item = new JSONObject(response.body());
            return parseBookFromJson(item);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar libro por ID", e);
            return null;
        }
    }
    
    /**
     * Parsea un objeto JSON a un objeto Book.
     * @param item Objeto JSON con los datos del libro
     * @return Objeto Book o null si no se pudo parsear
     */
    private static Book parseBookFromJson(JSONObject item) {
        try {
            JSONObject volumeInfo = item.optJSONObject("volumeInfo");
            if (volumeInfo == null) return null;

            String id = item.optString("id", "Sin ID");
            String title = volumeInfo.optString("title", "Sin título");
            String description = volumeInfo.optString("description", "Sin descripción");
            String publisher = volumeInfo.optString("publisher", "");
            String publishedDate = volumeInfo.optString("publishedDate", "");
            int pageCount = volumeInfo.optInt("pageCount", 0);
            String previewLink = volumeInfo.optString("previewLink", "");

            List<String> authors = parseJsonArray(volumeInfo.optJSONArray("authors"));
            List<String> categories = parseJsonArray(volumeInfo.optJSONArray("categories"));

            String isbn = extractIsbn(volumeInfo.optJSONArray("industryIdentifiers"));
            String thumbnail = extractThumbnail(volumeInfo.optJSONObject("imageLinks"));

            Book book = new Book(id, isbn, title, authors, description, thumbnail, id);
            book.setPublisher(publisher);
            book.setPublishedDate(publishedDate);
            book.setPageCount(pageCount);
            book.setCategories(categories);
            book.setPreviewLink(previewLink);
            
            return book;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al parsear libro", e);
            return null;
        }
    }
    
    /**
     * Extrae el ISBN de un array de identificadores.
     * @param identifiers Array JSON con los identificadores
     * @return ISBN o "No disponible" si no se encuentra
     */
    private static String extractIsbn(JSONArray identifiers) {
        if (identifiers == null) return "No disponible";
        
        for (int j = 0; j < identifiers.length(); j++) {
            JSONObject identifier = identifiers.optJSONObject(j);
            if (identifier == null) continue;

            String type = identifier.optString("type");
            if ("ISBN_13".equals(type)) {
                return identifier.optString("identifier");
            } else if ("ISBN_10".equals(type)) {
                return identifier.optString("identifier");
            }
        }
        
        return "No disponible";
    }
    
    /**
     * Extrae la URL de la miniatura de un objeto de enlaces de imágenes.
     * @param imageLinks Objeto JSON con los enlaces de imágenes
     * @return URL de la miniatura o null si no se encuentra
     */
    private static String extractThumbnail(JSONObject imageLinks) {
        if (imageLinks == null) return null;
        
        String thumbnail = imageLinks.optString("thumbnail", null);
        if (thumbnail != null && thumbnail.startsWith("http:")) {
            thumbnail = thumbnail.replace("http:", "https:");
        }
        
        return thumbnail;
    }
    
    /**
     * Parsea un array JSON a una lista de strings.
     * @param jsonArray Array JSON a parsear
     * @return Lista de strings
     */
    private static List<String> parseJsonArray(JSONArray jsonArray) {
        List<String> result = new ArrayList<>();
        if (jsonArray != null) {
            for (int j = 0; j < jsonArray.length(); j++) {
                result.add(jsonArray.optString(j));
            }
        }
        return result;
    }
}

