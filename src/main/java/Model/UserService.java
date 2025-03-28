package Model;

public class UserService {

	private static Usuario currentUser;

    public Usuario getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(Usuario user) {
        currentUser = user;
    }

    public void logout() {
        currentUser = null;
    }
}
