package Model;

import java.util.List;

public class Book {
    private String id;
    private String title;
    private String subtitle;
    private List<String> authors;
    private String publisher;
    private String publishedDate;
    private String description;
    private int pageCount;
    private List<String> categories;
    private String thumbnail;
    private String language;
    private String previewLink;
    private String id_api;
    private String isbn;
    
    public Book(String id, String isbn, String title, List<String> authors, String description, String thumbnail, String idApi) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.authors = authors;
        this.description = description;
        this.thumbnail = thumbnail;
        this.id_api = idApi;
    }
    
    public Book(String id, String isbn, String title, List<String> authors, String description) {
    	this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.authors = authors;
        this.description = description;
    }
    
    public Book() {
        // Constructor vacío
    }

	// Getters y setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPreviewLink() {
        return previewLink;
    }

    public void setPreviewLink(String previewLink) {
        this.previewLink = previewLink;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

	public String getId_api() {
		return id_api;
	}

	public void setId_api(String id_api) {
		this.id_api = id_api;
	}
}
