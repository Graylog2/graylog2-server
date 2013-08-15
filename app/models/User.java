package models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.Cache;

public class User {
	private static final Logger log = LoggerFactory.getLogger(User.class);

	private final String name;
	private final String email;
	private final String fullName;

	@Deprecated
	public User(String name, String email) {
		this.name = name;
		this.email = email;
		fullName = "";
	}

	public User(String id, String name, String email, String fullName) {
		this.name = name;
		this.email = email;
		this.fullName = fullName;
	}

	public static User load(String username) {
		final User user = (User) Cache.get(username);
		log.info("subject is {}", user);
		if (user != null) {
			return user;
		}
		return new User("lennart", "lennart@torch.sh");
	}

    public String getId() {
        // TODO implement me
        return "foo-bar-userid";
    }
	
	public String getName() {
		return name;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getFullName() {
		return fullName;
	}

}
