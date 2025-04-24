package Model;

public class BookService {
	
	private static Book currentBook;
	
    public Book getCurrentUser() {
        return currentBook;
    }

    public void setCurrentUser(Book book) {
    	currentBook = book;
    }

    public void logout() {
    	currentBook = null;
    }

}
