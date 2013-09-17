package models;

import com.google.common.collect.Lists;
import lib.APIException;
import lib.ApiClient;
import models.api.responses.system.UserResponse;
import models.api.responses.system.UsersListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.libs.Crypto;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;

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
        this(ur.id, ur.username, ur.email, ur.fullName, ur.permissions, passwordHash);
    }

	public User(String id, String name, String email, String fullName, List<String> permissions, String passwordHash) {
        this.id = id;
        this.name = name;
		this.email = email;
		this.fullName = fullName;
		this.permissions = permissions;
        this.passwordHash = passwordHash;
    }

    public static User authenticateSessionUser() {
        // is there a logged in user at all?
        final Http.Session session = Http.Context.current().session();
        final String sessionId = session.get("sessionid");
        if (sessionId == null) {
            // there is no authenticated user yet.
            log.info("Accessing the current user failed, there's no sessionid in the cookie.");
            return null;
        }
        final String userPassHash = Crypto.decryptAES(sessionId);
        final StringTokenizer tokenizer = new StringTokenizer(userPassHash, "\t");
        if (tokenizer.countTokens() != 2) {
            return null;
        }
        final String userName = tokenizer.nextToken();
        final String passwordSha1 = tokenizer.nextToken();

        // special case for the local admin user for the web interface
        if (userName != null) {
            final LocalAdminUser localAdminUser = LocalAdminUser.getInstance();
            if (userName.equals(localAdminUser.getName())) {
                User.setCurrent(localAdminUser);
                return localAdminUser;
            }
        }
        try {
            UserResponse response = ApiClient.get(UserResponse.class)
                    .credentials(userName, passwordSha1)
                    .path("/users/{0}", userName)
                    .execute();

            User currentUser = new User(response, passwordSha1);
            setCurrent(currentUser);
            return currentUser;
        } catch (IOException e) {
            log.error("Could not reach graylog2 server", e);
        } catch (APIException e) {
            log.error("Unauthorized to load user " + userName, e);
        }
        return null;
    }

    public static User current() {
        try {
            return (User) Http.Context.current().args.get("currentUser");
        } catch (RuntimeException e) {
            // Http.Context.current() throws a plain RuntimeException if there's no context,
            // for example in background threads.
            // That is fine, because we don't have a current user in those scenarios anyway.
            return null;
        }
    }

    public static void setCurrent(User user) {
        // save the current user in the request for easy access
        Http.Context.current().args.put("currentUser", user);
        log.debug("Setting the request's current user to {}", user);
    }

    public static User load(String username) {
        final User currentUser = current();
        if (username.equals(currentUser.getName())) {
            return currentUser;
        }
        // a different user was requested, go and fetch it from the server
        try {
            final UserResponse response = ApiClient.get(UserResponse.class).path("/users/{0}", username).execute();
            // TODO this user is not cached locally for now. we should be tracking REST requests.
            return new User(response, null);
        } catch (IOException e) {
            log.error("Could not load user " + username, e);
        } catch (APIException e) {
            log.info("User " + username + " does not exist", e);
        }
        return null;
	}

    public static List<User> all() {
        UsersListResponse response;
        try {
            response = ApiClient.get(UsersListResponse.class).path("/users").execute();
            List<User> users = Lists.newArrayList();
            for (UserResponse userResponse : response.users) {
                users.add(new User(userResponse, null)); // we don't have password's for the user list, obviously
            }
            return users;
        } catch (IOException e) {
            log.error("Could not retrieve list of users", e);
        } catch (APIException e) {
            log.error("Could not retrieve list of users", e);
        }
        return Lists.newArrayList();
    }

    public static void create(CreateUserRequest request) {
        try {
            ApiClient.post().path("/users").body(request).expect(Http.Status.CREATED).execute();
        } catch (APIException e) {
            log.error("Unable to create user", e);
        } catch (IOException e) {
            log.error("Unable to create user", e);
        }
    }

    public void update(ChangeUserRequest request) {
        try {
            ApiClient.put().path("/users/{0}", getName()).body(request).expect(Http.Status.NO_CONTENT).execute();
        } catch (APIException e) {
            log.error("Unable to update user", e);
        } catch (IOException e) {
            log.error("Unable to update user", e);
        }
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
        if (permissions == null) {
            return Lists.newArrayList();
        }
		return permissions;
	}

    public String getPasswordHash() {
        return passwordHash;
    }

    public static class LocalAdminUser extends User {

        private static AtomicReference<LocalAdminUser> instance = new AtomicReference<>(null);

        private LocalAdminUser(String id, String name, String email, String fullName, List<String> permissions, String passwordHash) {
            super(id, name, email, fullName, permissions, passwordHash);
        }

        public static void createSharedInstance(String username, String passwordHash) {
            final LocalAdminUser adminUser = new LocalAdminUser("0", username, "None",  "Interface Admin", Lists.newArrayList("*"), passwordHash);
            if (! instance.compareAndSet(null, adminUser)) {
                // unless we are in test mode, this would be a bug.
                if (! Play.application().isTest()) {
                    throw new IllegalStateException("Attempted to reset the local admin user object. This is a bug.");
                }
            }
        }

        public static LocalAdminUser getInstance() {
            return instance.get();
        }

    }
}
