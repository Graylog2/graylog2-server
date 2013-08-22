package models;

import lib.APIException;
import lib.Api;
import lib.Tools;
import models.api.responses.system.UserResponse;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.crypto.BlowfishCipherService;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;

public class User {
	private static final Logger log = LoggerFactory.getLogger(User.class);

    @Deprecated
    private final String id;
    private final String name;
	private final String email;
	private final String fullName;
	private final List<String> permissions;

    private final String passwordHash;

    public User(UserResponse ur, String passwordHash) {
        this(ur.id, ur.username, "", ur.fullName, ur.permissions, passwordHash);
    }

	public User(String id, String name, String email, String fullName, List<String> permissions, String passwordHash) {
        this.id = id;
        this.name = name;
		this.email = email;
		this.fullName = fullName;
		this.permissions = permissions;
        this.passwordHash = passwordHash;
    }

    public static User current() {
        User currentUser = (User) Http.Context.current().args.get("currentUser");
        if (currentUser == null) {
            // get the encrypted password from the session, and retrieve our user with it.
            final Http.Session session = Http.Context.current().session();
            final String encryptedPassword = session.get("creds");
            if (encryptedPassword == null) {
                log.error("No credentials found in session cookie, this is a bug.");
                return null;
            }
            final ByteSource passwordSha1Bytes = new BlowfishCipherService().decrypt(
                    Base64.decode(encryptedPassword.getBytes()),
                    Tools.appSecretAsBytes(16));
            final String username = session.get("username");
            try {
                final String passwordSha1 = new String(passwordSha1Bytes.getBytes());
                final UserResponse response = Api.get("/users/" + username, UserResponse.class, username, passwordSha1);
                currentUser = new User(response, passwordSha1);
                setCurrent(currentUser);
            } catch (IOException e) {
                log.error("Could not reach graylog2 server", e);
            } catch (APIException e) {
                log.error("Unauthorized to load user " + username, e);
            }

        }
        return currentUser;
    }

    public static void setCurrent(User user) {
        // save the current user in the request for easy access
        Http.Context.current().args.put("currentUser", user);
    }

    public static User load(String username) {
        final User currentUser = current();
        if (username.equals(currentUser.getName())) {
            return currentUser;
        }
        // a different user was requested, go and fetch it from the server
        try {
            final UserResponse response = Api.get("/users/" + username, UserResponse.class, currentUser.getName(), currentUser.getPasswordHash());
            // TODO this user is not cached locally for now. we should be tracking REST requests.
            return new User(response, null);
        } catch (IOException e) {
            log.error("Could not load user " + username, e);
        } catch (APIException e) {
            log.error("Not allowed to load user " + username, e);
        }
        log.error("Couldn't load user, this is a bug. Handle this.");
        return null;
	}

    @Deprecated
    public String getId() {
        return getName();
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

	public List<String> getPermissions() {
		return permissions;
	}

    public String getPasswordHash() {
        return passwordHash;
    }
}
