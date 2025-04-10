package bd;

import Model.Usuario;

public class BdPrueba {
	
	public static void main(String[] args) {
		if (DaoUser.addUsuario(new Usuario("alvaro","aa1.0@gmail.com","Fiction","1234")))
			System.out.println("Add: ok");
		else
			System.out.println("Add: ko");
		
		if (DaoUser.getUsuarioById(1) != null)
			System.out.println("Select id: ok");
		else
			System.out.println("Select id: ko");
		
		if (DaoUser.updateUsuario(new Usuario( 1, "alvaro","aa@gmail.com", "Fiction","1234")))
			System.out.println("Uptade: ok");
		else
			System.out.println("Uptade: ko");
		
		if (DaoUser.getAllUsuarios() != null)
			System.out.println("All: ok");
		else
			System.out.println("All: ko");
		
		if (DaoUser.loginUsuario("aa@gmail.com", "1234") != null)
			System.out.println("Login: ok");
		else
			System.out.println("Login: ko");
		
	}

}
