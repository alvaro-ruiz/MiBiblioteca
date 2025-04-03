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
            // Construimos la URL con la consulta codificada
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            URL url = new URL(API_URL + encodedQuery);

            // Abrimos la conexión HTTP
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            // Verificamos si la respuesta es exitosa (código 200)
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Error en la conexión: " + conn.getResponseCode());
            }

            // Leemos la respuesta JSON
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            conn.disconnect();

            // Parseamos la respuesta JSON
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray items = jsonResponse.optJSONArray("items");

            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    JSONObject volumeInfo = item.getJSONObject("volumeInfo");

                    // Extraemos los datos del libro
                    String title = volumeInfo.optString("title", "Sin título");
                    
                    List<String> authors = new ArrayList<>();
                    JSONArray authorsArray = volumeInfo.optJSONArray("authors");
                    if (authorsArray != null) {
                        for (int j = 0; j < authorsArray.length(); j++) {
                            authors.add(authorsArray.getString(j));
                        }
                    }

                    String description = volumeInfo.optString("description", "Sin descripción");

                    String isbn = "No disponible";
                    JSONArray industryIdentifiers = volumeInfo.optJSONArray("industryIdentifiers");
                    if (industryIdentifiers != null) {
                        for (int j = 0; j < industryIdentifiers.length(); j++) {
                            JSONObject identifier = industryIdentifiers.getJSONObject(j);
                            String type = identifier.getString("type");
                            if ("ISBN_13".equals(type)) {
                                isbn = identifier.getString("identifier");
                                break;
                            } else if ("ISBN_10".equals(type)) {
                                isbn = identifier.getString("identifier");
                            }
                        }
                    }

                    // Agregamos el libro a la lista
                    books.add(new Book(isbn, title, authors, description));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return books;
    }
}

