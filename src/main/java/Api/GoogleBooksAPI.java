package Api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import Model.Book;

public class GoogleBooksAPI {
	private static final String API_URL = "https://www.googleapis.com/books/v1/volumes?q=";

	public static List<Book> searchBooks(String query) {
	    List<Book> books = new ArrayList<>();

	    try {
	        String encodedQuery = URLEncoder.encode(query, "UTF-8");
	        URL url = new URL(API_URL + encodedQuery);
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("GET");
	        conn.setRequestProperty("Accept", "application/json");

	        if (conn.getResponseCode() != 200) {
	            throw new RuntimeException("Error en la conexión: " + conn.getResponseCode());
	        }

	        StringBuilder response = new StringBuilder();
	        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
	            String line;
	            while ((line = br.readLine()) != null) {
	                response.append(line);
	            }
	        }
	        conn.disconnect();

	        JSONArray items = new JSONObject(response.toString()).optJSONArray("items");
	        if (items == null) return books;

	        for (int i = 0; i < items.length(); i++) {
	            JSONObject item = items.getJSONObject(i);
	            JSONObject volumeInfo = item.optJSONObject("volumeInfo");

	            if (volumeInfo == null) continue;

	            String id = item.optString("id", "Sin ID");
	            String title = volumeInfo.optString("title", "Sin título");
	            String description = volumeInfo.optString("description", "Sin descripción");

	            // Autores
	            List<String> authors = new ArrayList<>();
	            JSONArray authorsArray = volumeInfo.optJSONArray("authors");
	            if (authorsArray != null) {
	                for (int j = 0; j < authorsArray.length(); j++) {
	                    authors.add(authorsArray.optString(j));
	                }
	            }

	            // ISBN
	            String isbn = "No disponible";
	            JSONArray identifiers = volumeInfo.optJSONArray("industryIdentifiers");
	            if (identifiers != null) {
	                for (int j = 0; j < identifiers.length(); j++) {
	                    JSONObject identifier = identifiers.optJSONObject(j);
	                    if (identifier == null) continue;

	                    String type = identifier.optString("type");
	                    if ("ISBN_13".equals(type)) {
	                        isbn = identifier.optString("identifier");
	                        break;
	                    } else if ("ISBN_10".equals(type)) {
	                        isbn = identifier.optString("identifier");
	                    }
	                }
	            }

	            books.add(new Book(id, isbn, title, authors, description));
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return books;
	}

	public static List<Book> searchBooksByGenre(String genre) {
	    String query = "subject:" + genre;
	    return searchBooks(query);
	}
	
	public static Book searchBookById(String volumeId) {
	    try {
	        String urlStr = "https://www.googleapis.com/books/v1/volumes/" + volumeId;
	        URL url = new URL(urlStr);

	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("GET");
	        conn.setRequestProperty("Accept", "application/json");

	        if (conn.getResponseCode() != 200) {
	            throw new RuntimeException("Error en la conexión: " + conn.getResponseCode());
	        }

	        StringBuilder response = new StringBuilder();
	        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
	            String line;
	            while ((line = br.readLine()) != null) {
	                response.append(line);
	            }
	        }

	        JSONObject item = new JSONObject(response.toString());
	        JSONObject volumeInfo = item.optJSONObject("volumeInfo");

	        if (volumeInfo == null) return null;

	        String id = item.optString("id", "Sin ID");
	        String title = volumeInfo.optString("title", "Sin título");
	        String description = volumeInfo.optString("description", "Sin descripción");

	        List<String> authors = new ArrayList<>();
	        JSONArray authorsArray = volumeInfo.optJSONArray("authors");
	        if (authorsArray != null) {
	            for (int j = 0; j < authorsArray.length(); j++) {
	                authors.add(authorsArray.optString(j));
	            }
	        }

	        String isbn = "No disponible";
	        JSONArray identifiers = volumeInfo.optJSONArray("industryIdentifiers");
	        if (identifiers != null) {
	            for (int j = 0; j < identifiers.length(); j++) {
	                JSONObject identifier = identifiers.optJSONObject(j);
	                if (identifier == null) continue;

	                String type = identifier.optString("type");
	                if ("ISBN_13".equals(type)) {
	                    isbn = identifier.optString("identifier");
	                    break;
	                } else if ("ISBN_10".equals(type)) {
	                    isbn = identifier.optString("identifier");
	                }
	            }
	        }

	        return new Book(id, isbn, title, authors, description);

	    } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	    }
	}


}

