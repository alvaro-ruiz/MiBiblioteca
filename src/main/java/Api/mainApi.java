package Api;

import java.util.List;

import Model.Book;

public class mainApi {

    public static void main(String[] args) {
        List<Book> books = GoogleBooksAPI.searchBooks("starwars");
        for (Book book : books) {
            System.out.println(book.getTitle() + ", " + book.getIsbn());
        }
    }


}
