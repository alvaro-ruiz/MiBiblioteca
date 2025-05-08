package Api;

import java.util.List;

import Model.Book;

public class mainApi {

    public static void main(String[] args) {
        List<Book> books = GoogleBooksAPI.searchBooksByGenre("Ficción");
        int i = 0;
        for (Book book : books) {
            System.out.println(book.getTitle() + ", " + book.getPublisher());
            System.out.println(i + 1);
            i++;
        }
    }


}
