package models;

public class User {
	
	private final String name;
	private final String email;
	
	public User(String name, String email) {
		this.name = name;
		this.email = email;
	}
	
	public static User load(String userId) {
		User user = new User("lennart", "lennart@torch.sh");
		
		return user;
	}
	
	public static boolean authenticate(String username, String password) {
		if (username.equals("lennart") && password.equals("123123123")) {
			return true; 
		} else {
			return false;
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getFullName() {
		return "Lennart Koopmann";
	}
	
}
