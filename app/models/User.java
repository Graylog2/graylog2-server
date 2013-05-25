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
