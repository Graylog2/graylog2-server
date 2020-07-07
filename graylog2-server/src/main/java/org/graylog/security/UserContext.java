package org.graylog.security;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.Callable;

public class UserContext {
    private final UserService userService;
    private final String username;
    private final Subject subject;

    public static class Factory {
        private final UserService userService;

        @Inject
        public Factory(UserService userService) {
            this.userService = userService;
        }

        /**
         * Create a UserContext from the currently accessible Shiro Subject available to the calling code depending on runtime environment.
         * This should only be called from within an existing Shiro context.
         * If a UserContext is needed from an environment where there is no existing context,
         * the code can be run using: {@link UserContext#runAs(String username, Callable)}
         *
         * @return a user context reflecting the currently executing user.
         * @throws UserContextMissingException
         */
        public UserContext create() throws UserContextMissingException {
            try {
                final Subject subject = SecurityUtils.getSubject();
                final Object username = subject.getPrincipal();
                if (!(username instanceof String)) {
                    throw new UserContextMissingException("Unknown SecurityContext class <" + username + ">, cannot continue.");
                }
                return new UserContext((String) username, subject, userService);
            } catch (IllegalStateException | UnavailableSecurityManagerException e) {
                throw new UserContextMissingException("Cannot retrieve current subject, SecurityContext isn't set.");
            }
        }
    }

    /**
     * Build a temporary Shiro Subject and run the callable within that context
     * @param username  The username of the subject
     * @param callable  The callable to be executed
     * @param <T>       The return type of the callable.
     * @return          whatever the callable returns.
     */
    public static <T> T runAs(String username, Callable<T> callable) {
        final Subject subject = new Subject.Builder()
                .principals(new SimplePrincipalCollection(username, "runAs-context"))
                .authenticated(true)
                .buildSubject();

        return subject.execute(callable);
    }

    private UserContext(String username, Subject subject, UserService userService) {
        this.username = username;
        this.subject = subject;
        this.userService = userService;
    }

    public boolean isPermitted(String permission, String id) {
        return subject.isPermitted(permission + ":" + id);
    }

    public boolean isPermitted(String permission) {
        return subject.isPermitted(permission);
    }

    public String getUsername() {
        return username;
    }

    public Optional<User> getUser() {
        return Optional.ofNullable(userService.load(username));
    }
}
